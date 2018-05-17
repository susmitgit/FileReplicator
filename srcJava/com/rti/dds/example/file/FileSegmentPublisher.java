package com.rti.dds.example.file;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.rti.dds.domain.DomainParticipant;
import com.rti.dds.domain.DomainParticipantFactory;
import com.rti.dds.domain.DomainParticipantFactoryQos;
import com.rti.dds.domain.DomainParticipantQos;
import com.rti.dds.example.file.messages.FileFragment;
import com.rti.dds.example.file.messages.FileSegmentDataWriter;
import com.rti.dds.example.file.messages.FileSegmentTypeCode;
import com.rti.dds.example.file.messages.FileSegmentTypeSupport;
import com.rti.dds.example.util.DebugDomainParticipantListener;
import com.rti.dds.example.util.DebugTopicListener;
import com.rti.dds.example.util.ProgramOptions;
import com.rti.dds.example.util.ProgramOptions.Option;
import com.rti.dds.infrastructure.DurabilityQosPolicyKind;
import com.rti.dds.infrastructure.Duration_t;
import com.rti.dds.infrastructure.HistoryQosPolicyKind;
import com.rti.dds.infrastructure.LivelinessQosPolicyKind;
import com.rti.dds.infrastructure.PublishModeQosPolicyKind;
import com.rti.dds.infrastructure.RETCODE_ERROR;
import com.rti.dds.infrastructure.ReliabilityQosPolicyKind;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.infrastructure.TransportBuiltinKind;
import com.rti.dds.publication.DataWriterQos;
import com.rti.dds.publication.FlowControllerProperty_t;
import com.rti.dds.publication.FlowControllerSchedulingPolicy;
import com.rti.dds.publication.Publisher;
import com.rti.dds.publication.PublisherQos;
import com.rti.dds.topic.Topic;
import com.rti.dds.topic.TopicQos;
import com.rti.ndds.config.LogCategory;
import com.rti.ndds.config.LogVerbosity;

/**
 * FileSegmentPublisher establishes entities required to read, segment and
 * transmit files from the base directory.
 * <p>
 * In this thread, it sets up the DDS Participant, Publisher, Topic and
 * DataWriter objects. After initialization, the real work is done by two other
 * threads: {@link FileSegmenter} reads files from the base directory, splits
 * them into chunks and pushes these chunks to a segment queue.
 * {@link FileSegmentWriteThread} pops chunks from the segment queue and sends
 * them to subscribers via DDS.
 */

public class FileSegmentPublisher {

    /**
     * Standard java.util logger
     */
    private static final Logger logger = Logger
            .getLogger(FileSegmentPublisher.class.getName());

    /**
     * FileSegmentPublisher command-line options.
     */
    static final class Opt {
        /**
         * Used to manipulate Options as a group.
         */
        static ProgramOptions All = new ProgramOptions();

        /**
         * Enables certain debug output. There are many other verbosity options.
         * See the <code>Debug*Listeners</code> in the util package, in
         * particular.
         */
        static final Option DEBUG = Option.makeBooleanOption(All, "debug",
                false);

        /**
         * Specifies domain ID for participant (0 by default).
         */
        static final Option DOMAIN_ID = Option.makeOption(All, "domainID",
                int.class, "0");

        /**
         * Specifies whether DISABLE_UDP should be used as a transport.
         */
        static final Option UDP = Option.makeBooleanOption(All, "noudp", true);

        /**
         * Specifies whether shared memory should be used as a transport.
         */
        static final Option SHMEM = Option.makeBooleanOption(All, "noshmem",
                true);

        /**
         * Root directory from which to publish files.
         */
        static final Option BASE_DIRECTORY = Option.makeStringOption(All,
                "baseDir", "original");

        /**
         * Determines whether the files in subdirectories will be published or
         * not.
         */
        static final Option RECURSE_SUBDIRECTORIES = Option.makeBooleanOption(
                All, "norecurse", true);

        /**
         * If true, files modified after transfer will be resent. Note that a
         * new reader will always cause all files to be resent anyway.
         */
        static final Option RESEND_MODIFIED_FILES = Option.makeBooleanOption(
                All, "noresend", true);

        /**
         * Specifies topic string.
         */
        static final Option TOPIC = Option.makeStringOption(All, "topic",
                "file_replicator");

