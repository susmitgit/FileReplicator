package com.rti.dds.example.file;                            

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rti.dds.domain.DomainParticipant;
import com.rti.dds.domain.DomainParticipantFactory;
import com.rti.dds.domain.DomainParticipantFactoryQos;
import com.rti.dds.domain.DomainParticipantQos;
import com.rti.dds.example.file.messages.FileFragment;
import com.rti.dds.example.file.messages.FileSegmentDataReader;
import com.rti.dds.example.file.messages.FileSegmentSeq;
import com.rti.dds.example.file.messages.FileSegmentTypeCode;
import com.rti.dds.example.file.messages.FileSegmentTypeSupport;
import com.rti.dds.example.util.DebugDataReaderListener;
import com.rti.dds.example.util.DebugDomainParticipantListener;
import com.rti.dds.example.util.DebugSubscriberListener;
import com.rti.dds.example.util.DebugTopicListener;
import com.rti.dds.example.util.ProgramOptions;
import com.rti.dds.example.util.ProgramOptions.Option;
import com.rti.dds.infrastructure.ConditionSeq;
import com.rti.dds.infrastructure.Duration_t;
import com.rti.dds.infrastructure.HistoryQosPolicyKind;
import com.rti.dds.infrastructure.LivelinessQosPolicyKind;
import com.rti.dds.infrastructure.RETCODE_ALREADY_DELETED;
import com.rti.dds.infrastructure.RETCODE_BAD_PARAMETER;
import com.rti.dds.infrastructure.RETCODE_ERROR;
import com.rti.dds.infrastructure.RETCODE_ILLEGAL_OPERATION;
import com.rti.dds.infrastructure.RETCODE_IMMUTABLE_POLICY;
import com.rti.dds.infrastructure.RETCODE_INCONSISTENT_POLICY;
import com.rti.dds.infrastructure.RETCODE_NOT_ENABLED;
import com.rti.dds.infrastructure.RETCODE_NO_DATA;
import com.rti.dds.infrastructure.RETCODE_OUT_OF_RESOURCES;
import com.rti.dds.infrastructure.RETCODE_PRECONDITION_NOT_MET;
import com.rti.dds.infrastructure.RETCODE_TIMEOUT;
import com.rti.dds.infrastructure.RETCODE_UNSUPPORTED;
import com.rti.dds.infrastructure.ReliabilityQosPolicyKind;
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.infrastructure.StringSeq;
import com.rti.dds.infrastructure.TransportBuiltinKind;
import com.rti.dds.infrastructure.TransportMulticastSettings_t;
import com.rti.dds.infrastructure.WaitSet;
import com.rti.dds.subscription.DataReaderQos;
import com.rti.dds.subscription.InstanceStateKind;
import com.rti.dds.subscription.ReadCondition;
import com.rti.dds.subscription.SampleInfo;
import com.rti.dds.subscription.SampleInfoSeq;
import com.rti.dds.subscription.SampleStateKind;
import com.rti.dds.subscription.Subscriber;
import com.rti.dds.subscription.SubscriberQos;
import com.rti.dds.subscription.ViewStateKind;
import com.rti.dds.topic.ContentFilteredTopic;
import com.rti.dds.topic.Topic;
import com.rti.dds.topic.TopicDescription;
import com.rti.dds.topic.TopicQos;
import com.rti.ndds.config.LogCategory;
import com.rti.ndds.config.LogVerbosity;

/**
 * FileSegmentSubscriber establishes entities to receive and reconstruct
 * FileSegments and write them to disk.
 * <p>
 * Two threads are created to do this.  The first, in this class, reads
 * messages via DDS and passes received {@link FileFragment}s to a
 * blocking queue.  The second, {@link FileReconstructor} reads from
 * the queue until a complete file has been received and writes it to disk.
 */
public class FileSegmentSubscriber {
    /**
     * Standard java.util logger
     */
    private static final Logger logger = 
        Logger.getLogger(FileSegmentSubscriber.class.getName());

    /**
     * This is a somewhat random overhead size for packets to ensure that RTPS
     * headers, heartbeats, etc. can be accounted for (above and beyond what the
     * application strictly needs for its serialized data).
     */
    public static final int MESSAGE_OVERHEAD_SIZE = 2048;
    
