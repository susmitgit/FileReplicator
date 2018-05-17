package com.rti.dds.example.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.rti.dds.example.file.FileSegmentPublisher.Opt;
import com.rti.dds.example.file.messages.FileFragment;
import com.rti.dds.example.file.messages.MAX_FILE_SEGMENT_SIZE;

/**
 * This class finds files to be send over DDS and prepares messages to represent
 * those files. Since a particular file may contain more bytes than can be
 * represented in a single message, the primary function of this class is to
 * produce a message set appropriate for each file. The messages produced are
 * put in a blocking queue, which is in turn read by FileSegmentWriteThread.
 * <p>
 * This class is also responsible for searching the base directory for files to
 * send. Two structures are used for this. The first is a set of all found
 * files. As findAllFiles() scans the directory hierarchy, files are added to
 * this set; elements are never removed. The second structure is a queue of
 * files to be sent. Files are pushed onto this queue for three reasons: 1.)
 * Newly found files are added to this queue; 2.) Files are requeued if there
 * was a processing error on the previous attempt; 3.) Modified files are
 * requeued if enabled (See FileSegmentPublisher command-line options).
 * <p>
 * While running, we call getFileToSend() to pop a file from queuedFiles. If
 * queuedFiles is empty, call findAllFiles() to rescan the directory, and try
 * again. If queuedFiles is still empty, sleep for a bit and try again.
 * <p>
 * When we have a file to send, open an input stream and call chunkFile() to
 * split the file into segments and place them on the blocking queue. If the
 * blocking queue is full, chunkFiles() waits until it's empty to proceed, since
 * we want reliable file transmission. Iff the file was sent successfully, we
 * add the last modification time (read at the time of transfer) to a map.
 * findAllFiles uses this map to determine if a found file was already
 * processed, or whether a found file has been modified and needs to be resent.
 * <p>
 * Between file transmissions, we check if we've found a new reader. If so, we
 * assume it is truly new, and thus needs all of the previously sent files. To
 * do this, we clear queuedFiles and add to it everything in foundFiles.
 * 
 * @author ken
 */
public class FileSegmenter extends Thread {
    /**
     * Standard java.util logger
     */
    private static final Logger logger = Logger.getLogger(FileSegmenter.class
            .getName());

    /**
     * The base directory to use when looking for files to be sent.
     */
    private final File baseDirectory;

    /**
     * The queue to put file segments.
     */
    private final ArrayBlockingQueue<FileFragment> queue;

    /**
     * The set of all located files
     */
    private final Set<File> foundFiles;

    /**
     * The set of previously processed files.
     */
    private final List<File> queuedFiles;

    /**
     * Map storing modification times for files.
     */
    private final Map<String, Long> lastModifiedTimes;

    /**
     * The filter which will accept or reject files based on both the
     * <code>includeFilters</code> and the <code>excludeFilters</code> arguments
     * to the constructor.
     */
    private final FilenameFilter filenameFilter;

    /**
     * A boolean indicating whether this object is current processing files or
     * not.
     */
    private boolean processingFiles;

    /**
     * A boolean indicating whether this object should exit its <code>run</code>
     * method or not.
     */
    private boolean shouldRun;

    /**
     * Set when a new reader has joined.
     */
    private boolean foundReader;

    /**
     * Constructs a <code>FileSegmenter</code>.
     * 
     * @param baseDirectory
     *            The base directory to look for files to send.
     * @param queue
     *            The queue to put generated <code>FileSegment</code> messages.
     */
    public FileSegmenter(File baseDirectory,
            ArrayBlockingQueue<FileFragment> queue) {
        super("FileSegmenter");
        setDaemon(true); // this thread shouldn't hold up a shutdown
        this.baseDirectory = baseDirectory.getAbsoluteFile();
        this.queue = queue;

        foundFiles = new HashSet<File>();
        queuedFiles = new ArrayList<File>();
        lastModifiedTimes = new HashMap<String, Long>();
        shouldRun = true;
        foundReader = false;
        processingFiles = false;
        filenameFilter = new FileSegmenterFilenameFilter(
                Opt.INCLUDE_FILTER.asStringList(","),
                Opt.EXCLUDE_FILTER.asStringList(","));
    }

    /**
     * The logic to process files. This includes finding the files to be sent,
     * packaging the files into piece parts for transmission, and keeping track
     * of what has been sent (for possible resending depending on options).
     */
    public void run() {
        while (shouldRun) {
            while (!processingFiles) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    // this exception is not a problem
                }
            }

            // Check if we received a new reader; if so, refresh queue so it
            // will receive everything. foundReader is cleared only by us, and
            // we don't care if we find multiple readers, so it's OK that this
            // isn't synchronized
            if (foundReader) {
                queuedFiles.clear();
                queuedFiles.addAll(foundFiles);
                foundReader = false;
            }