        /**
         * Size of file segment blocking queue lying between Segmenter and
         * WriteThread.
         */
        static final Option QUEUE_SIZE = Option.makeOption(All,
                "segmentQueueSize", int.class, "200");

        /**
         * Regular expression specifying files to be included. Overrides exclude
         * filter.
         */
        static final Option INCLUDE_FILTER = Option.makeStringOption(All,
                "includeFilter", "");

        /**
         * Regular expression specifying files to be excluded.
         */
        static final Option EXCLUDE_FILTER = Option.makeStringOption(All,
                "excludeFilter", "");

        /**
         * Enables default asynchronous publication.
         */
        static final Option ASYNC_PUB = Option.makeBooleanOption(All,
                "asyncPub", false);

        /**
         * Send rate (implies asynchronous publication). Units are messages per
         * second for shared memory, Mb per second for DISABLE_UDP.
         */
        static final Option SEND_RATE = Option.makeOption(All, "sendRate",
                int.class, "-1");

        /**
         * Log Level as a string. See <code>java.util.logging.Logger</code>
         */
        static final Option LOG_LEVEL = Option.makeStringOption(All,
                "logLevel", "INFO");

        /**
         * If true, data from files is ignored and generated content sent
         * instead. The file is still accessed to read metadata.
         */
        static final Option FAKE_READ = Option.makeBooleanOption(All,
                "fakeRead", false);

        /**
         * Size of sample queue lying between WriteThread and DDS.
         */
        static final Option DDS_QSIZE = Option.makeOption(All, "DDSQueueSize",
                int.class, "25");

        /**
         * Size of transmit transport buffers.
         */
        static final Option TRANSPORT_QUEUE_SIZE = Option.makeOption(All,
                "transportQueueSize", int.class, "20");

        /**
         * Interfaces which should be denied for use.
         */
        static final Option UDP_DENY_INTERFACE_LIST = Option.makeOption(All,
                "udpDenyInteraceList", String.class, "10.10.*");
    }

    /**
     * DDS Domain Particpant. Declared here so we can access it in shutdown();
     */
    private DomainParticipant participant;

    /**
     * Blocking queue that acts as communication channel between FileSegmenter
     * and the write thread.
     */
    private final ArrayBlockingQueue<FileFragment> queue;

    /**
     * The FileSegmentPublisher merely starts a thread that monitors the segment
     * queue size. The real working threads get started in <code>main()</code>.
     */
    protected FileSegmentPublisher() {
        queue = new ArrayBlockingQueue<FileFragment>(Opt.QUEUE_SIZE.asInt());
        Runnable queueMonitor = new Runnable() {
            public void run() {
                int size = 0;
                while (true) {
                    if (size != queue.size()) {
                        logger.fine("queue size changed from " + size + " to "
                                + queue.size());
                        size = queue.size();
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ie) {
                    }
                }
            }
        };
        new Thread(queueMonitor).start();
    }

    /**
     * Initializes DDS entities, starts the Segmenter and Write threads.
     */
    protected void setupRTIDDS() {

        try {
            createParticipant();
            Publisher publisher = createPublisher();
            Topic topic = createTopic();

            String baseDir = Opt.BASE_DIRECTORY.asString();
            File baseDirectory = new File(baseDir).getAbsoluteFile();
            if (!baseDirectory.exists()) {
                if (!baseDirectory.mkdirs()) {
                    logger.severe("Cannot create base directory : "
                            + baseDirectory);
                    throw new RuntimeException(
                            "cannot create needed directories!");
                }
            }
            FileSegmenter fileSegmenter = new FileSegmenter(baseDirectory,
                    queue);
            fileSegmenter.start();

            // create the thread that will send the samples on DDS
            FileSegmentWriteThread fileSegmentWriteThread = new FileSegmentWriteThread(
                    queue);

            // ensure that the file segmenter and file segment write thread
            // objects are created before creating the data writer (because the
            // attached data writer listener needs them).
            FileSegmentDataWriter dataWriter = createDataWriter(
                    fileSegmentWriteThread, fileSegmenter, publisher, topic);

            // We set the dataWriter associated with the WriteThread in a sort
            // of roundabout
            // method, since the dataWriter needs a handle to the writeThread.
            // This access is
            // necessary so the WriterListener (established when the dataWriter
            // is constructed)
            // can notify the WriteThread when new Readers have been found.
            fileSegmentWriteThread.setWriter(dataWriter);
            fileSegmentWriteThread.start();

            // remember to enable the participant
            participant.enable();

        } catch (RETCODE_ERROR error) {
            error.printStackTrace();
            throw new RuntimeException("an unrecoverable error occurred");
        }
    }

