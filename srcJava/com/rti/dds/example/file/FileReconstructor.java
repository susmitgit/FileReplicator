package com.rti.dds.example.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.rti.dds.example.file.FileSegmentSubscriber.Opt;
import com.rti.dds.example.file.messages.FileDescription;
import com.rti.dds.example.file.messages.FileDescriptionTypeSupport;
import com.rti.dds.example.file.messages.FileFragment;
import com.rti.dds.example.file.messages.MAX_FILE_SEGMENT_SIZE;

/**
 * This class reconstructs a file from <code>FileSegment</code> objects. It 
 * gets <code>FileSegment</code> objects from a queue and writes the data in
 * them to a file. This class checks for consistency in the 
 * <code>FileSegment</code>s that it processes. If <code>FileSegment</code>s
 * are received out of order, or if any are missing, then an error message will
 * be printed. Also, if the reconstructed file's size does not match the
 * description, an error message will be printed (this can occur if the 
 * publisher has application logic problems).
 * @author ken
 */
public class FileReconstructor extends Thread {
    /**
     * Standard java.util logger
     */    
    private static final Logger logger = 
        Logger.getLogger(FileReconstructor.class.getName());
    
    /**
     * The base directory to use for reconstructing files. All files will be
     * reconstructed relative to this directory.
     */
    private final File baseDirectory;
    
    /**
     * The queue supplying <code>FileSegment</code> objects.
     */
    private final ArrayBlockingQueue<FileFragment> queue;
    
    /**
     * An output stream for the current file. This member may be null if there
     * is no current file or if the file couldn't be created for writing.
     */
    private OutputStream outputStream;
    
    /**
     * A boolean indicating whether or not this object should continue to run.
     */
    private boolean shouldRun;
    
    /**
     * A local byte buffer for temporary storage of file data.
     */
    private final byte[] buffer;
    
    /**
     * The count of bytes written for the current file. This is used to check
     * the consistency between the files reconstructed and what their 
     * description states.
     */
    private long bytesWrittenCurrentFile;
    
    /**
     * The description of the current file. This member is used for consistency
     * checking.
     */
    private FileDescription currentFileDescription;
    
    /**
     * The next segment number that is expected. This member is used for
     * consistency checking.
     */
    private int nextSegmentNumberExpected;
    
    /**
     * Indicates whether there is an error with the current file.
     */
    private boolean errorWithCurrentFile;
    
    /**
     * The <code>File</code> object representing the file currently being
     * processed.
     */
    private File currentFile;

    /**
     * Tracks how often the Reconstructor is waiting on an empty queue.
     */
    private long queueEmpty;
    
    
    /**
     * Constructs a <code>FileReconstructor</code> with the given base directory
     * and queue. <code>FileSegment</code> objects coming through the queue must
     * be sequential and complete. That is, the <code>segmentNumber</code>s must
     * start at 1 and continue through <code>totalSegmentCount</code>.
     * @param baseDirectory The root directory to place reconstructed files.
     * @param queue The queue to read <code>FileSegment</code> data from.
     */
    public FileReconstructor(File baseDirectory, 
            ArrayBlockingQueue<FileFragment> queue) {
        super("FileReconstructor");
        setPriority(Thread.MAX_PRIORITY);         
        setDaemon(true); // this thread shouldn't hold up a shutdown
        this.baseDirectory = baseDirectory;
        this.queue = queue;
        shouldRun = true;
        buffer = new byte[MAX_FILE_SEGMENT_SIZE.VALUE];
        nextSegmentNumberExpected = 1;
        errorWithCurrentFile = false;
        queueEmpty = 0;
        currentFileDescription = (FileDescription) 
            FileDescriptionTypeSupport.getInstance().create_java_type_instance();
    }
    
    /**
     * Sets whether or not this object should continue to run.
     * @param shouldRun Whether or not this object should continue to run.
     */
    public void setShouldRun(boolean shouldRun) {
        this.shouldRun = shouldRun;
    }
    
