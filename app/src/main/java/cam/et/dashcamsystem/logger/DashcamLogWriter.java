package cam.et.dashcamsystem.logger;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cam.et.dashcamsystem.device.FilePathManager;

/**
 * A hook to be used with NemoLog
 * <p>
 * Override this implementation and set it to NemoLog to receive debug messages
 */
public class DashcamLogWriter {

    // Filename includes a timestamp generated once per class load (i.e. once per app execution).
    private static final String FILENAME;

    private static final Object LOCK = new Object();

    static {
        // Generate a filename-safe timestamp for this execution (no spaces/colons).
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        FILENAME = "dashcamlog_" + ts + ".txt";
    }

    private static String timeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
    }

    /**
     * Write debug message
     */
    public void write(Logger logger, DashcamLog.Level level, String message) {
        writeInternal(logger, level, message, null);
    }

    /**
     * Write debug message
     */
    public void write(Logger logger, DashcamLog.Level level, String message, Throwable t) {
        writeInternal(logger, level, message, t);
    }

    private void writeInternal(Logger logger, DashcamLog.Level level, String message, Throwable t) {
        String tag = (logger != null && logger.getName() != null) ? logger.getName() : "NemoLog";
        String lvl = (level != null) ? level.name() : "UNKNOWN";
        String msg = (message != null) ? message : "";

        String line = String.format(Locale.getDefault(), "%s [%s] %s - %s\n", timeStamp(), lvl, tag, msg);

        // If there is a throwable, capture its stacktrace
        if (t != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
            line += sw.toString();
        }

        // Use the application-managed logs directory provided by FilePathManager.
        try {
            File dir = FilePathManager.INSTANCE.getLogsDir();
            // FilePathManager guarantees a directory; if it somehow returns null, bail out.
            if (dir == null) return;
            synchronized (LOCK) {
                if (!dir.exists()) {
                    // Try to create directory; if creation fails, give up silently.
                    if (!dir.mkdirs() && !dir.exists()) {
                        return;
                    }
                }
                File out = new File(dir, FILENAME);
                try (FileOutputStream fos = new FileOutputStream(out, true)) {
                    fos.write(line.getBytes(StandardCharsets.UTF_8));
                    fos.flush();
                } catch (Exception ignored) {
                    // unable to write; give up
                }
            }
        } catch (Throwable ignored) {
            // FilePathManager not available or failed; do not attempt legacy candidate paths.
        }

    }

    /**
     * Return true if messages are required. This information is used to
     * determine whether log message needs to be formated or not.
     */
    public boolean isWriting() {
        // Return true so NemoLog will forward messages to this writer and formatting occurs.
        return true;
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