    /**
     * Set up Participant QoS and transport parameters. Initializes
     * <code>participant</code> if successful, and sets it to to
     * <code>null</code> otherwise.
     */
    protected void createParticipant() {
        DomainParticipantFactory factory = DomainParticipantFactory
                .get_instance();
        DomainParticipantFactoryQos domainParticipantFactoryQos = new DomainParticipantFactoryQos();
        factory.get_qos(domainParticipantFactoryQos);

        // Change the QosPolicy to create disabled participants, since we can't
        // modify the transport properties of an enables participant.
        // Don't forget to enable the participant later--we do it in
        // setupRTIDDS()
        domainParticipantFactoryQos.entity_factory.autoenable_created_entities = false;

        factory.set_qos(domainParticipantFactoryQos);

        DomainParticipantQos participantQos = new DomainParticipantQos();
        factory.get_default_participant_qos(participantQos);

        // increase the size of the typecode that we can handle
        int typeCodeSize = 0;
        for (int i = 0; i <= 7; i++) {
            typeCodeSize = Math.max(typeCodeSize,
                    FileSegmentTypeCode.VALUE.get_serialized_size(i));
        }
        participantQos.resource_limits.type_object_max_serialized_length = 2 * typeCodeSize;
        participantQos.resource_limits.type_object_max_deserialized_length = 2 * typeCodeSize;
        participantQos.resource_limits.deserialized_type_object_dynamic_allocation_threshold = 2 * typeCodeSize;

        // assert liveliness every 2 and a half seconds
        participantQos.discovery_config.participant_liveliness_assert_period.sec = 2;
        participantQos.discovery_config.participant_liveliness_assert_period.nanosec = 500000000;

        // allow liveliness to time out at 10 seconds (so that we can
        // find out that there are no readers and stop sending data)
        participantQos.discovery_config.participant_liveliness_lease_duration.sec = 10;
        participantQos.discovery_config.participant_liveliness_lease_duration.nanosec = 0;

        final boolean enableUdp = Opt.UDP.asBoolean();
        final boolean enableShmem = Opt.SHMEM.asBoolean();

        // set the mask for the transport(s) selected
        participantQos.transport_builtin.mask = (enableUdp ? TransportBuiltinKind.UDPv4
                : 0)
                | (enableShmem ? TransportBuiltinKind.SHMEM : 0);

        logger.fine("transport(s) in use :" + (enableUdp ? " DISABLE_UDP" : "")
                + (enableShmem ? " DISABLE_SHMEM" : ""));

        // this listener can be configured to print only those events of
        // interest by using the constructor that takes a printMask.
        DebugDomainParticipantListener debugParticipantListener = new DebugDomainParticipantListener();
        participant = factory.create_participant(Opt.DOMAIN_ID.asInt(),
                participantQos, debugParticipantListener,
                StatusKind.STATUS_MASK_ALL);
    }

    /**
     * Creates and returns Publisher with default QoS.
     * 
     * @return new Publisher
     */
    protected Publisher createPublisher() {
        PublisherQos publisherQos = new PublisherQos();
        participant.get_default_publisher_qos(publisherQos);

        /*
         * change the publisherQos here. Because this program doesn't need to
         * adjust the defaults, we pass the unmodified publisherQoS here.
         * Alternatively, we could call participant.create_publisher(
         * DomainParticipant.PUBLISHER_QOS_DEFAULT, null,
         * StatusKind.STATUS_MASK_NONE);
         */

        return participant.create_publisher(publisherQos, null,
                StatusKind.STATUS_MASK_NONE);
    }

