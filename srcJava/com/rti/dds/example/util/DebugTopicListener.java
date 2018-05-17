/**
 * 
 */
package com.rti.dds.example.util;

import java.util.logging.Logger;

import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.topic.InconsistentTopicStatus;
import com.rti.dds.topic.Topic;
import com.rti.dds.topic.TopicListener;

/**
 * @author ken
 *
 */
public class DebugTopicListener implements TopicListener {

    private static final Logger logger = 
        Logger.getLogger(DebugTopicListener.class.getName());
    
    private int printMask;
    
    /**
     * Create a <code>DebugTopicListener</code> that prints all callback
     * calls. Callbacks that you want printed should be ORed into this printMask from
     * values found in <code>StatusKind</code>.
     */
    public DebugTopicListener() {
        this(StatusKind.STATUS_MASK_ALL);
    }
    
    /**
     * Create a <code>DebugTopicListener</code> with the specified
     * printMask. Callbacks that you want printed should be ORed into this printMask from
     * values found in <code>StatusKind</code>.
     */
    public DebugTopicListener(int printMask) {
        super();
        this.printMask = printMask;
    }

    /* (non-Javadoc)
     * @see com.rti.dds.topic.TopicListener#on_inconsistent_topic(com.rti.dds.topic.Topic, com.rti.dds.topic.InconsistentTopicStatus)
     */
    public void on_inconsistent_topic(Topic topic, 
                                      InconsistentTopicStatus status) {
        if ((printMask & StatusKind.INCONSISTENT_TOPIC_STATUS) > 0) {
            logger.fine(
                "DebugTopicListener : " +
                "on_inconsistent_topic() : " +
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

}
