package cam.et.tee;

import com.github.tony19.logback.android.BuildConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.lang.reflect.Method;


/**
 * Replacement for the android logger.
 * <p>
 * The underlying implementation uses the logback-android project, which can be used for advanced logging features, such as file logging etc. Refer to
 * the
 * <a href="https://github.com/tony19/logback-android">https://github.com/tony19/logback-android</a> for details.
 * </p>
 * <p>
 * The default logback configuration is loaded from assets/logback.xml file, and all project specific configuration must be done in that file.
 * </p>
 * NemoLog usage in code:
 *
 * <pre>
 * <code>
 * class MyClass {
 *     private static final NemoLog LOG = NemoLog.get(MyClass.class);
 *
 *     void foo() {
 *          int x = 391;
 *          Object y = null;
 *          LOG.i("This message is logged only in in debug builds, x={} y={}", x, y);
 *          LOG.rd("This message is logged in release builds too, x={} y={}", x, y);
 *     }
 * }
 *
 *
 * <pre>
 * @see <a href="https://github.com/tony19/logback-android">https://github.com/tony19/logback-android</a>
 */
public final class NemoLog {

    /* Example usage:
    static class MyClass {
        private static final NemoLog LOG = NemoLog.get(MyClass.class);

        void foo() {
            int x = 391;
            Object y = null;
            LOG.i("This message is logged only in in debug builds, x={} y={}", x, y);
            LOG.rd("This message is logged in release builds too, x={} y={}", x, y);
        }
    }*/

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
    private static NemoLogWriter sLogWriter = new NemoLogWriter();


    /**
     * If true, fatal message will call assert
     */
    private static boolean sAssertOnFatal = false;

    /*
     * Verify that the default configuration has been loaded.
     */
    static {
        NemoLogConfigurator.checkDefaultConfig();
    }

