package com.rti.dds.example.file;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Logger;

import com.rti.dds.example.file.messages.FileFragment;
import com.rti.dds.example.util.DebugDataWriterListener;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.publication.DataWriter;
import com.rti.dds.publication.PublicationMatchedStatus;

/**
 * ReaderTrackingWriterListener handles </code>on_publication_matched</code>
 * callbacks.  When readers join or leave, it notifies the FileSegmenter
 * and FileSegmentWriteThread so they can adjus their processing accordingly.
 * 
 * @author ken
 */
public class ReaderTrackingWriterListener extends DebugDataWriterListener {
    /**
     * Standard java.util logger
     */    
    private static final Logger logger =
        Logger.getLogger(ReaderTrackingWriterListener.class.getName());

    /**
     * Number of readers as of the last on_publication_matched()
     */
    protected int readerCurrentCount;
    /**
     * Handle to Segmenter, so we can notify it to start or stop
     * processing files
     */    
    private final FileSegmenter fileSegmenter;
    /**
     * Queue containing FileSegments waiting to be written to DDS.
     * We clear it as necessary here to save some work.
     */    
    private final ArrayBlockingQueue<FileFragment> queue;
    /**
     * Handle to WriteThread, so we can notify it to start or stop
     * sending FileSegments
     */    
    private final FileSegmentWriteThread fileSegmentWriteThread;
    
    /**
     * Constructs Listener that logs all status callbacks.
     */
    public ReaderTrackingWriterListener(
            FileSegmenter fileSegmenter, 
            ArrayBlockingQueue<FileFragment> queue,
            FileSegmentWriteThread fileSegmentWriteThread) {
        this(fileSegmenter, queue, 
             fileSegmentWriteThread, StatusKind.STATUS_MASK_ALL);
    }

    /**
     * Constructs Listener that only prints the status callbacks specified by printMask.
     * @param printMask Mask that maps directly to bits in <code>StatusKind</code>.
     */
    public ReaderTrackingWriterListener(
            FileSegmenter fileSegmenter, 
            ArrayBlockingQueue<FileFragment> queue,
            FileSegmentWriteThread fileSegmentWriteThread,
            int printMask) {
        super(printMask);
        this.fileSegmenter = fileSegmenter;
        this.queue = queue;
        this.fileSegmentWriteThread = fileSegmentWriteThread;
        readerCurrentCount = 0;
    }

    /**
     * Overridden to keep track of how many readers are currently alive.
     */
    public void on_publication_matched(DataWriter dataWriter, 
                                       PublicationMatchedStatus status) {
        super.on_publication_matched(dataWriter, status);
        int newReaderCount = status.current_count;
        logger.info("publication matched");
        if (newReaderCount > readerCurrentCount) {
            logger.info("A new reader has been discovered, informing segmenter");
            // Segmenter needs to know about new readers so it can resend files
            fileSegmenter.foundNewReader();
        }
        if (newReaderCount > 0 && readerCurrentCount == 0) {
            logger.info("The number of readers is now greater than zero.");
            // clear the queue, start processing files and sending messages
            queue.clear();
            fileSegmenter.setProcessingFiles(true);
            fileSegmentWriteThread.setSendMessages(true);
        }
        if (newReaderCount == 0 && readerCurrentCount > 0) {
            logger.info("The number of readers has gone to zero.");
            // don't send messages unless there are readers out there to get them            
            fileSegmenter.setProcessingFiles(false);
            fileSegmentWriteThread.setSendMessages(false);
            queue.clear();
        }
        readerCurrentCount = status.current_count;
    }

}