    /**
     * Creates and returns Topic with default QoS, and sets DebugTopicListener
     * to receive associated callbacks.
     * 
     * @return new Topic
     */
    protected Topic createTopic() {
        // Register the type before creating topic
        String typeName = FileSegmentTypeSupport.get_type_name();
        FileSegmentTypeSupport.register_type(participant, typeName);

        String topicName = Opt.TOPIC.asString();

        TopicQos topicQos = new TopicQos();
        participant.get_default_topic_qos(topicQos);
        // as with the publisherQos, this program doesn't need to adjust any of
        // the defaults

        // this listener can be configured to print only those events of
        // interest by using the constructor that takes a printMask.
        DebugTopicListener debugTopicListener = new DebugTopicListener();
        return participant.create_topic(topicName, typeName, topicQos,
                debugTopicListener, StatusKind.STATUS_MASK_ALL);
    }

    /**
     * Sets up DataWriter QoS, then creates and returns a new DataWriter.
     * 
     * @param fileSegmentWriteThread
     *            thread that will use this DataWriter to send samples
     * @param fileSegmenter
     *            thread that splits files to be passed to WriteThread
     * @param publisher
     *            Publisher for this DataWriter
     * @param topic
     *            Topic for this DataWriter
     * @return new DataWriter
     */
    protected FileSegmentDataWriter createDataWriter(
            FileSegmentWriteThread fileSegmentWriteThread,
            FileSegmenter fileSegmenter, Publisher publisher, Topic topic) {
        DataWriterQos datawriterQos = new DataWriterQos();
        publisher.get_default_datawriter_qos(datawriterQos);

        datawriterQos.liveliness.kind = LivelinessQosPolicyKind.AUTOMATIC_LIVELINESS_QOS;
        datawriterQos.liveliness.lease_duration.sec = 1;
        datawriterQos.liveliness.lease_duration.nanosec = 0;

        // The FileSegmenter will resend processed files when a new reader
        // appears, so it makes no differences if we save the past samples
        datawriterQos.durability.kind = DurabilityQosPolicyKind.TRANSIENT_LOCAL_DURABILITY_QOS;

        datawriterQos.reliability.kind = ReliabilityQosPolicyKind.RELIABLE_RELIABILITY_QOS;

        /*
         * push_on_write = true means samples are sent on the wire when write()
         * is called. When false, the DataWriter will only send samples in
         * response to a NACK from the DataReader. The biggest advantage is that
         * small samples can be coalesced into fewer network packets. Since
         * we're using samples almost as large as the maximum transport message
         * size anyway, setting this false will have negligible impact.
         * 
         * Note that asynchronous publishing does not currently work if
         * push_on_write is set to false.
         */
        datawriterQos.protocol.push_on_write = true;

        /*
         * This sets the maximum size of repair packets, send when readers
         * report missing samples. A high value lets the reader catch up on
         * samples quickly, but also risks flooding the network. For example, if
         * many samples are dropped, the reader will send NACKs requesting
         * resends. If many of these repair resends are dropped as well, they
         * will be re-NACKed, leading to a cycle of resend bursts and drops.
         * Lowering max_bytes_per_nack_response limits the resend burst rate,
         * alleviating this problem.
         */
        // datawriterQos.protocol.rtps_reliable_writer.max_bytes_per_nack_response
        // = maximumMessageSize * 30;

        final int max_samples = Opt.DDS_QSIZE.asInt();
        datawriterQos.resource_limits.max_samples = max_samples;
        // Go ahead and set initial_samples = max_samples so we won't have to
        // realloc. This could be set lower, if we expect Readers to process
        // data fast enough to keep the Write queue size low.
        datawriterQos.resource_limits.initial_samples = max_samples;

        // Only one instance
        datawriterQos.resource_limits.initial_instances = 1;
        datawriterQos.resource_limits.max_instances = 1;
        datawriterQos.resource_limits.max_samples_per_instance = max_samples;

        // Send one piggyback heartbeat for every max_samples/8 samples
        // Increasing this number means we'll get ACKNACKs sooner, so we can
        // resend samples readers missed, or drop samples that all readers have
        // received from the DDS queue. This allows us to have smaller queues,
        // at the cost of slightly increased overhead.

        datawriterQos.protocol.rtps_reliable_writer.heartbeats_per_max_samples = Math
                .min(8, max_samples);

        datawriterQos.protocol.rtps_reliable_writer.high_watermark = 1;
        datawriterQos.protocol.rtps_reliable_writer.low_watermark = 0;
        // heartbeat once every 3 seconds
        datawriterQos.protocol.rtps_reliable_writer.heartbeat_period.sec = 3;
        datawriterQos.protocol.rtps_reliable_writer.heartbeat_period.nanosec = 0;
        // fast heartbeat every 100 milliseconds
        datawriterQos.protocol.rtps_reliable_writer.fast_heartbeat_period.sec = 0;
        datawriterQos.protocol.rtps_reliable_writer.fast_heartbeat_period.nanosec = 100000000;

        // If a Reader doesn't respond for this number of heartbeats, we assume
        // it's dead.
        // We set this high, because temporarily dropping a reader is
        // expensive--all
        // Readers are assumed to be new, and thus restart processing of all
        // files.
        datawriterQos.protocol.rtps_reliable_writer.max_heartbeat_retries = 25;

        // block forever if the reader's queue is full for reliable transmission
        datawriterQos.history.kind = HistoryQosPolicyKind.KEEP_ALL_HISTORY_QOS;
        datawriterQos.reliability.max_blocking_time.sec = Duration_t.DURATION_INFINITY_SEC;
        datawriterQos.reliability.max_blocking_time.nanosec = Duration_t.DURATION_INFINITY_NSEC;

        /*
         * check if asynchronous publishing is requested. sendRate > 0 implies
         * async pub.
         * 
         * Note that asynchronous publishing does not currently work if
         * push_on_write is set to false.
         */

        final int sendRate = Opt.SEND_RATE.asInt();
        final boolean asynchPub = Opt.ASYNC_PUB.asBoolean() || (sendRate > 0);
        if (asynchPub) {
            datawriterQos.publish_mode.kind = PublishModeQosPolicyKind.ASYNCHRONOUS_PUBLISH_MODE_QOS;
        }

        if (sendRate > 0) {
            logger.fine("setting up for a controlled send rate");

            FlowControllerProperty_t fcp = new FlowControllerProperty_t();
            fcp.scheduling_policy = FlowControllerSchedulingPolicy.RR_FLOW_CONTROLLER_SCHED_POLICY;

            /*
             * These settings determine the steady-state throughput.
             * 
             * Where fragmentation is required, the fragment size will be
             * bytes_per_token or the minimum largest message size across all
             * transports installed with the DataWriter, whichever is less.
             */

            fcp.token_bucket.period.sec = 0;
            fcp.token_bucket.period.nanosec = 100 * 1000 * 1000; // 100ms
            fcp.token_bucket.bytes_per_token = 4096;

            /*
             * tokens_added_per_period will be proportional to sendRate, but we
             * want the units of sendRate to be something reasonable, say Mbps,
             * so we scale it by K. To find K, suppose sendRate = 1... 1
             * Mb_per_sec = Mb_per_byte * bytes_per_token * tokens_per_period *
             * periods_per_sec 1 = 8/(1024*1024) * 4096 * K * 10
             * 
             * K = (1024 * 1024)/(8 * 4096 * 10) K = 3.2
             * 
             * So, these particular values allow 3.2*sendRate*4096B per 100ms =
             * 32*sendRate*4KB per sec = sendRate Mbps
             */
            fcp.token_bucket.tokens_added_per_period = (int) Math
                    .round(3.2 * sendRate);

            /*
             * Setting the tokens_leaked_per_period to 0 will allow tokens to
             * accumulate. If you want to allow a short high-burst of data being
             * sent, you can increase the max_tokens to a level above the
             * tokens_added_per_period. However, we just want to put an upper
             * limit on the amount of bandwidth ever used. Therefore, we will
             * not allow tokens to build up above tokens_added_per_period.
             */
            fcp.token_bucket.tokens_leaked_per_period = 0;
            fcp.token_bucket.max_tokens = fcp.token_bucket.tokens_added_per_period;

            final String flowControllerName = "controlledRate";
            participant.create_flowcontroller(flowControllerName, fcp);

            logger.fine("fcp : " + fcp);

            datawriterQos.publish_mode.flow_controller_name = flowControllerName;
            datawriterQos.publish_mode.kind = PublishModeQosPolicyKind.ASYNCHRONOUS_PUBLISH_MODE_QOS;
        }

        // this listener can be configured to print only those events of
        // interest by using the constructor that takes a printMask.
        final int mask = StatusKind.STATUS_MASK_ALL
                & ~StatusKind.RELIABLE_WRITER_CACHE_CHANGED_STATUS;
        ReaderTrackingWriterListener readerTrackingWriterListener = new ReaderTrackingWriterListener(
                fileSegmenter, queue, fileSegmentWriteThread, mask);
        return (FileSegmentDataWriter) publisher.create_datawriter(topic,
                datawriterQos, readerTrackingWriterListener,
                StatusKind.STATUS_MASK_ALL);
    }

