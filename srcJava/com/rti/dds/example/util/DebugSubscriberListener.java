/**
 * 
 */
package com.rti.dds.example.util;

import java.util.logging.Logger;

import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.subscription.DataReader;
import com.rti.dds.subscription.LivelinessChangedStatus;
import com.rti.dds.subscription.RequestedDeadlineMissedStatus;
import com.rti.dds.subscription.RequestedIncompatibleQosStatus;
import com.rti.dds.subscription.SampleLostStatus;
import com.rti.dds.subscription.SampleRejectedStatus;
import com.rti.dds.subscription.Subscriber;
import com.rti.dds.subscription.SubscriberListener;
import com.rti.dds.subscription.SubscriptionMatchedStatus;

/**
 * @author ken
 *
 */
public class DebugSubscriberListener 
    extends DebugDataReaderListener implements SubscriberListener {
    
    private static final Logger logger = 
        Logger.getLogger(DebugSubscriberListener.class.getName());

    /**
     * Create a <code>DebugSubscriberListener</code> that prints all callback
     * calls. Callbacks that you want printed should be ORed into this printMask from
     * values found in <code>StatusKind</code>.
     */
    public DebugSubscriberListener() {
        super();
    }

    /**
     * Create a <code>DebugSubscriberListener</code> with the specified
     * printMask. Callbacks that you want printed should be ORed into this printMask from
     * values found in <code>StatusKind</code>.
     */
    public DebugSubscriberListener(int printMask) {
        super(printMask);
    }

    /* (non-Javadoc)
     * @see com.rti.dds.subscription.SubscriberListener#on_data_on_readers(com.rti.dds.subscription.Subscriber)
     */
    public void on_data_on_readers(Subscriber subscriber) {
        if ((printMask & StatusKind.DATA_ON_READERS_STATUS) > 0) {
            logger.fine(
                "DebugSubscriberListener : " +
                "on_data_on_readers() : " +
                subscriber);
        }
    }
    
    /* (non-Javadoc)
     * @see com.rti.dds.subscription.DataReaderListener#on_requested_deadline_missed(com.rti.dds.subscription.DataReader, com.rti.dds.subscription.RequestedDeadlineMissedStatus)
     */
    public void on_requested_deadline_missed(
            DataReader dataReader,
            RequestedDeadlineMissedStatus status) {
        if ((printMask & StatusKind.REQUESTED_DEADLINE_MISSED_STATUS) > 0) {
            logger.fine(
                "DebugSubscriberListener : " +
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
                "DebugSubscriberListener : " +
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
                "DebugSubscriberListener : " +
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
                "DebugSubscriberListener : " +
                "on_liveliness_changed() : " +
                status);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.subscription.DataReaderListener#on_data_available(com.rti.dds.subscription.DataReader)
     */
    public void on_data_available(DataReader dataReader) {
        if ((printMask & StatusKind.DATA_AVAILABLE_STATUS) > 0) {
            logger.fine(
                "DebugSubscriberListener : " +
                "on_data_available() : " +
                dataReader);
        }
    }

    /* (non-Javadoc)
     * @see com.rti.dds.subscription.DataReaderListener#on_sample_lost(com.rti.dds.subscription.DataReader, com.rti.dds.subscription.SampleLostStatus)
     */
    public void on_sample_lost(DataReader dataReader, SampleLostStatus status) {
        if ((printMask & StatusKind.SAMPLE_LOST_STATUS) > 0) {
            logger.fine(
                "DebugSubscriberListener : " +
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
                "DebugSubscriberListener : " +
                "on_subscription_matched() : " +
                status);
        }
    }

}
