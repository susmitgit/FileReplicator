/**
 * 
 */
package com.rti.dds.example.util;

import java.util.logging.Logger;

import com.rti.dds.infrastructure.Cookie_t;
import com.rti.dds.infrastructure.InstanceHandle_t;
import com.rti.dds.infrastructure.Locator_t;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.publication.AcknowledgmentInfo;
import com.rti.dds.publication.DataWriter;
import com.rti.dds.publication.DataWriterListener;
import com.rti.dds.publication.LivelinessLostStatus;
import com.rti.dds.publication.OfferedDeadlineMissedStatus;
import com.rti.dds.publication.OfferedIncompatibleQosStatus;
import com.rti.dds.publication.PublicationMatchedStatus;
import com.rti.dds.publication.ReliableReaderActivityChangedStatus;
import com.rti.dds.publication.ReliableWriterCacheChangedStatus;

/**
 * @author ken
 *
 */
public class DebugDataWriterListener implements DataWriterListener {
    
    private static final Logger logger = 
        Logger.getLogger(DebugDataWriterListener.class.getName());
    
    protected int printMask;
    
    /**
     * Create a <code>DebugDataWriterListener</code> that prints all callback
     * calls. Callbacks that you want printed should be ORed into this printMask from
     * values found in <code>StatusKind</code>.
     */
    public DebugDataWriterListener() {
        this(StatusKind.STATUS_MASK_ALL);
    }
    
    /**
     * Create a <code>DebugDataWriterListener</code> with the specified
     * printMask. Callbacks that you want printed should be ORed into this printMask from
     * values found in <code>StatusKind</code>.
     */
    public DebugDataWriterListener(int printMask) {
        this.printMask = printMask;
    }


    /* (non-Javadoc)
     * @see com.rti.dds.publication.DataWriterListener#on_offered_deadline_missed(com.rti.dds.publication.DataWriter, com.rti.dds.publication.OfferedDeadlineMissedStatus)
     */
    public void on_offered_deadline_missed(DataWriter dataWriter,
                                           OfferedDeadlineMissedStatus status) {
        if ((printMask & StatusKind.OFFERED_DEADLINE_MISSED_STATUS) > 0) {
            logger.fine(
                "DebugDataWriterListener : " +
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
                "DebugDataWriterListener : " +
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
                "DebugDataWriterListener : " +
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
                "DebugDataWriterListener : " +
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
                "DebugDataWriterListener : " +
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
                "DebugDataWriterListener : " +
                "on_reliable_reader_activity_changed() : " +
                status);
        }
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
}