    /**
     * FileSegmentSubscriber command-line options.
     */       
    static final class Opt {
        
        /**
         * Used to manipulate Options as a group.
         */
        static ProgramOptions All = new ProgramOptions();
        
        /**
         * Enables certain debug output.  There are many other verbosity 
         * options. See the <code>Debug*Listeners</code> in the util package, 
         * in particular. 
         */
        static final Option DEBUG = Option.makeBooleanOption(All, 
                "debug", false);
        
        /**
         * Specifies domain ID for participant (0 by default).
         */
        static final Option DOMAIN_ID = Option.makeOption(All, 
                "domainID", int.class, "0");
        
        /**
         * Specifies whether UDP should be used as a transport.
         */
        static final Option DISABLE_UDP = Option.makeBooleanOption(All, 
                "disableUdp", true);
        
        /**
         * Specifies whether shared memory should be used as a transport.
         */
        static final Option DISABLE_SHMEM = Option.makeBooleanOption(All, 
                "disableShmem", true);
        
        /**
         * Root directory from which to publish files.
         */
        static final Option BASE_DIRECTORY = Option.makeStringOption(All, 
                "baseDir", "copy");
        
        /**
         * Specifies topic string.
         */
        static final Option TOPIC = Option.makeStringOption(All, 
                "topic", "file_replicator");
        
        /**
         * Size of file segment blocking queue lying between Segmenter and WriteThread.
         */
        static final Option QUEUE_SIZE = Option.makeOption(All, 
                "segmentQueueSize", int.class, "50");
        
        /**
         * String specifying content filter.
         */
        static final Option CONTENT_FILTER = Option.makeStringOption(All, 
                "contentFilter", "");
        
        /**
         * Address to receive multicast data.
         */
        static final Option MULTICAST_ADDRS = Option.makeStringOption(All, 
                "multicastReceiveAddress", "");
        
        /**
         * Log Level as a string.  See <code>java.util.logging.Logger</code>
         */
        static final Option LOG_LEVEL = Option.makeStringOption(All, 
                "logLevel", "INFO");
        
        /**
         * If true, data isn't actually written and no files created.
         */
        static final Option FAKE_WRITE = Option.makeBooleanOption(All, 
                "fakeWrite", false);
        
        /**
         * Size of sample queue lying between WriteThread and DDS.
         */
        static final Option DDS_QSIZE = Option.makeOption(All, 
                "DDSQueueSize", int.class, "50");
        
        /**
         * Size of transmit transport buffers.
         */
        static final Option TRANSPORT_QUEUE_SIZE = Option.makeOption(All, 
                "transportQueueSize", int.class, "25");
        
        /**
         * Interfaces which should be denied for use.
         */
        static final Option UDP_DENY_INTERFACE_LIST =
            Option.makeOption(All, "udpDenyInteraceList", String.class, 
                    "10.10.*");
    }

    /**
     * DDS Domain Particpant.  Declared here so we can access it in shutdown();
     */
    private DomainParticipant participant;
    /**
     * Allows filtering based on content of particular data samples. 
     */
    private ContentFilteredTopic cft;  
    /**
     * DDS DataReader generated by rtiddsgen
     */
    private FileSegmentDataReader dataReader;
    /**
     * Reconstructs received file segments.
     */
    private final FileReconstructor fileReconstructor;
    /**
     * Blocking queue that acts as communication channel between the DataReader and Reconstructor.
     */
    private final ArrayBlockingQueue<FileFragment> queue;
    /**
     * Tracks how often the Subscriber is waiting on a full segment queue
     */
    private long queueFull;    
    
    /**
     * Creates a new FileSegmentSubscriber and associated FileReconstructor. 
     *
     */
    private FileSegmentSubscriber() {        
        cft = null;
        
        // make our queue
        queue = new ArrayBlockingQueue<FileFragment>(Opt.QUEUE_SIZE.asInt());
        
        String baseDir = Opt.BASE_DIRECTORY.asString();
        File baseDirectory = new File(baseDir);
        if (!baseDirectory.exists()){
            logger.warning("Base directory does not exist.");
            if (!baseDirectory.mkdirs()){
                logger.log(Level.SEVERE, "Cannot create base directory!");
                System.exit(-1);
            }
        }
        
        // create the FileReconstructor
        fileReconstructor = new FileReconstructor(baseDirectory, queue);
    }
    
