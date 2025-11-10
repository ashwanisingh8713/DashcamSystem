package cam.et.tee;

import android.content.Context;
import android.content.res.AssetManager;

import com.github.tony19.logback.android.BuildConfig;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.BasicLogcatConfigurator;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;


/**
 * Utility methods for configuring the logging.
 */
public final class NemoLogConfigurator {

    private static final NemoLog LOG = NemoLog.get("NemoLog");

    /** Provide direct access to the logger context. */
    public static LoggerContext getLoggerContext() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    /**
     * Save the current process PID so it's available for logging.
     * <p>
     * This method is also automatically called by the
     * {@link NemoLogConfigurator#checkCustomConfig(Context)} method.
     * </p>
     */
    public static void savePid() {
        MDC.put("pid", Integer.toString(android.os.Process.myPid()));
    }

    /**
     * Stops the logging, flushes buffers and stops all background threads.
     */
    public static void stopLogging() {
        LOG.ri("stopLogging()");
        getLoggerContext().stop();
    }

    /**
     * Verify that the assets/logback.xml has been loaded. If not, then
     * set the default logcat configuration so there will be at least
     * some logging output.
     */
    static void checkDefaultConfig() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        Iterator<Appender<ILoggingEvent>> it = root.iteratorForAppenders();
        if (!it.hasNext()) {
            // No appenders means that configuration file was not loaded.

            // Set the default log output to the logcat
            BasicLogcatConfigurator.configureDefaultContext();

            // Set the default log level
            root.setLevel(BuildConfig.DEBUG ? Level.TRACE : Level.INFO);

            LOG.re("assets/logback.xml not loaded, using default");
        }
    }

    /**
     * Re-configure logging from the passed configuration file.
     *
     * @param file The configuration file.
     * @return True if the reconfigure was successful, false if not.
     */
    public static boolean reconfigure(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("No such file=" + file);
        }
        LOG.ri("reconfigure() file={}", file);
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.reset();
            configurator.doConfigure(file);
        } catch (Exception e) {
            LOG.re("reconfigure() failed, file=" + file, e);
            return false;
        }
        return true;
    }

    /**
     * Check if the application files directory contains custom "logback.xml"
     * configuration file, and if it does, reconfigure logging using that file.
     *
     * @param context The application context.
     */
    public static void checkCustomConfig(Context context) {
        savePid();

        File customConfig = new File(context.getFilesDir(), "logback.xml");
        if (customConfig.exists()) {
            LOG.ri("checkCustomConfig() found file={}", customConfig);
            reconfigure(customConfig);
        }

        if (BuildConfig.DEBUG) {
            LOG.v("test VERBOSE level");
            LOG.d("test DEBUG level");
            LOG.i("test INFO level");
            LOG.w("test WARN level");
            LOG.e("test ERROR level");
        }
    }

    /**
     * Reconfigure the logging using the logback.xml file in assets.
     *
     * @param context The application context.
     */
    public static void configureFromAssets(Context context) {
        configureFromAssets(context.getAssets());
    }

    /**
     * Reconfigure the logging using the logback.xml file in assets.
     *
     * @param assets The asset manager.
     */
    public static void configureFromAssets(AssetManager assets) {
        InputStream stream;
        try {
            stream = assets.open("logback.xml");
        } catch (IOException e) {
            LOG.rw("configureFromAssets() no asset/logback.xml");
            return;
        }
        LOG.ri("configureFromAssets()");

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            loggerContext.reset();
            configurator.doConfigure(stream);
        } catch (Exception e) {
            LOG.re("configureFromAssets() failed", e);
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Create and add custom appender with DEBUG level threshold.
     *
     * @param output The stream where the formatted log output will be written.
     * @return The created appender, which can be later passed to {@link NemoLogConfigurator#removeCustomAppender(AsyncAppender)}.
     */
    public static AsyncAppender addCustomAppender(OutputStream output) {
        return addCustomAppender(output, Level.DEBUG);
    }

    /**
     * Create and add custom appender for the given log level.
     *
     * @param output The stream where the formatted log output will be written.
     * @param level  The log level threshold.
     * @return The created appender, which can be later passed to {@link NemoLogConfigurator#removeCustomAppender(AsyncAppender)}.
     */
    public static AsyncAppender addCustomAppender(OutputStream output, Level level) {
        return addCustomAppender(output, level, "%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{pid}] [%thread] %-5level %logger{1} %msg%n");
    }

    /**
     * Create and add custom appender for the given log level.
     *
     * @param output  The stream where the formatted log output will be written.
     * @param level   The log level threshold.
     * @param pattern The pattern for formatting the log output.
     * @return The created appender, which can be later passed to {@link NemoLogConfigurator#removeCustomAppender(AsyncAppender)}.
     */
    public static AsyncAppender addCustomAppender(final OutputStream output, final Level level, final String pattern) {
        if (output == null || level == null || pattern == null) {
            throw new IllegalArgumentException();
        }
        final LoggerContext context = getLoggerContext();

        // Pattern encoder for formatting the log output
        final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(pattern);
        //encoder.setImmediateFlush(true);
        encoder.start();

        // Construct the appender, which writes the formatted log output to the
        // passed output stream.
        final OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
        appender.setContext(context);
        appender.setEncoder(encoder);
        appender.setOutputStream(output);
        appender.start();

        // Construct new async appender which forwards the logging events to
        // the actual appender when they get pass the ThresholdFilter.
        final ThresholdFilter filter = new ThresholdFilter();
        filter.setContext(context);
        filter.setLevel(level.levelStr);
        filter.start();

        final AsyncAppender async = new AsyncAppender();
        async.setContext(context);
        async.addFilter(filter);
        async.addAppender(appender);
        async.start();

        // And finally add the async appender to the root logger
        final Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(async);

        return async;
    }

    /**
     * Remove previously created custom appender.
     *
     * @param appender The appender to remove.
     */
    public static void removeCustomAppender(AsyncAppender appender) {
        if (appender != null) {
            final LoggerContext context = getLoggerContext();
            final Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
            if (root.detachAppender(appender)) {
                appender.stop();
                appender.detachAndStopAllAppenders();
            }
        }
    }

    /**
     * Flush all file appenders.
     */
    public static void flushFileAppenders() {
        final LoggerContext context = getLoggerContext();
        final Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        final Iterator<Appender<ILoggingEvent>> it = root.iteratorForAppenders();
        while (it.hasNext()) {
            final Appender<ILoggingEvent> appender = it.next();
            if (appender instanceof FileAppender) {
                flushFileAppender((FileAppender<ILoggingEvent>) appender);
            } else if (appender instanceof AsyncAppender) {
                // AsyncAppenders can have file appenders as reference-appenders
                final Iterator<Appender<ILoggingEvent>> itAsync = ((AsyncAppender) appender).iteratorForAppenders();
                while (itAsync.hasNext()) {
                    final Appender<ILoggingEvent> fileAppender = itAsync.next();
                    if (fileAppender instanceof FileAppender) {
                        flushFileAppender((FileAppender<ILoggingEvent>) fileAppender);
                    }
                }
            }
        }
    }

    private static void flushFileAppender(final FileAppender<ILoggingEvent> pAppender) {
        final OutputStream o = pAppender.getOutputStream();
        if (o != null) {
            try {
                o.flush();
            } catch (Exception e) {
            }
        }
    }
}