    /**
     * This method contains the logic for reconstructing files from
     * <code>FileSegment</code> objects.
     * @see java.lang.Runnable#run()
     */
    public void run() {
        try{
            while(shouldRun) {
                FileFragment segment = (FileFragment) queue.poll(10, 
                        TimeUnit.MILLISECONDS);
                if (segment == null) {
                    // Don't process if timeout expired
                    queueEmpty++;
                    if(queueEmpty % 250 == 0)
                            logger.fine("queueEmpty = " + queueEmpty);
                    
                } else {                
                    processSegment(segment);
                }
            }
        }catch(Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * Process the given <code>FileSegment</code>. This method will start a new
     * file if the argument is the first segment. If the argument is the last
     * segment, then this method will close the file. In either case, the data
     * contained in the argument will be written to a file (barring any errors,
     * of course).
     * @param segment The segment to process.
     */
    private void processSegment(FileFragment segment) {
        if (segment.segmentNumber % 100 == 0) {
            logger.fine("Processing segment " + segment.segmentNumber +
                    " of " + segment.totalSegmentCount);           
        }
        
        // if this is a new file, cleanup any old files and start a new 
        // output stream
        if (segment.segmentNumber == 1) {
            resetForNextFile();
            closeStream();
            openStream(segment.fileDescription);
            currentFileDescription.copy_from(segment.fileDescription);
            errorWithCurrentFile = (outputStream == null);
            // get the start time so that the transfer time can be measured
            logger.info("Start of receive for file : " +  
                    segment.fileDescription.name);
        }
        
        // if there is an error with the current file, just return (don't annoy
        // with repeated error messages).
        if (errorWithCurrentFile) {
            return;
        }
        
        // if the segment isn't for the expected file, then we've got an error
        if (!currentFileDescription.equals(segment.fileDescription)) {
            // check to see if we got started in the middle of a transfer, this is
            // not technically an error
            if (segment.segmentNumber > 1) {
                logger.warning("File data didn't start from the beginning - got segment " + segment.segmentNumber +
                    "\n" + segment.fileDescription.toString("File Description:", 1));
                logger.warning("Was expecting segment " + nextSegmentNumberExpected + " of " + currentFileDescription.toString());
            } else {
                logger.warning("Unexpected segment encountered." +
                    "\n" + segment.fileDescription.toString("File Description:", 1));
            }
            resetForNextFile();
            errorWithCurrentFile = true;
            deleteCurrentFile();
            return;
        }
        
        // check if the incoming segment number is the expected one
        if (nextSegmentNumberExpected != segment.segmentNumber) {
            logger.warning("nextSegmentNumberExpected (" + 
                    nextSegmentNumberExpected + ") != segment.segmentNumber (" +
                    segment.segmentNumber + ")");
            resetForNextFile();
            errorWithCurrentFile = true;
            deleteCurrentFile();
            return;
        }
        
        // update the next expected segment number
        nextSegmentNumberExpected = segment.segmentNumber + 1;
        
        try {
            // get the contents of this segment
            segment.contents.toArrayByte(buffer);           
            // write the contents to the file
            if(!Opt.FAKE_WRITE.asBoolean()){
                outputStream.write(buffer, 0, segment.contents.size());
            }
            
            // update the number of bytes written
            bytesWrittenCurrentFile += segment.contents.size();
            
        } catch(IOException ioException) {
            ioException.printStackTrace();
            closeStream();
            errorWithCurrentFile = true;
            deleteCurrentFile();
        }
        
        // if this is the last segment, flush any pending data and close the file
        if (segment.segmentNumber == segment.totalSegmentCount
            && !errorWithCurrentFile) {
            if (segment.fileDescription.size != bytesWrittenCurrentFile) {
                logger.warning("Segment " + segment.segmentNumber + " segment.size (" + 
                        segment.fileDescription.size + ") != bytesWrittenCurrentFile (" +
                        bytesWrittenCurrentFile + ").");
            }
            if (closeStream()) {
                logger.info("Successfully wrote file : " +
                    segment.fileDescription.toString());
            } else {
                logger.warning("Failed to write file : " +
                        segment.fileDescription.toString());
            }
            resetForNextFile();
        }
    }
    
    /**
     * Reset internal state members to prepare for the start of a new file.
     */
    private void resetForNextFile() {
        bytesWrittenCurrentFile = 0;
        nextSegmentNumberExpected = 1;
        // reset the data for the current file
        currentFileDescription = (FileDescription) 
            FileDescriptionTypeSupport.getInstance().create_java_type_instance();
    }
    
    /**
     * Open (and create if necessary) a file to store data from incoming
     * <code>FileSegment</code> objects.
     * @param fileDescription Incoming file metadata
     */
    private void openStream(FileDescription fileDescription) {
        
        // ensure that the stream has been closed up properly
        closeStream();
        
        // check precondition
        if (fileDescription.name.length() < 1) {
            logger.warning("Cannot create file without a name.");
            return;
        }
        
        File directory = null;
        if (fileDescription.path.length() > 0) {
            directory = new File(baseDirectory, fileDescription.path);
        } else {
            directory = baseDirectory;
        }
        
        // if the directory (or directories) does not exist, attempt to create them
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                logger.warning("Failed to create necessary directory : " +
                        directory);
            }
        }
        
        // open the stream
        currentFile = new File(directory, fileDescription.name);
        try{            
            outputStream = new BufferedOutputStream(new FileOutputStream(currentFile));
        }catch(IOException ioException) {
            ioException.printStackTrace();
            outputStream = null;
        }
    }
    
    /**
     * Close the current stream.
     * @return true if the stream was successfully closed, false otherwise.
     */
    private boolean closeStream() {
        boolean success = true;
        if (outputStream != null) {
            try {                
                outputStream.flush();
                outputStream.close();
                outputStream = null;
                
            } catch(IOException ioException) {
                ioException.printStackTrace();
                success = false;
            }
        }
        return success;
    }
    
    /**
     * Delete the current file being processed.
     */
    private void deleteCurrentFile() {
        if (currentFile != null) {
            try {
                currentFile.delete();
            }catch(Exception e){
                logger.warning("Could not delete file : " + currentFile);
            }
        }
        currentFile = null;
    }
}