    /**
     * Initializes DDS entities, starts the Segmenter and Write threads.
     * @return true iff RTI DDS setup succeeded
     */    
    protected boolean setupRTIDDS() {
        try {
            boolean ok = createParticipant();
            if(ok) {
                Subscriber subscriber = createSubscriber();
                Topic topic = createTopic();
                
                // The content-filtered topic in DataReader needs its parents enabled
                try{
                    participant.enable();
                }catch(RETCODE_ALREADY_DELETED r){
                System.out.println("RETCODE_ALREADY_DELETED");
                }
                catch(RETCODE_BAD_PARAMETER r){
                System.out.println("RETCODE_BAD_PARAMETER");
                }
                catch(RETCODE_ILLEGAL_OPERATION r){
                System.out.println("RETCODE_ILLEGAL_OPERATION");
                }
                catch(RETCODE_IMMUTABLE_POLICY r){
                System.out.println("RETCODE_IMMUTABLE_POLICY");
                }
                catch(RETCODE_INCONSISTENT_POLICY r){
                System.out.println("RETCODE_INCONSISTENT_POLICY");
                }
                catch(RETCODE_NO_DATA r){
                System.out.println("RETCODE_NO_DATA");
                }
                catch(RETCODE_NOT_ENABLED r){
                System.out.println("RETCODE_NOT_ENABLED");
                }
                catch(RETCODE_OUT_OF_RESOURCES r){
                System.out.println("RETCODE_OUT_OF_RESOURCES");
                }
                catch(RETCODE_PRECONDITION_NOT_MET r){
                System.out.println("RETCODE_PRECONDITION_NOT_MET");
                }
                catch(RETCODE_TIMEOUT r){
                System.out.println("RETCODE_TIMEOUT");
                }
                catch(RETCODE_UNSUPPORTED r){
                System.out.println("RETCODE_UNSUPPORTED");
                }
                catch(RETCODE_ERROR r){
                System.out.println("RETCODE_ERROR");
                }                
                createDataReader(subscriber, topic);
                fileReconstructor.start();
            }
            return ok;
        }catch(RETCODE_ERROR error) {
            error.printStackTrace();
            return false;
        }
    }
    
    /**
     * Sets up Domain Participant
     * @return true iff Participant configuration suceeded
     */
    
