/**
 * 
 */
package com.rti.dds.example.util;

import java.util.logging.Logger;

import com.rti.dds.domain.DomainParticipantListener;
import com.rti.dds.infrastructure.Cookie_t;
import com.rti.dds.infrastructure.InstanceHandle_t;
import com.rti.dds.infrastructure.Locator_t;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.publication.AcknowledgmentInfo;
import com.rti.dds.publication.DataWriter;
import com.rti.dds.publication.LivelinessLostStatus;
import com.rti.dds.publication.OfferedDeadlineMissedStatus;
import com.rti.dds.publication.OfferedIncompatibleQosStatus;
import com.rti.dds.publication.PublicationMatchedStatus;
import com.rti.dds.publication.ReliableReaderActivityChangedStatus;
import com.rti.dds.publication.ReliableWriterCacheChangedStatus;
import com.rti.dds.subscription.DataReader;
import com.rti.dds.subscription.LivelinessChangedStatus;
import com.rti.dds.subscription.RequestedDeadlineMissedStatus;
import com.rti.dds.subscription.RequestedIncompatibleQosStatus;
import com.rti.dds.subscription.SampleLostStatus;
import com.rti.dds.subscription.SampleRejectedStatus;
import com.rti.dds.subscription.Subscriber;
import com.rti.dds.subscription.SubscriptionMatchedStatus;
import com.rti.dds.topic.InconsistentTopicStatus;
import com.rti.dds.topic.Topic;

/**
 * @author ken
 *
 */