    /**
     * Test if debug build logging is enabled.
     *
     * @return True if this is debug build, false if not.
     */
    public static boolean isDebug() {
        return BuildConfig.DEBUG;
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
    public static void setLogWriter(NemoLogWriter writer) {
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
    public static NemoLog get(Class<?> clazz) {
        return new NemoLog(clazz);
    }

    /**
     * Returns a new NemoLog instance for the given name.
     *
     * @param name The logger name.
     * @return The constructed NemoLog instance.
     */
    public static NemoLog get(String name) {
        return new NemoLog(name);
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
    private NemoLog(Class<?> clazz) {
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
    private NemoLog(String name) {
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
     * Log message according to the specified format and arguments in debug builds only.
     *
     * @param level  log level
     * @param format The format string.
     * @param arg    The argument.
     */
    public void l(Level level, String format, Object arg) {
        switch (level) {
            case DEBUG:
                d(format, arg);
                break;
            case ERROR:
                e(format, arg);
                break;
            case INFO:
                i(format, arg);
                break;
            case VERBOSE:
                v(format, arg);
                break;
            case WARNING:
                w(format, arg);
                break;
            case FATAL:
                f(format, arg);
                break;
        }
    }

    /**
     * Log message according to the specified format and arguments in debug builds only.
     *
     * @param level     log level
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void l(Level level, String format, Object... arguments) {
        switch (level) {
            case DEBUG:
                d(format, arguments);
                break;
            case ERROR:
                e(format, arguments);
                break;
            case INFO:
                i(format, arguments);
                break;
            case VERBOSE:
                v(format, arguments);
                break;
            case WARNING:
                w(format, arguments);
                break;
            case FATAL:
                f(format, arguments);
                break;
        }
    }

    /**
     * Log message according to the specified format and arguments in debug builds only.
     *
     * @param level log level
     * @param arg1  The first argument.
     * @param arg2  The second argument.
     */
    public void l(Level level, String format, Object arg1, Object arg2) {
        switch (level) {
            case DEBUG:
                d(format, arg1, arg2);
                break;
            case ERROR:
                e(format, arg1, arg2);
                break;
            case INFO:
                i(format, arg1, arg2);
                break;
            case VERBOSE:
                v(format, arg1, arg2);
                break;
            case WARNING:
                w(format, arg1, arg2);
                break;
            case FATAL:
                f(format, arg1, arg2);
                break;
        }
    }

    /**
     * Log message and exception in debug builds.
     *
     * @param level log level
     * @param msg   The message you would like logged.
     * @param tr    An exception to log
     */
    public void l(Level level, String msg, Throwable tr) {
        switch (level) {
            case DEBUG:
                d(msg, tr);
                break;
            case ERROR:
                e(msg, tr);
                break;
            case INFO:
                i(msg, tr);
                break;
            case VERBOSE:
                v(msg, tr);
                break;
            case WARNING:
                w(msg, tr);
                break;
            case FATAL:
                f(msg, tr);
                break;
        }
    }

    /**
     * Log message - For writing log messages from native side
     *
     * @param level log level ordinal
     * @param msg   The message you would like logged.
     */
    public void l(int level, String msg) {
        Level l = Level.values()[level];
        l(l, msg);
    }


    /**
     * Log VERBOSE message in debug builds only.
     *
     * @param msg The message you would like logged.
     */
    public void v(String msg) {
        if (mLogger.isTraceEnabled()) {

            if (BuildConfig.DEBUG) {
                mLogger.trace(msg);
            }
        }
    }

    /**
     * Log VERBOSE message according to the specified format and arguments in debug builds only.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void v(String format, Object arg) {
        if (mLogger.isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            String msg = ft.getMessage();

            if (BuildConfig.DEBUG) {
                mLogger.trace(msg);
            }
        }
    }

    /**
     * Log VERBOSE message according to the specified format and arguments in debug builds only.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void v(String format, Object arg1, Object arg2) {
        if (mLogger.isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            String msg = ft.getMessage();

            if (BuildConfig.DEBUG) {
                mLogger.trace(msg);
            }
        }
    }

    /**
     * Log VERBOSE message according to the specified format and arguments in debug builds only.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void v(String format, Object... arguments) {
        if (mLogger.isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            String msg = ft.getMessage();

            if (BuildConfig.DEBUG) {
                mLogger.trace(msg);
            }
        }
    }

    /**
     * Log VERBOSE message and exception in debug builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void v(String msg, Throwable tr) {
        if (mLogger.isTraceEnabled()) {

            if (BuildConfig.DEBUG) {
                mLogger.trace(msg, tr);
            }
        }
    }

    /**
     * Log VERBOSE message in debug and release builds.
     *
     * @param msg The message you would like logged.
     */
    public void rv(String msg) {
        mLogger.trace(msg);
    }

    /**
     * Log VERBOSE message according to the specified format and arguments in debug and release builds.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void rv(String format, Object arg) {
        FormattingTuple ft = MessageFormatter.format(format, arg);
        String msg = ft.getMessage();
        mLogger.trace(msg);
    }

    /**
     * Log VERBOSE message according to the specified format and arguments in debug and release builds.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void rv(String format, Object arg1, Object arg2) {
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        String msg = ft.getMessage();
        mLogger.trace(msg);
    }

    /**
     * Log VERBOSE message according to the specified format and arguments in debug and release builds.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void rv(String format, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
        String msg = ft.getMessage();
        mLogger.trace(msg);
    }

    /**
     * Log VERBOSE message and exception in debug and release builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void rv(String msg, Throwable tr) {
        mLogger.trace(msg, tr);
    }

    /**
     * Log DEBUG message in debug builds only.
     *
     * @param msg The message you would like logged.
     */
    public void d(String msg) {
        if (mLogger.isDebugEnabled() || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg);

            if (BuildConfig.DEBUG) {
                mLogger.debug(msg);
            }
        }
    }

    /**
     * Log DEBUG message according to the specified format and arguments in debug builds only.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void d(String format, Object arg) {
        if (mLogger.isDebugEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.DEBUG, msg);

            if (BuildConfig.DEBUG) {
                mLogger.debug(msg);
            }
        }
    }

    /**
     * Log DEBUG message according to the specified format and arguments in debug builds only.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void d(String format, Object arg1, Object arg2) {
        if (mLogger.isDebugEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.DEBUG, msg);

            if (BuildConfig.DEBUG) {
                mLogger.debug(msg);
            }
        }
    }

    /**
     * Log DEBUG message according to the specified format and arguments in debug builds only.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void d(String format, Object... arguments) {
        if (mLogger.isDebugEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.DEBUG, msg);

            if (BuildConfig.DEBUG) {
                mLogger.debug(msg);
            }
        }
    }

    /**
     * Log DEBUG message and exception in debug builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void d(String msg, Throwable tr) {
        if (mLogger.isDebugEnabled() || sLogWriter.isWriting()) {

            sLogWriter.write(mLogger, Level.DEBUG, msg, tr);

            if (BuildConfig.DEBUG) {
                mLogger.debug(msg, tr);
            }
        }
    }

    /**
     * Log DEBUG message in debug and release builds.
     *
     * @param msg The message you would like logged.
     */
    public void rd(String msg) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg);
        }
        mLogger.debug(msg);
    }

    /**
     * Log DEBUG message according to the specified format and arguments in debug and release builds.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void rd(String format, Object arg) {
        FormattingTuple ft = MessageFormatter.format(format, arg);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg);
        }
        mLogger.debug(msg);
    }

    /**
     * Log DEBUG message according to the specified format and arguments in debug and release builds.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void rd(String format, Object arg1, Object arg2) {
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg);
        }
        mLogger.debug(msg);
    }

    /**
     * Log DEBUG message according to the specified format and arguments in debug and release builds.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void rd(String format, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg);
        }
        mLogger.debug(msg);
    }

    /**
     * Log DEBUG message and exception in debug and release builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void rd(String msg, Throwable tr) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg, tr);
        }
        mLogger.debug(msg, tr);
    }

    /**
     * Log INFO message in debug builds only.
     *
     * @param msg The message you would like logged.
     */
    public void i(String msg) {
        if (mLogger.isInfoEnabled() || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.INFO, msg);

            if (BuildConfig.DEBUG) {
                mLogger.info(msg);
            }
        }
    }