    /**
     * Stops and deletes DDS entities associated with the Participant.
     */
    protected void shutdown() {
        if (participant != null) {
            participant.delete_contained_entities();

            DomainParticipantFactory.TheParticipantFactory
                    .delete_participant(participant);
        }
        /*
         * RTI Data Distribution Service provides finalize_instance() method for
         * people who want to release memory used by the participant factory
         * singleton. Uncomment the following block of code for clean
         * destruction of the participant factory singleton.
         */
        // DomainParticipantFactory.finalize_instance();
    }

    /**
     * Parses Options, starts Logger, DDS entities, and Publisher application.
     * 
     * @param args
     *            Program Options
     */
    public static void main(String[] args) throws Exception {

        try {
            Opt.All.parseOptions(args);
        } catch (IllegalArgumentException iae) {
            // need to use System err since the logger isn't set up yet
            System.err.println("Unknown option: " + iae.getMessage());
            System.err.println("Usage: \n" + Opt.All.getPrintableDescription());
            System.exit(-1);
        }

        Level level = Level.parse(Opt.LOG_LEVEL.asString());
        Logger.getLogger("").setLevel(level);
        Handler[] handlers = Logger.getLogger("").getHandlers();
        Formatter myFormatter = new Formatter() {
            public String format(LogRecord record) {
                return new Date(record.getMillis()).toString() + " : "
                        + record.getLevel() + " : " + record.getLoggerName()
                        + " : " + record.getSourceMethodName() + "() : "
                        + record.getMessage() + "\n";
            }
        };
        for (int i = 0; i < handlers.length; i++) {
            handlers[i].setLevel(level);
            handlers[i].setFormatter(myFormatter);
        }

        logger.finest(Opt.All.toString());

        if (Opt.DEBUG.asBoolean()) {
            com.rti.ndds.config.Logger.get_instance()
                    .set_verbosity_by_category(
                            LogCategory.NDDS_CONFIG_LOG_CATEGORY_API,
                            LogVerbosity.NDDS_CONFIG_LOG_VERBOSITY_STATUS_ALL);
            com.rti.ndds.config.Logger.get_instance()
                    .set_verbosity_by_category(
                            LogCategory.NDDS_CONFIG_LOG_CATEGORY_COMMUNICATION,
                            LogVerbosity.NDDS_CONFIG_LOG_VERBOSITY_STATUS_ALL);
            com.rti.ndds.config.Logger.get_instance()
                    .set_verbosity_by_category(
                            LogCategory.NDDS_CONFIG_LOG_CATEGORY_DATABASE,
                            LogVerbosity.NDDS_CONFIG_LOG_VERBOSITY_STATUS_ALL);
            com.rti.ndds.config.Logger.get_instance()
                    .set_verbosity_by_category(
                            LogCategory.NDDS_CONFIG_LOG_CATEGORY_ENTITIES,
                            LogVerbosity.NDDS_CONFIG_LOG_VERBOSITY_STATUS_ALL);
            com.rti.ndds.config.Logger.get_instance()
                    .set_verbosity_by_category(
                            LogCategory.NDDS_CONFIG_LOG_CATEGORY_PLATFORM,
                            LogVerbosity.NDDS_CONFIG_LOG_VERBOSITY_STATUS_ALL);
        }

        // This is this classes' constructor; the DDS Publisher is created
        // in setupRTIDDS()
        FileSegmentPublisher fileSegmentPublisher = new FileSegmentPublisher();

        fileSegmentPublisher.setupRTIDDS();

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                // this is not a problem
            }
        }
        // the shutdown method is commented out right now since there is no
        // shutdown hook implemented
        // fileSegmentPublisher.shutdown();
    }
}
