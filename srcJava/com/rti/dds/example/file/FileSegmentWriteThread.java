package com.rti.dds.example.file;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.rti.dds.example.file.messages.FileFragment;
import com.rti.dds.example.file.messages.FileSegmentDataWriter;
import com.rti.dds.infrastructure.InstanceHandle_t;
import com.rti.dds.publication.ReliableWriterCacheChangedStatus;

/**
 * This class takes data samples from the queue and writes them to DDS. It can
 * optionally print throughput statistics.
 * 
 * @author ken
 */
public class FileSegmentWriteThread extends Thread {
    /**
     * Standard java.util logger
     */
    private static final Logger logger = Logger
            .getLogger(FileSegmentWriteThread.class.getName());

    /**
     * The queue of data samples to be sent.
     */
    private final ArrayBlockingQueue<FileFragment> queue;

    /**
     * The <code>FileSegmentDataWriter</code> which is used to write samples.
     */
    private FileSegmentDataWriter writer;

    /**
     * This field indicates whether this thread should be sending messages or
     * not.
     */
    private boolean sendMessages;

    /**
     * The total count of samples sent.
     */
    private int samplesSent;

    /**
     * The total count of bytes sent.
     */
    private int bytesSent;

    /**
     * This field indicates if this thread should continue to run or not.
     */
    private boolean shouldRun;

    /**
     * This boolean indicates if an internal monitoring thread should be spawned
     * or not. It is primarily useful for tuning and debugging and should
     * otherwise be turned off.
     */
    private final boolean spawnSampleMonitor = false;

    /**
     * Constructs a <code>FileSegmentWriteThread</code>. Ensure that the
     * <code>writer</code> gets set before allowing this thread to write data
     * (or else it will result in a <code>NullPointerException</code>).
     * 
     * @param queue
     *            The queue from which to get data samples.
     */
    public FileSegmentWriteThread(ArrayBlockingQueue<FileFragment> queue) {
        this.queue = queue;
        sendMessages = false;
        samplesSent = 0;
        bytesSent = 0;
        shouldRun = true;

        // if desired, spawn a thread to monitor the sample rate
        if (spawnSampleMonitor) {
            new Thread("sample monitor") {
                private boolean queueWasZero = false;

                public void run() {
                    while (true) {
                        int samplesSentAtStart = 
                                FileSegmentWriteThread.this.samplesSent;
                        int bytesSentAtStart = 
                                FileSegmentWriteThread.this.bytesSent;
                        long startTime = System.currentTimeMillis();
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            // nothing to do
                        }
                        int samplesSentAtEnd = 
                                FileSegmentWriteThread.this.samplesSent;
                        int bytesSentAtEnd = 
                                FileSegmentWriteThread.this.bytesSent;
                        long endTime = System.currentTimeMillis();

                        int totalSamplesSent = samplesSentAtEnd
                                - samplesSentAtStart;
                        int totalBytesSent = bytesSentAtEnd - bytesSentAtStart;
                        long elapsedTime = endTime - startTime;
                        // only print out data if there is something interesting
                        // to report on...
                        if (!queueWasZero
                                || (FileSegmentWriteThread.this.queue.size() != 0)) {
                            double sampleRate = totalSamplesSent
                                    / ((double) elapsedTime / 1000);
                            double byteRate = totalBytesSent
                                    / ((double) elapsedTime);
                            logger.info("sample rate is : " + sampleRate
                                    + " messages/second (" + byteRate
                                    + " Kbps)");
                            logger.info("queue size is : "
                                    + FileSegmentWriteThread.this.queue.size());
                        }
                        queueWasZero = (FileSegmentWriteThread.this.queue
                                .size() == 0);
                    }
                }
            }.start();
        }
    }

    /**
     * This method contains the logic to take samples from the queue and sends
     * them through the <code>FileSegmentDataWriter</code>.
     */
    public void run() {
        ReliableWriterCacheChangedStatus status = new ReliableWriterCacheChangedStatus();
        while (shouldRun) {
            // attempt to get the next segment
            // Pop waits 10ms, so we're not just spinning here.
            FileFragment segment = null;
            try {
                segment = (FileFragment) queue.poll(10, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // not a problem
            }
            if (segment != null) {
                // only send messages if it's ok to
                while (!sendMessages) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        // there is nothing to do for this exception
                    }
                }

                writer.get_reliable_writer_cache_changed_status(status);

                // publish the segment
                if (segment.segmentNumber == 1) {
                    logger.info("start of sending DDS messages for file : "
                            + segment.fileDescription.name);
                }

                writer.write(segment, InstanceHandle_t.HANDLE_NIL);
                if (segment.segmentNumber == segment.totalSegmentCount) {
                    logger.info("finished sending DDS messages for file : "
                            + segment.fileDescription.name);
                }
                bytesSent += segment.contents.size();
                samplesSent++;
            }
        }
    }

    /**
     * Set whether or not to send messages.
     * 
     * @param sendMessages
     *            The sendMessages to set.
     */
    public void setSendMessages(boolean sendMessages) {
        // This is called only by ReaderTrackingWriterListener when the number
        // of readers is going to or coming from zero. It does not need to be
        // locked for correctness. The worst case is that run() has popped a
        // FileSegment, but sendMessages gets cleared before run() tests it.
        // When new a new reader joins, it will be sent the stale FileSegment,
        // which will be discarded anyway.
        this.sendMessages = sendMessages;
    }

    /**
     * Indicates whether the logic should continue running or not.
     * 
     * @param shouldRun
     *            true to continue running, false otherwise.
     */
    public void setShouldRun(boolean shouldRun) {
        this.shouldRun = shouldRun;
    }

    /**
     * @param writer
     *            The writer to set.
     */
    public void setWriter(FileSegmentDataWriter writer) {
        this.writer = writer;
    }
}