    /**
     * Log INFO message according to the specified format and arguments in debug builds only.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void i(String format, Object arg) {
        if (mLogger.isInfoEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.INFO, msg);

            if (BuildConfig.DEBUG) {
                mLogger.info(msg);
            }
        }
    }

    /**
     * Log INFO message according to the specified format and arguments in debug builds only.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void i(String format, Object arg1, Object arg2) {
        if (mLogger.isInfoEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.INFO, msg);

            if (BuildConfig.DEBUG) {
                mLogger.info(msg);
            }
        }
    }

    /**
     * Log INFO message according to the specified format and arguments in debug builds only.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void i(String format, Object... arguments) {
        if (mLogger.isInfoEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.INFO, msg);

            if (BuildConfig.DEBUG) {
                mLogger.info(msg);
            }
        }
    }

    /**
     * Log INFO message and exception in debug builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void i(String msg, Throwable tr) {
        if (mLogger.isInfoEnabled() || sLogWriter.isWriting()) {

            sLogWriter.write(mLogger, Level.INFO, msg, tr);

            if (BuildConfig.DEBUG) {
                mLogger.info(msg, tr);
            }
        }
    }

    /**
     * Log INFO message in debug and release builds.
     *
     * @param msg The message you would like logged.
     */
    public void ri(String msg) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.INFO, msg);
        }
        mLogger.info(msg);
    }

    /**
     * Log INFO message according to the specified format and arguments in debug and release builds.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void ri(String format, Object arg) {
        FormattingTuple ft = MessageFormatter.format(format, arg);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.INFO, msg);
        }
        mLogger.info(msg);
    }

    /**
     * Log INFO message according to the specified format and arguments in debug and release builds.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void ri(String format, Object arg1, Object arg2) {
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.INFO, msg);
        }
        mLogger.info(msg);
    }

    /**
     * Log INFO message according to the specified format and arguments in debug and release builds.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void ri(String format, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.INFO, msg);
        }
        mLogger.info(msg);
    }

    /**
     * Log INFO message and exception in debug and release builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void ri(String msg, Throwable tr) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.INFO, msg, tr);
        }
        mLogger.info(msg, tr);
    }

    /**
     * Log WARN message in debug builds only.
     *
     * @param msg The message you would like logged.
     */
    public void w(String msg) {
        if (mLogger.isWarnEnabled() || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.WARNING, msg);

            if (BuildConfig.DEBUG) {
                mLogger.warn(msg);
            }
        }
    }

    /**
     * Log WARN message according to the specified format and arguments in debug builds only.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void w(String format, Object arg) {
        if (mLogger.isWarnEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.WARNING, msg);

            if (BuildConfig.DEBUG) {
                mLogger.warn(msg);
            }
        }
    }

    /**
     * Log WARN message according to the specified format and arguments in debug builds only.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void w(String format, Object arg1, Object arg2) {
        if (mLogger.isWarnEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.WARNING, msg);

            if (BuildConfig.DEBUG) {
                mLogger.warn(msg);
            }
        }
    }

    /**
     * Log WARN message according to the specified format and arguments in debug builds only.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void w(String format, Object... arguments) {
        if (mLogger.isWarnEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.WARNING, msg);

            if (BuildConfig.DEBUG) {
                mLogger.warn(msg);
            }
        }
    }

    /**
     * Log WARN message and exception in debug builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void w(String msg, Throwable tr) {
        if (mLogger.isWarnEnabled() || sLogWriter.isWriting()) {

            sLogWriter.write(mLogger, Level.WARNING, msg, tr);

            if (BuildConfig.DEBUG) {
                mLogger.warn(msg, tr);
            }
        }
    }

    /**
     * Log WARN message in debug and release builds.
     *
     * @param msg The message you would like logged.
     */
    public void rw(String msg) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.WARNING, msg);
        }
        mLogger.warn(msg);
    }

    /**
     * Log WARN message according to the specified format and arguments in debug and release builds.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void rw(String format, Object arg) {
        FormattingTuple ft = MessageFormatter.format(format, arg);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.WARNING, msg);
        }
        mLogger.warn(msg);
    }

    /**
     * Log WARN message according to the specified format and arguments in debug and release builds.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void rw(String format, Object arg1, Object arg2) {
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.WARNING, msg);
        }
        mLogger.warn(msg);
    }

    /**
     * Log WARN message according to the specified format and arguments in debug and release builds.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void rw(String format, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.WARNING, msg);
        }
        mLogger.warn(msg);
    }

    /**
     * Log WARN message and exception in debug and release builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void rw(String msg, Throwable tr) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.WARNING, msg, tr);
        }
        mLogger.warn(msg, tr);
    }

    /**
     * Log ERROR message in debug builds only.
     *
     * @param msg The message you would like logged.
     */
    public void e(String msg) {
        if (mLogger.isErrorEnabled() || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.ERROR, msg);

            if (BuildConfig.DEBUG) {
                mLogger.error(msg);
            }
        }
    }

    /**
     * Log ERROR message according to the specified format and arguments in debug builds only.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void e(String format, Object arg) {
        if (mLogger.isErrorEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.ERROR, msg);

            if (BuildConfig.DEBUG) {
                mLogger.error(msg);
            }
        }
    }

    /**
     * Log ERROR message according to the specified format and arguments in debug builds only.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void e(String format, Object arg1, Object arg2) {
        if (mLogger.isErrorEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.ERROR, msg);

            if (BuildConfig.DEBUG) {
                mLogger.error(msg);
            }
        }
    }

    /**
     * Log ERROR message according to the specified format and arguments in debug builds only.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void e(String format, Object... arguments) {
        if (mLogger.isErrorEnabled() || sLogWriter.isWriting()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            String msg = ft.getMessage();
            sLogWriter.write(mLogger, Level.ERROR, msg);

            if (BuildConfig.DEBUG) {
                mLogger.error(msg);
            }
        }
    }

    /**
     * Log ERROR message and exception in debug builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void e(String msg, Throwable tr) {
        if (mLogger.isErrorEnabled() || sLogWriter.isWriting()) {

            sLogWriter.write(mLogger, Level.ERROR, msg, tr);

            if (BuildConfig.DEBUG) {
                mLogger.error(msg, tr);
            }
        }
    }

    /**
     * Log ERROR message in debug and release builds.
     *
     * @param msg The message you would like logged.
     */
    public void re(String msg) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.ERROR, msg);
        }
        mLogger.error(msg);
    }

    /**
     * Log ERROR message according to the specified format and arguments in debug and release builds.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void re(String format, Object arg) {
        FormattingTuple ft = MessageFormatter.format(format, arg);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.ERROR, msg);
        }
        mLogger.error(msg);
    }

    /**
     * Log ERROR message according to the specified format and arguments in debug and release builds.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void re(String format, Object arg1, Object arg2) {
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.ERROR, msg);
        }
        mLogger.error(msg);
    }

    /**
     * Log ERROR message according to the specified format and arguments in debug and release builds.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void re(String format, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
        String msg = ft.getMessage();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.ERROR, msg);
        }
        mLogger.error(msg);
    }

    /**
     * Log ERROR message and exception in debug and release builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void re(String msg, Throwable tr) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.ERROR, msg, tr);
        }
        mLogger.error(msg, tr);
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

            if (BuildConfig.DEBUG) {
                mLogger.error(msg);
            }
            if (sAssertOnFatal) {

            }
        }


    }

    /**
     * Log FATAL message according to the specified format and arguments in debug builds only.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void f(String format, Object arg) {
        if (mLogger.isErrorEnabled() || sLogWriter.isWriting() || sAssertOnFatal) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            String msg = ft.getMessage() + getStackTraceWhenNotAsserting();

            sLogWriter.write(mLogger, Level.FATAL, msg);

            if (BuildConfig.DEBUG) {
                mLogger.error(msg);
            }

        }
    }

    /**
     * Log FATAL message according to the specified format and arguments in debug builds only.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void f(String format, Object arg1, Object arg2) {
        if (mLogger.isErrorEnabled() || sLogWriter.isWriting() || sAssertOnFatal) {
            FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
            String msg = ft.getMessage() + getStackTraceWhenNotAsserting();
            sLogWriter.write(mLogger, Level.FATAL, msg);

            if (BuildConfig.DEBUG) {
                mLogger.error(msg);
            }

        }
    }

    /**
     * Log FATAL message according to the specified format and arguments in debug builds only.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void f(String format, Object... arguments) {
        if (mLogger.isErrorEnabled() || sLogWriter.isWriting() || sAssertOnFatal) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            String msg = ft.getMessage() + getStackTraceWhenNotAsserting();
            sLogWriter.write(mLogger, Level.FATAL, msg);

            if (BuildConfig.DEBUG) {
                mLogger.error(msg);
            }
            if (sAssertOnFatal) {

            }
        }
    }

    /**
     * Log FATAL message and exception in debug builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void f(String msg, Throwable tr) {
        if (mLogger.isErrorEnabled() || sLogWriter.isWriting()) {

            sLogWriter.write(mLogger, Level.FATAL, msg, tr);

            if (BuildConfig.DEBUG) {
                mLogger.error(msg, tr);
            }
            if (sAssertOnFatal) {

            }
        }
    }

    /**
     * Log FATAL message in debug and release builds.
     *
     * @param msg The message you would like logged.
     */
    public void rf(String msg) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.FATAL, msg + getStackTraceWhenNotAsserting());
        }
        mLogger.error(msg);
        if (sAssertOnFatal) {

        }
    }

    /**
     * Log ERROR message according to the specified format and arguments in debug and release builds.
     *
     * @param format The format string.
     * @param arg    The argument.
     */
    public void rf(String format, Object arg) {
        FormattingTuple ft = MessageFormatter.format(format, arg);
        String msg = ft.getMessage() + getStackTraceWhenNotAsserting();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.FATAL, msg);
        }
        mLogger.error(msg);

    }

    /**
     * Log FATAL message according to the specified format and arguments in debug and release builds.
     *
     * @param arg1 The first argument.
     * @param arg2 The second argument.
     */
    public void rf(String format, Object arg1, Object arg2) {
        FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
        String msg = ft.getMessage() + getStackTraceWhenNotAsserting();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.FATAL, msg);
        }
        mLogger.error(msg);

    }

    /**
     * Log FATAL message according to the specified format and arguments in debug and release builds.
     *
     * @param format    The format string.
     * @param arguments A list of 3 or more arguments.
     */
    public void rf(String format, Object... arguments) {
        FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
        String msg = ft.getMessage() + getStackTraceWhenNotAsserting();
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.FATAL, msg);
        }
        mLogger.error(msg);

    }

    /**
     * Log FATAL message and exception in debug and release builds.
     *
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public void rf(String msg, Throwable tr) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.FATAL, msg, tr);
        }
        mLogger.error(msg, tr);

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

    public static String methodNameGet() {
        Method[] mehtods = NemoLog.class.getDeclaredMethods();
        for (Method m : mehtods) {
            Class<?> rType = m.getReturnType();
            Class<?>[] pTypes = m.getParameterTypes();
            Class<?> onlyParam = pTypes.length == 1 ? pTypes[0] : null;
            if (rType == NemoLog.class && pTypes.length == 1 && pTypes[0] == String.class) {
                return m.getName();
            }
        }
        return null;
    }

    public static String methodNameL() {
        Method[] mehtods = NemoLog.class.getDeclaredMethods();

        for (Method m : mehtods) {
            String name = m.getName();
            Class<?> rType = m.getReturnType();
            Class<?>[] pTypes = m.getParameterTypes();

            if (rType == void.class && pTypes.length == 2 && pTypes[0] == int.class && pTypes[1] == String.class) {
                return name;
            }
        }
        return null;
    }

}