public class DebugDomainParticipantListener implements
        DomainParticipantListener {
    
    private static final Logger logger = 
        Logger.getLogger(DebugDomainParticipantListener.class.getName());
    
    protected int printMask;
    
    /**
     * Create a <code>DebugDomainParticipantListener</code> that prints all callback
     * calls. Callbacks that you want printed should be ORed into this printMask from
     * values found in <code>StatusKind</code>.
     */
    public DebugDomainParticipantListener() {
        this(StatusKind.STATUS_MASK_ALL);
    }
    
    /**
     * Create a <code>DebugDomainParticipantListener</code> with the specified
     * printMask. Callbacks that you want printed should be ORed into this printMask from
     * values found in <code>StatusKind</code>.
     */
    public DebugDomainParticipantListener(int printMask) {
        super();
        this.printMask = printMask;
    }

    /* (non-Javadoc)
     * @see com.rti.dds.topic.TopicListener#on_inconsistent_topic(com.rti.dds.topic.Topic, com.rti.dds.topic.InconsistentTopicStatus)
     */
    public void on_inconsistent_topic(Topic topic, InconsistentTopicStatus status) {
        if ((printMask & StatusKind.INCONSISTENT_TOPIC_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_inconsistent_topic() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.publication.DataWriterListener#on_offered_deadline_missed(com.rti.dds.publication.DataWriter, com.rti.dds.publication.OfferedDeadlineMissedStatus)
     */
    public void on_offered_deadline_missed(DataWriter dataWriter,
                                           OfferedDeadlineMissedStatus status) {
        if ((printMask & StatusKind.OFFERED_DEADLINE_MISSED_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_offered_deadline_missed() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.publication.DataWriterListener#on_offered_incompatible_qos(com.rti.dds.publication.DataWriter, com.rti.dds.publication.OfferedIncompatibleQosStatus)
     */
    public void on_offered_incompatible_qos(
            DataWriter dataWriter,
            OfferedIncompatibleQosStatus status) {
        if ((printMask & StatusKind.OFFERED_INCOMPATIBLE_QOS_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_offered_incompatible_qos() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.publication.DataWriterListener#on_liveliness_lost(com.rti.dds.publication.DataWriter, com.rti.dds.publication.LivelinessLostStatus)
     */
    public void on_liveliness_lost(DataWriter dataWriter, 
                                   LivelinessLostStatus status) {
        if ((printMask & StatusKind.LIVELINESS_LOST_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_liveliness_lost() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.publication.DataWriterListener#on_publication_matched(com.rti.dds.publication.DataWriter, com.rti.dds.publication.PublicationMatchedStatus)
     */
    public void on_publication_matched(DataWriter dataWriter,
                                       PublicationMatchedStatus status) {
        if ((printMask & StatusKind.PUBLICATION_MATCHED_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_publication_matched() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.publication.DataWriterListener#on_reliable_writer_cache_changed(com.rti.dds.publication.DataWriter, com.rti.dds.publication.ReliableWriterCacheChangedStatus)
     */
    public void on_reliable_writer_cache_changed(
            DataWriter dataWriter,
            ReliableWriterCacheChangedStatus status) {
        if ((printMask & StatusKind.RELIABLE_WRITER_CACHE_CHANGED_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_reliable_writer_cache_changed() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.publication.DataWriterListener#on_reliable_reader_activity_changed(com.rti.dds.publication.DataWriter, com.rti.dds.publication.ReliableReaderActivityChangedStatus)
     */
    public void on_reliable_reader_activity_changed(
            DataWriter dataWriter,
            ReliableReaderActivityChangedStatus status) {
        if ((printMask & StatusKind.RELIABLE_READER_ACTIVITY_CHANGED_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_reliable_reader_activity_changed() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.subscription.SubscriberListener#on_data_on_readers(com.rti.dds.subscription.Subscriber)
     */
    public void on_data_on_readers(Subscriber subscriber) {
    }

    /* (non-Javadoc)
     * @see com.rti.dds.subscription.DataReaderListener#on_requested_deadline_missed(com.rti.dds.subscription.DataReader, com.rti.dds.subscription.RequestedDeadlineMissedStatus)
     */
    public void on_requested_deadline_missed(
            DataReader dataReader,
            RequestedDeadlineMissedStatus status) {
        if ((printMask & StatusKind.REQUESTED_DEADLINE_MISSED_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_requested_deadline_missed() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.subscription.DataReaderListener#on_requested_incompatible_qos(com.rti.dds.subscription.DataReader, com.rti.dds.subscription.RequestedIncompatibleQosStatus)
     */
    public void on_requested_incompatible_qos(
            DataReader dataReader,
            RequestedIncompatibleQosStatus status) {
        if ((printMask & StatusKind.REQUESTED_INCOMPATIBLE_QOS_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_requested_incompatible_qos() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.subscription.DataReaderListener#on_sample_rejected(com.rti.dds.subscription.DataReader, com.rti.dds.subscription.SampleRejectedStatus)
     */
    public void on_sample_rejected(DataReader dataReader, SampleRejectedStatus status) {
        if ((printMask & StatusKind.SAMPLE_REJECTED_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_sample_rejected() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.subscription.DataReaderListener#on_liveliness_changed(com.rti.dds.subscription.DataReader, com.rti.dds.subscription.LivelinessChangedStatus)
     */
    public void on_liveliness_changed(DataReader dataReader,
                                      LivelinessChangedStatus status) {
        if ((printMask & StatusKind.LIVELINESS_CHANGED_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_liveliness_changed() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.subscription.DataReaderListener#on_data_available(com.rti.dds.subscription.DataReader)
     */
    public void on_data_available(DataReader dataReader) {
    }

    /* (non-Javadoc)
     * @see com.rti.dds.subscription.DataReaderListener#on_sample_lost(com.rti.dds.subscription.DataReader, com.rti.dds.subscription.SampleLostStatus)
     */
    public void on_sample_lost(DataReader dataReader, SampleLostStatus status) {
        if ((printMask & StatusKind.SAMPLE_LOST_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_sample_lost() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.subscription.DataReaderListener#on_subscription_matched(com.rti.dds.subscription.DataReader, com.rti.dds.subscription.SubscriptionMatchedStatus)
     */
    public void on_subscription_matched(DataReader dataReader,
                                        SubscriptionMatchedStatus status) {
        if ((printMask & StatusKind.SUBSCRIPTION_MATCHED_STATUS) > 0) {
            logger.fine(
                "DebugDomainParticipantListener : " +
                "on_subscription_matched() : " +
                status);
        }
    }

    /**
     * @return Returns the printMask.
     */
    public int getPrintMask() {
        return printMask;
    }

    /**
     * @param printMask The printMask to set.
     */
    public void setPrintMask(int printMask) {
        this.printMask = printMask;
    }

    @Override
    public void on_application_acknowledgment(DataWriter arg0,
            AcknowledgmentInfo arg1) {
    }

    @Override
    public Object on_data_request(DataWriter arg0, Cookie_t arg1) {
        return null;
    }

    @Override
    public void on_data_return(DataWriter arg0, Object arg1, Cookie_t arg2) {
    }

    @Override
    public void on_destination_unreachable(DataWriter arg0,
            InstanceHandle_t arg1, Locator_t arg2) {
    }

    @Override
    public void on_instance_replaced(DataWriter arg0, InstanceHandle_t arg1) {
    }

    @Override
    public void on_sample_removed(DataWriter arg0, Cookie_t arg1) {
    }
}
