package cam.et.tee;

import org.slf4j.Logger;
/**
 * A hook to be used with NemoLog
 * <p>
 * Override this implementation and set it to NemoLog to receive debug messages
 */
public class NemoLogWriter {

    /**
     * Write debug message
     */
    public void write(Logger logger, NemoLog.Level level, String message) {

    }

    /**
     * Write debug message
     */
    public void write(Logger logger, NemoLog.Level level, String message, Throwable t) {

    }

    /**
     * Return true if messages are required. This information is used to
     * determine whether log message needs to be formated or not.
     */
    public boolean isWriting() {
        return false;
    }

    /**
     * Return true if messages .r?-type logs are required. This information is used to
     * determine whether log message need to be written to prevent duplication of release logs in NATA
     * verbose logging.  Cannot prevent formating of the logs with this condition as
     *  as .r?-type logs are written anyway to logger.
     */
    public boolean isWritingReleaseLogs() {
        return true;
    }

}
