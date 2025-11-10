package cam.et.dashcamlog;

import android.os.Environment;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A hook to be used with NemoLog
 * <p>
 * Override this implementation and set it to NemoLog to receive debug messages
 */
public class DashcamLogWriter {

    // Candidate directories to try (in order). These are legacy external locations.
    private static final String[] PATH_DIRS;
    // Filename now includes a timestamp generated once per class load (i.e. once per app execution).
    private static final String FILENAME;

    private static final Object LOCK = new Object();

    static {
        // Generate a filename-safe timestamp for this execution (no spaces/colons).
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        FILENAME = "dashcamlog_" + ts + ".txt";

        String ext = null;
        try {
            File external = Environment.getExternalStorageDirectory();
            if (external != null) {
                ext = external.getAbsolutePath();
            }
        } catch (Exception ignored) {
        }
        if (ext == null) {
            PATH_DIRS = new String[] {
                    "/mnt/sdcard/DashcamSystem/logs",
                    "/sdcard/DashcamSystem/logs",
                    "/storage/emulated/0/DashcamSystem/logs"
            };
        } else {
            PATH_DIRS = new String[] {
                    ext + "/DashcamSystem/logs",
                    "/mnt/sdcard/DashcamSystem/logs",
                    "/sdcard/DashcamSystem/logs"
            };
        }
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

        // Try each candidate directory and write the log file into the first writable one.
        for (String dirPath : PATH_DIRS) {
            try {
                synchronized (LOCK) {
                    File dir = new File(dirPath);
                    if (!dir.exists()) {
                        boolean created = dir.mkdirs();
                        // created may be false if directory couldn't be created; we'll try writing anyway and catch the exception
                    }
                    File out = new File(dir, FILENAME);
                    try (FileOutputStream fos = new FileOutputStream(out, true)) {
                        fos.write(line.getBytes(StandardCharsets.UTF_8));
                        fos.flush();
                        return; // success
                    } catch (Exception ignored) {
                        // try next directory
                    }
                }
            } catch (Exception ignored) {
                // try next directory
            }
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