    protected boolean createParticipant() {
        DomainParticipantFactory factory = DomainParticipantFactory.get_instance();
        DomainParticipantFactoryQos domainParticipantFactoryQos = 
            new DomainParticipantFactoryQos();
        factory.get_qos(domainParticipantFactoryQos);
        
        // Change the QosPolicy to create disabled participants, since we can't
        // modify the transport properties of an enables participant.
        // Don't forget to enable the participant later--we do it in setupRTIDDS()
        domainParticipantFactoryQos.entity_factory.autoenable_created_entities = false;
        
        factory.set_qos(domainParticipantFactoryQos);
        
        DomainParticipantQos participantQos = new DomainParticipantQos();
        factory.get_default_participant_qos(participantQos);

        // increase the size of the typecode that we can handle
        int typeCodeSize = 0;
        for(int i = 0; i <= 7; i++) {
            typeCodeSize = Math.max(typeCodeSize, 
                    FileSegmentTypeCode.VALUE.get_serialized_size(i));
        }
        participantQos.resource_limits.type_object_max_serialized_length = 
                2 * typeCodeSize;
        participantQos.resource_limits.type_object_max_deserialized_length = 
                2 * typeCodeSize;
        participantQos.resource_limits
            .deserialized_type_object_dynamic_allocation_threshold = 
                2 * typeCodeSize;
        
        // set our liveliness assertion period to 2 and half seconds
        participantQos.discovery_config.participant_liveliness_assert_period.sec = 2;
        participantQos.discovery_config.participant_liveliness_assert_period.nanosec = 500000000;       
        
        // adjust our lease duration so that a writer won't attempt to publish
        // data after this reader has exited
        participantQos.discovery_config.participant_liveliness_lease_duration.sec = 10;
        participantQos.discovery_config.participant_liveliness_lease_duration.nanosec = 00000000;
              
        final boolean enableUdp = Opt.DISABLE_UDP.asBoolean();
        final boolean enableShmem = Opt.DISABLE_SHMEM.asBoolean();
        
        // set the mask for the transport(s) selected
        participantQos.transport_builtin.mask = 
              (enableUdp ? TransportBuiltinKind.UDPv4 : 0)
            | (enableShmem ? TransportBuiltinKind.SHMEM : 0);
        
        logger.finer("Transport(s) in use :" +
                (enableUdp ? " DISABLE_UDP" : "") +
                 (enableShmem ? " DISABLE_SHMEM" : ""));
        
        // this listener can be configured to print only those events of interest
        // by using the constructor that takes a printMask.
        DebugDomainParticipantListener debugParticipantListener =  
            new DebugDomainParticipantListener();
        participant = factory.create_participant(
                Opt.DOMAIN_ID.asInt(), 
                participantQos,
                debugParticipantListener, 
                StatusKind.STATUS_MASK_ALL);
                
        if (participant == null) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Creates and returns Publisher with default QoS.
     * @return new Publisher
     */    
    protected Subscriber createSubscriber() {
        
        SubscriberQos subscriberQos = new SubscriberQos();
        participant.get_default_subscriber_qos(subscriberQos);
        
        // change the publisherQos here.  Because this program doesn't need to adjust the
        // defaults, we pass the unmodified publisherQoS here.  Alternatively, we could call
        // participant.create_publisher(DomainParticipant.SUBSCRIBER_QOS_DEFAULT, debugSubscriberListener, mask); 
        
        // this listener can be configured to print only those events of interest
        // by using the constructor that takes a printMask.
        final int mask = StatusKind.STATUS_MASK_ALL & ~StatusKind.DATA_ON_READERS_STATUS;
        DebugSubscriberListener debugSubscriberListener 
            = new DebugSubscriberListener(mask);
        return participant.create_subscriber(
            subscriberQos, 
            debugSubscriberListener,
            StatusKind.STATUS_MASK_ALL);
    }

    /**
     * Creates and returns Topic with default QoS, and sets DebugTopicListener
     * to receive associated callbacks.
     * @return new Topic
     */
    protected Topic createTopic() {
        // Register type before creating topic
        String typeName = FileSegmentTypeSupport.get_type_name(); 
        FileSegmentTypeSupport.register_type(participant, typeName);

        String topicName = Opt.TOPIC.asString();
        
        TopicQos topicQos = new TopicQos();
        participant.get_default_topic_qos(topicQos);
        // this program doesn't need to adjust any of the defaults
        
        // this listener can be configured to print only those events of interest
        // by using the constructor that takes a printMask.
        DebugTopicListener debugTopicListener = new DebugTopicListener();
        Topic topic = participant.create_topic(
            topicName,
            typeName, 
            topicQos,
            debugTopicListener, 
            StatusKind.STATUS_MASK_ALL);
        
        String contentFilter = Opt.CONTENT_FILTER.asString();
        if (contentFilter.length() > 0) {
            // Not an interactive program, so changing filtering parameters
            // at runtime isn't necessary.  One use of this might be to
            // only accept files that have been modified within the last
            // n hours/days.
            StringSeq filterParameters = new StringSeq();
            
            // remove the beginning and trailing quotes
            if (contentFilter.charAt(0) == '"') {
                contentFilter = contentFilter.substring(1);
            }
            
            if (contentFilter.charAt(contentFilter.length()-1) == '"') {
                contentFilter = contentFilter.substring(0, contentFilter.length()-1);
            }
            try {
                cft = participant.create_contentfilteredtopic(
                        "testFilter",
                        topic,
                        contentFilter,
                        filterParameters);
            } catch (Throwable t) {
                System.out.println("");
                System.out.println("");
                System.out.println("");
                System.out.println("caught it");
                System.out.println("");
                System.out.println("");
                System.out.println("");
                t.printStackTrace();
                System.exit(-1);
            }
        }
        return topic;
    }
    
    /**
     * Configures QoS parameters and creates DataReaader
     * @param subscriber Subscriber used to create this DataReader
     * @param topic Topic DataReader follows
     */
    protected void createDataReader(Subscriber subscriber, Topic topic) {
        // we want a reliable reader
        DataReaderQos dataReaderQos = new DataReaderQos();
        subscriber.get_default_datareader_qos(dataReaderQos);
        
        dataReaderQos.liveliness.kind = 
            LivelinessQosPolicyKind.AUTOMATIC_LIVELINESS_QOS;
        dataReaderQos.liveliness.lease_duration.sec = 15;
        dataReaderQos.liveliness.lease_duration.nanosec = 0;
       
        dataReaderQos.reliability.kind = 
            ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;
       
        final int max_samples = Opt.DDS_QSIZE.asInt();
        dataReaderQos.resource_limits.max_samples = max_samples;

        // Only one publisher
        dataReaderQos.resource_limits.max_instances = 1;
        dataReaderQos.resource_limits.max_samples_per_instance = max_samples;
        dataReaderQos.resource_limits.initial_samples = max_samples;
        dataReaderQos.resource_limits.initial_instances = 1;
        dataReaderQos.reader_resource_limits.max_samples_per_remote_writer = max_samples;
        
        // Keep All means we inform writer when the receive queue is full.  Samples
        // are not removed until the client calls take().
        dataReaderQos.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
        
        // The DataReader will send ACKNACK responses at a random time within the
        // min/max delay range.  Increasing this range can be useful if many readers
        // are receiving data via multicast, as it decreases the chance of ACKNACK
        // response collisions.
        dataReaderQos.protocol.rtps_reliable_reader.max_heartbeat_response_delay.sec = 0;
        dataReaderQos.protocol.rtps_reliable_reader.
            max_heartbeat_response_delay.nanosec = 10000000; // 10 milliseconds
        
        String multicastAddress = Opt.MULTICAST_ADDRS.asString();
        if (multicastAddress.length() > 0) {
            try {
                TransportMulticastSettings_t multicastReceiveSettings = 
                    new TransportMulticastSettings_t();
                multicastReceiveSettings.receive_address = 
                    InetAddress.getByName(multicastAddress);
                dataReaderQos.multicast.value.add(multicastReceiveSettings);
            }catch(UnknownHostException uhe){
                logger.warning("Cannot use multicast receive address : " + 
                        multicastAddress);
            }
        }
        
        // this listener can be configured to print only those events of interest
        // by using the constructor that takes a printMask.
        DebugDataReaderListener debugDataReaderListener 
            = new DebugDataReaderListener();
        
        TopicDescription topicDescription = topic;
        // if there is a content filter in place, use it for the topic description
        if (cft != null) {
            logger.info("Creating contentFilteredTopic from " + cft.get_filter_expression());
            topicDescription = cft;
        }

        // create the data reader
        dataReader = (FileSegmentDataReader)
            subscriber.create_datareader(
                topicDescription, 
                dataReaderQos, 
                debugDataReaderListener,
                StatusKind.STATUS_MASK_ALL);
    }
    
    /**
     * This method gets messages from RTI DDS and pushes them over to the queue.
     * It continuously loops, waiting until the DataReader has new samples
     * available.  We then read all of these samples and push them to the
     * {@link FileReconstructor}'s queue.
     *
     */
    protected void readMessages() {        
        
        /* Set up readCondition to activate when unread samples are available
         * NOT_READ_SAMPLE_STATE: We are only interested in unread samples
         * ANY_VIEW_STATE: We don't care if samples are from a new or
         *                 reborn (lost and regained liveliness) instance.
         * ANY_INSTANCE_STATE: We don't care if samples are from an alive
         *                     (has live writers), disposed (a DataWriter
         *                     explicitly disposed instance) or dead
         *                     (no live writers).
         */
         
        ReadCondition readCondition = dataReader.create_readcondition(
                SampleStateKind.NOT_READ_SAMPLE_STATE,
                ViewStateKind.ANY_VIEW_STATE,
                InstanceStateKind.ANY_INSTANCE_STATE);
        
        WaitSet waitSet = new WaitSet();
        waitSet.attach_condition(readCondition);
        Duration_t duration = 
            new Duration_t(Duration_t.DURATION_INFINITY_SEC, 
                           Duration_t.DURATION_INFINITY_NSEC);
        ConditionSeq conditions = new ConditionSeq();
        
        // The time when the first <code>FileSegment</code> for the current file 
        // was received.
        long startTime = 0;
        
        // The time when the lasst <code>FileSegment</code> for the current file 
        // was received.
        long endTime = 0;
        
        // run forever
        while(true){
            try {
                // wait forever for the attached readCondition(s) to be true.
                // When wait returns, the conditions parameter will contain
                // the conditions that triggered the unblock
                waitSet.wait(conditions, duration);
                
                // take the data and put it into the queue
                FileSegmentSeq dataSeq = new FileSegmentSeq();
                SampleInfoSeq infoSeq = new SampleInfoSeq();
                
                try {
                    // Passing empty sequences to take() means we borrow memory from the
                    // receive queue.  After processing, we return the loan in 'finally'                    
                    dataReader.take(
                        dataSeq, infoSeq,
                        ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
                        SampleStateKind.NOT_READ_SAMPLE_STATE,
                        ViewStateKind.ANY_VIEW_STATE,
                        InstanceStateKind.ANY_INSTANCE_STATE);
                    
                    /* Loop through all of the samples received.
                     * Note that the DataReader doesn't notify us about a given
                     * sample until all prior samples have arrived, so we can
                     * just iterator through the sequence. 
                     */
                    for(int i = 0; i < dataSeq.size(); ++i) {
                        SampleInfo info = (SampleInfo)infoSeq.get(i);
                        // ensure that we have actual data before processing it
                        if (info.valid_data) {
                            FileFragment segment = (FileFragment)dataSeq.get(i);
                            
                            // record the start time if this is the first segment
                            // for the given file
                            if (segment.segmentNumber == 1) {
                                startTime = System.currentTimeMillis();
                            }
                            
                            // if this is the last segment, then print some
                            // statistics
                            if (segment.segmentNumber == segment.totalSegmentCount) {
                                endTime = System.currentTimeMillis();
                                printStatistics(startTime, endTime, segment);
                            }
                            
                            // it is critically important to make a copy of the
                            // data. if we don't make a copy, we'll get the same
                            // instance back with new data values in it which 
                            // will confuse the FileReconstructor greatly
                            FileFragment copy = new FileFragment();
                            copy.copy_from(segment);
                            
                            boolean success = false;
                            while (!success) {
                                try {
                                    success = queue.offer(copy, 10,
                                            TimeUnit.MILLISECONDS);
                                } catch (InterruptedException e) {
                                    queueFull++;
                                    if (queueFull % 250 == 0) {
                                        logger.info("queueFull = " + queueFull);
                                    }
                                }
                            }
                        }
                    }
                } catch (RETCODE_NO_DATA noData) {
                    // No data to process
                    // this is not an error
                } finally {
                    dataReader.return_loan(dataSeq, infoSeq);
                }
                
            } catch(RETCODE_PRECONDITION_NOT_MET precondition) {
                logger.log(Level.WARNING, "Precondition not met : ", precondition);
                break;
                
            } catch (RETCODE_TIMEOUT timeout) {
                // with an infinite duration, this shouldn't happen
                if (duration.is_infinite()) {
                    logger.warning("Timed out with an infinite timout!");
                    
                }else{
                    // if the timeout was adjusted away from infinite, then
                    // this isn't an error.
                    // if you have other logic to do when the timeout occurred
                    // this is the place to put it
                }
            }
        }
    }
    
    /**
     * Print throughput stats for a received file.
     * @param startTime time we received first segment
     * @param endTime time we received last segment
     * @param segment any segment from file in question.  Used to get overall file information.
     */
    private void printStatistics(long startTime, 
                                 long endTime, 
                                 FileFragment segment) {
        
        if (endTime == startTime) {
            logger.info("   There are no statistics to print, the file " +
                    "received in less time than the clock resolution.");
            return;
        }
        
        double receiveTime = (endTime - startTime) / 1000.0 ;
        // add up all of the fields for this file segment
        long numBytesReceived = 
            segment.fileDescription.size
            + (segment.totalSegmentCount *
                    (segment.fileDescription.name.length() + 1
                    + segment.fileDescription.path.length() + 1
                    + 8 // size
                    + 8 // lastModifiedDate
                    + 4 // segmentNumber
                    + 4)); // totalSegmentCount
        
        StringBuffer buffer = new StringBuffer();
        buffer.append("\n   took this long to receive " + receiveTime + " seconds.");
        buffer.append("\n   throughput : " + 
                (numBytesReceived / 1024.0) / receiveTime
                + "KB/s");
        buffer.append("\n   throughput : " + 
                ((8 * numBytesReceived) / (1024.0 * 1024.0)) / receiveTime
                + "Mb/s");
        logger.info(buffer.toString());
    }
    
    /**
     * Destroy all RTI DDS communication entities.
     */
    protected void shutdown() {
        if(participant != null) {
            participant.delete_contained_entities();

            DomainParticipantFactory.TheParticipantFactory.
                delete_participant(participant);
        }
        /* RTI Data Distribution Service provides finalize_instance()
         method for people who want to release memory used by the
         participant factory singleton. Uncomment the following block of
         code for clean destruction of the participant factory
         singleton. */
        
        //DomainParticipantFactory.finalize_instance();
    }
    
    /**
     * Parses Options, starts Logger, DDS entities, and Subscriber application. 
     * @param args Program Options
     */    
    public static void main(String[] args) {
        
        Thread.currentThread().setUncaughtExceptionHandler(
                new UncaughtExceptionHandler(){
                    public void uncaughtException(Thread t, Throwable e) {
                        System.out.println("caught in uncaught exception handler");
                        e.printStackTrace();
                    }});
        
        try {
            Opt.All.parseOptions(args);
        } catch(IllegalArgumentException iae) {
            iae.printStackTrace();
            logger.severe("Incorrect program arguments.");
            logger.severe("Usage : \n" + Opt.All.getPrintableDescription());
            System.exit(-1);
        }
        
        Level level = Level.parse(Opt.LOG_LEVEL.asString());
        Logger.getLogger("").setLevel(level);
        Handler[] handlers = Logger.getLogger("").getHandlers();
        for(int i = 0; i < handlers.length; i++) {
            handlers[i].setLevel(level);
        }
        
        logger.finest(Opt.All.toString());
        
        if(Opt.DEBUG.asBoolean()) {
            LogVerbosity verbosity = LogVerbosity.NDDS_CONFIG_LOG_VERBOSITY_WARNING;
            System.err.println("setting dds log level to : " + verbosity);
            com.rti.ndds.config.Logger.get_instance().set_verbosity(verbosity);
            com.rti.ndds.config.Logger.get_instance().set_verbosity_by_category(
                    LogCategory.NDDS_CONFIG_LOG_CATEGORY_API, 
                    verbosity);
            com.rti.ndds.config.Logger.get_instance().set_verbosity_by_category(
                    LogCategory.NDDS_CONFIG_LOG_CATEGORY_COMMUNICATION, 
                    verbosity);
            com.rti.ndds.config.Logger.get_instance().set_verbosity_by_category(
                    LogCategory.NDDS_CONFIG_LOG_CATEGORY_DATABASE, 
                    verbosity);
            com.rti.ndds.config.Logger.get_instance().set_verbosity_by_category(
                    LogCategory.NDDS_CONFIG_LOG_CATEGORY_ENTITIES, 
                    verbosity);
            com.rti.ndds.config.Logger.get_instance().set_verbosity_by_category(
                    LogCategory.NDDS_CONFIG_LOG_CATEGORY_PLATFORM, 
                    verbosity);
        }
        
        FileSegmentSubscriber fileSegmentSubscriber = new FileSegmentSubscriber();
        
        if (!fileSegmentSubscriber.setupRTIDDS()) {
            return;
        }

        // readMessages() currently returns only on error.
        // If a hook is added in shutdown(), ensure readMessages() is updated
        fileSegmentSubscriber.readMessages();
        
        fileSegmentSubscriber.shutdown();
    }
}

