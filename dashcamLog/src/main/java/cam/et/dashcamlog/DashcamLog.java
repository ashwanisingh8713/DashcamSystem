package cam.et.dashcamlog;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DashcamLog {

    public enum Level {
        VERBOSE('V', 4),
        DEBUG('D', 4),
        WARNING('W', 3),
        INFO('I', 3),
        ERROR('E', 2),
        FATAL('F', 1);

        public final char levelChar;
        public final int nblType;

        Level(char c, int nblType) {
            this.levelChar = c;
            this.nblType = nblType;
        }

    }

    /**
     * A hook to reroute logs to debug log file. Empty implementation as default.
     */
    private static DashcamLogWriter sLogWriter = new DashcamLogWriter();


    /**
     * If true, fatal message will call assert
     */
    private static boolean sAssertOnFatal = false;

    // Optional runtime override: some projects import the wrong BuildConfig (for a library)
    // which causes BuildConfig.DEBUG to be false even in app debug builds.
    // Call DashcamLog.setDebug(BuildConfig.DEBUG) from your Application.onCreate() so the
    // application-provided BuildConfig.DEBUG value is used by this library.
    private static Boolean sDebugOverride = null;


    /**
     * Test if debug build logging is enabled.
     *
     * @return True if this is debug build, false if not.
     */
    public static boolean isDebug() {
        if (sDebugOverride != null) {
            return sDebugOverride;
        }
        // Default to false if no override is provided. The library cannot reliably
        // reference the application's BuildConfig at compile time, so callers
        // should set the override from the application when needed.
        return false;
    }

    /**
     * Allow the application to explicitly override debug detection at runtime.
     * Useful when BuildConfig.DEBUG is unavailable or inaccurate for this library.
     * Recommended usage (in your app module's Application.onCreate):
     *     DashcamLog.setDebug(BuildConfig.DEBUG);
     */
    public static void setDebug(boolean debug) {
        sDebugOverride = debug;
    }

    /**
     * Helper that inspects the application's debuggable flag. This lets the
     * library detect debug-mode at runtime without depending on the app's
     * BuildConfig. Call this from your Application.onCreate():
     *     DashcamLog.setDebugFromContext(this);
     */
    public static void setDebugFromContext(Context context) {
        if (context == null) return;
        try {
            boolean debuggable = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            sDebugOverride = debuggable;
        } catch (Exception ignored) {
            // Leave override unchanged on failure
        }
    }

    /**
     * Are debuglogs required. Return true if debug messages are being written.
     *
     * @return True if debug messages are required.
     */
    public static boolean isConnectedToFileWriter() {
        return sLogWriter.isWriting();
    }

    /**
     * Set Hook for acquiring debug messages
     */
    public static void setLogWriter(DashcamLogWriter writer) {
        sLogWriter = writer;
    }

    /**
     * If set to true, fatal message will cause assert
     */
    public static void setAssertOnFatal(boolean assertOnFatal) {
        sAssertOnFatal = assertOnFatal;
    }

    /**
     * Returns a new NemoLog instance for the given class.
     *
     * @param clazz The class.
     * @return The constructed NemoLog instance.
     */
    public static DashcamLog get(Class<?> clazz) {
        return new DashcamLog(clazz);
    }

    /**
     * Returns a new NemoLog instance for the given name.
     *
     * @param name The logger name.
     * @return The constructed NemoLog instance.
     */
    public static DashcamLog get(String name) {
        return new DashcamLog(name);
    }

    /**
     * The actual logger.
     */
    private final Logger mLogger;

    /**
     * Construct logger for the given class.
     *
     * @param clazz The class.
     */
    private DashcamLog(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException();
        }
        mLogger = LoggerFactory.getLogger(clazz);
    }

    /**
     * Construct logger with the given name.
     *
     * @param name The logger name.
     */
    private DashcamLog(String name) {
        if (name == null) {
            throw new IllegalArgumentException();
        }
        mLogger = LoggerFactory.getLogger(name);
    }

    /**
     * Access the underlying logger instance directly.
     *
     * @return The logger instance.
     */
    public Logger get() {
        return mLogger;
    }

    /**
     * Log VERBOSE message in debug builds only.
     *
     * @param msg The message you would like logged.
     */
    public void v(String msg) {
        if (mLogger.isTraceEnabled()) {

            if (isDebug()) {
                mLogger.trace(msg);
            }
        }
    }

    /**
     * Log DEBUG message in debug builds only.
     *
     * @param msg The message you would like logged.
     */
    public void d(String msg) {
        if (mLogger.isDebugEnabled() || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg);

            if (isDebug()) {
                mLogger.debug(msg);
            }
        }
    }

    /**
     * Log INFO message in debug builds only.
     *
     * @param msg The message you would like logged.
     */
    public void i(String msg) {
        if (mLogger.isInfoEnabled() || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.INFO, msg);

            if (isDebug()) {
                mLogger.info(msg);
            }
        }
    }

    /**
     * Log WARN message in debug builds only.
     *
     * @param msg The message you would like logged.
     */
    public void w(String msg) {
        if (mLogger.isWarnEnabled() || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.WARNING, msg);

            if (isDebug()) {
                mLogger.warn(msg);
            }
        }
    }

    /**
     * Log ERROR message in debug builds only.
     *
     * @param msg The message you would like logged.
     */
    public void e(String msg) {
        if (mLogger.isErrorEnabled() || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.ERROR, msg);

            if (isDebug()) {
                mLogger.error(msg);
            }
        }
    }


    /**
     * Log FATAL message in debug builds only.
     *
     * @param msg The message you would like logged.
     */
    public void f(String msg) {
        if (mLogger.isErrorEnabled() || sLogWriter.isWriting() || sAssertOnFatal) {
            msg += getStackTraceWhenNotAsserting();
            sLogWriter.write(mLogger, Level.FATAL, msg);

            if (isDebug()) {
                mLogger.error(msg);
            }
            if (sAssertOnFatal) {

            }
        }
    }


    /**
     * Return stack trace if assert is not enabled.
     * <p>
     * If assert is enabled stack trace is acquired from uncaughtException.
     *
     * @return Stack trace if sAssertOnFatal is false. Otherwise empty string.
     */
    private static String getStackTraceWhenNotAsserting() {
        if (sAssertOnFatal) {
            return "";
        }
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StringBuilder b = new StringBuilder();
        for (StackTraceElement element : stack) {
            b.append("\n").append(element.toString());
        }
        return b.toString();
    }

}