            File file = getFileToSend();
            // ensure that we got a file and that we can read the file before
            // starting
            if (file != null && file.exists()) {
                // we've got a file to process, open an input stream to ensure
                // that we can read it
                InputStream inputStream = null;
                try {
                    if (!Opt.FAKE_READ.asBoolean()) {
                        inputStream = new BufferedInputStream(
                                new FileInputStream(file));
                    }

                    final int completeSegmentCount = (int) (file.length() / MAX_FILE_SEGMENT_SIZE.VALUE);
                    // need to account for file sizes which need a partial
                    // segment to be sent
                    final int totalSegmentCount = completeSegmentCount
                            + ((file.length()
                                    - (completeSegmentCount * MAX_FILE_SEGMENT_SIZE.VALUE) > 0 ? 1
                                    : 0));

                    // loop to create samples
                    chunkFile(file, inputStream, totalSegmentCount);

                    // if there were no exceptions, add the file to
                    // those that have been successfully processed.
                    lastModifiedTimes.put(file.toString(),
                            new Long(file.lastModified()));
                    logger.fine("processed: " + file);
                } catch (IOException ioException) {
                    // this is sometimes expected; for example, when you are
                    // copying a large file the OS might report that the file
                    // exists but is unavailable for reading (since it's still
                    // being written)

                    logger.warning("IOException, failed to send " + file);
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                }
            } else {
                // nothing to do but sleep for a little while
                try {
                    sleep(500);
                } catch (InterruptedException ie) {
                    // not a problem
                }
            }
        }
    }

    /**
     * Called by Listener threads to indicate that we have a new reader
     */
    public void foundNewReader() {
        foundReader = true;
    }

    /**
     * This method breaks up a file into piece parts for transmission over DDS.
     * 
     * @param file
     *            The file to be read and sent.
     * @param inputStream
     *            The stream from which the file is read.
     * @param totalSegmentCount
     *            The total number piece parts.
     * @throws IOException
     *             If there are any problems reading the file.
     */
    private void chunkFile(File file, InputStream inputStream,
            int totalSegmentCount) throws IOException {
        logger.info("started reading file : " + file);

        final int baseDirStringLength = baseDirectory.toString().length();

        int segmentNumber = 1;
        int totalBytesRead = 0;
        while (totalBytesRead < file.length()) {
            // create a new sample to store the file data
            FileFragment segment = new FileFragment();
            // set the metadata. Samples are processed independently,
            // so each segment needs this information
            segment.fileDescription.name = file.getName();
            segment.fileDescription.path = file.toString().substring(
                    baseDirStringLength,
                    file.toString().length()
                            - segment.fileDescription.name.length() - 1);
            segment.fileDescription.size = file.length();
            segment.fileDescription.lastModifiedDate = file.lastModified();

            // set segment processing information.
            segment.segmentNumber = segmentNumber++;
            segment.totalSegmentCount = totalSegmentCount;

            int currentBytesRead = 0;
            int data = -1;
            do {
                if (Opt.FAKE_READ.asBoolean()) {
                    if (segmentNumber < totalSegmentCount
                            || totalBytesRead < file.length()) {
                        data = segment.segmentNumber;
                    } else {
                        data = -1;
                    }
                } else {
                    data = inputStream.read();
                }
                if (data != -1) {
                    segment.contents.addByte((byte) data);
                }
                currentBytesRead++;
                totalBytesRead++;
            } while (currentBytesRead < MAX_FILE_SEGMENT_SIZE.VALUE
                    && data != -1);

            // once the segment is ready, push it into the queue
            boolean success = false;
            while (!success) {
                try {
                    success = queue.offer(segment, 10, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        logger.info("finished reading file : " + file);
    }

    /**
     * This method will get the next entry from <code>queuedFiles</code>,
     * determined by <code>qidx</code> If we're already at the end of the list,
     * research for files and try again. If no files are found then
     * <code>null</code> is returned.
     * 
     * @return The next file to send, or null if none were found.
     */
    protected File getFileToSend() {
        File f = null;
        if (queuedFiles.isEmpty()) {
            findAllFiles(baseDirectory);
            if (!queuedFiles.isEmpty()) {
                f = (File) queuedFiles.remove(0);
            }
        } else {
            f = (File) queuedFiles.remove(0);
        }

        return f;
    }

    /**
     * Find all files (filtered by the current filter) in the given directory
     * (recursively if that option is chosen). The files that are found are
     * compared with those that have been sent (possibly accounting for the last
     * time the file was modified if that option is chosen). Files are added to
     * <code>foundFiles</code> and again to<code>queuedFiles</code> if needed.
     * 
     * @param dir
     *            The directory to look for files.
     */
    protected void findAllFiles(File dir) {
        File[] children = dir.listFiles(filenameFilter);
        for (int i = 0; i < children.length; i++) {
            if (children[i].isDirectory()
                    && Opt.RECURSE_SUBDIRECTORIES.asBoolean()) {
                findAllFiles(children[i]);
            } else if (children[i].isFile()) {
                File f = children[i].getAbsoluteFile();

                // Got a potential file, have we seen it before?
                if (foundFiles.contains(f)) {
                    /*
                     * Requeue the file if: 1.) We never successfully processed
                     * it (null lastModifiedTime) 2.) The file has been modified
                     * and RESEND_MODIFIED_FILES is true
                     */
                    Object o = lastModifiedTimes.get(f.toString());
                    if (o != null) {
                        long lastModified = ((Long) lastModifiedTimes.get(f
                                .toString())).longValue();
                        if (Opt.RESEND_MODIFIED_FILES.asBoolean()
                                && lastModified != f.lastModified()) {
                            queuedFiles.add(f);
                        }
                    } else {
                        queuedFiles.add(f);
                    }
                } else {
                    // Not found, so add to both found set and send queue.
                    foundFiles.add(f);
                    queuedFiles.add(f);
                }
            }
        }
    }

    /**
     * Determines whether this object should exit its <code>run</code> method or
     * not. Once this method is called with a value of <code>false</code> and
     * the <code>run</code> method is exited it cannot be re-entered. So once
     * exited, a new instance of the object should be created and run.
     * 
     * @param shouldRun
     *            desired run status
     */
    public void setShouldRun(boolean shouldRun) {
        this.shouldRun = shouldRun;
    }

    /**
     * Get whether or not this object is processing files.
     * 
     * @return Returns whether or not this object is processing files.
     */
    public boolean isProcessingFiles() {
        return processingFiles;
    }

    /**
     * Set whether or not this object should be processing files.
     * 
     * @param processingFiles
     *            true to process files, false otherwise.
     */
    public void setProcessingFiles(boolean processingFiles) {
        this.processingFiles = processingFiles;
    };

    /**
     * This class provides a filter for filenames. It takes into account the
     * include and exclude patterns. Note that the filter is applied to the
     * filename, not the path + filename.
     * 
     * @author ken
     */
    private static final class FileSegmenterFilenameFilter implements
            FilenameFilter {

        /**
         * Standard java.util logger
         */
        private static final Logger logger = Logger
                .getLogger(FileSegmenterFilenameFilter.class.getName());

        /**
         * List of patterns specifying files to be included for transfer.
         */
        private final List<Pattern> includePatterns;
        /**
         * List of patterns specifying files to be excluded for transfer.
         */
        private final List<Pattern> excludePatterns;

        /**
         * Constructs a <code>FileSegmenterFilenameFilter</code> with the
         * specified inclusion and exclusion lists.
         * 
         * @param includeFilter
         *            A list of regular expression <code>String</code>s that
         *            include files.
         * @param excludeFilter
         *            A list of regular expression <code>String</code>s that
         *            exclude files.
         */
        FileSegmenterFilenameFilter(List<String> includeFilter,
                List<String> excludeFilter) {
            includePatterns = new ArrayList<Pattern>(includeFilter.size());
            for (int i = 0; i < includeFilter.size(); i++) {
                includePatterns.add(Pattern.compile(includeFilter.get(i)));
            }
            excludePatterns = new ArrayList<Pattern>(excludeFilter.size());
            for (int i = 0; i < excludeFilter.size(); i++) {
                excludePatterns.add(Pattern.compile(excludeFilter.get(i)));
            }
        }

        /**
         * Implementation of the <code>FilenameFilter</code> interface. This
         * method will only accept file names that are not excluded and are
         * included (where a zero-length include filter implies to accept any
         * file name).
         */
        public boolean accept(File dir, String name) {
            boolean match = true;

            // first check if it should be excluded
            boolean exclude = false;
            for (int i = 0; i < excludePatterns.size() && !exclude; i++) {
                Pattern p = (Pattern) excludePatterns.get(i);
                exclude = p.matcher(name).matches();
                logger.finer("exclude " + name + "? : " + exclude);
            }

            if (exclude) {
                // if the file was explicitly excluded, then it is not a match
                match = false;
            } else {
                // if there are no include patterns, accept anything
                match = (includePatterns.size() < 1);
                for (int i = 0; i < includePatterns.size() && !match; i++) {
                    Pattern p = (Pattern) includePatterns.get(i);
                    match = p.matcher(name).matches();
                    logger.finer("match " + name + "? : " + match);
                }
            }
            logger.finer(name + " " + (match ? "matches" : "doesn't match"));
            return match;
        }
    }
}
