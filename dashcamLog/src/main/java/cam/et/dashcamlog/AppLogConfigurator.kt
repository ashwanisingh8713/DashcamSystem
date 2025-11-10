package cam.et.dashcamlog

import android.content.Context
import android.content.res.AssetManager
import android.os.Process
import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.android.BasicLogcatConfigurator
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.util.ContextUtil
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


/**
 * Utility methods for configuring the logging (Kotlin port of NemoLogConfigurator.java).
 */
object AppLogConfigurator {

    private val LOG = AppLog.get("AppLog")

    /** Provide direct access to the logger context. */
    @JvmStatic
    fun getLoggerContext(): LoggerContext {
        return LoggerFactory.getILoggerFactory() as LoggerContext
    }

    /**
     * Save the current process PID so it's available for logging.
     * This method is also automatically called by checkCustomConfig.
     */
    @JvmStatic
    fun savePid() {
        MDC.put("pid", Process.myPid().toString())
    }

    /** Stops the logging, flushes buffers and stops all background threads. */
    @JvmStatic
    fun stopLogging() {
        LOG.ri("stopLogging()")
        getLoggerContext().stop()
    }

    /**
     * Verify that the assets/logback.xml has been loaded. If not, then
     * set the default logcat configuration so there will be at least
     * some logging output.
     */
    @JvmStatic
    fun checkDefaultConfig() {
        val context = getLoggerContext()
        val root = context.getLogger(Logger.ROOT_LOGGER_NAME)
        val it = root.iteratorForAppenders()
        if (!it.hasNext()) {
            // No appenders means that configuration file was not loaded.

            // Set the default log output to the logcat
            BasicLogcatConfigurator.configureDefaultContext()

            // Try to set default level. If BuildConfig.DEBUG exists in this module it will be used;
            // otherwise default to INFO for release-like builds.
            try {
                val debugField = Class.forName("cam.et.dashcamlog.BuildConfig").getField("DEBUG")
                val debug = debugField.getBoolean(null)
                root.level = if (debug) Level.TRACE else Level.INFO
            } catch (t: Throwable) {
                // Fallback if BuildConfig is not available
                root.level = Level.INFO
            }

            LOG.re("assets/logback.xml not loaded, using default")
        }
    }

    /**
     * Re-configure logging from the passed configuration file.
     * @return True if the reconfigure was successful, false if not.
     */
    @JvmStatic
    fun reconfigure(file: File): Boolean {
        if (!file.exists()) throw IllegalArgumentException("No such file=$file")
        LOG.ri("reconfigure() file={}", file)
        val context = getLoggerContext()
        return try {
            val configurator = JoranConfigurator()
            configurator.context = context
            context.reset()
            configurator.doConfigure(file)
            true
        } catch (e: Exception) {
            LOG.re("reconfigure() failed, file=$file", e)
            false
        }
    }

    /**
     * Check if the application files directory contains custom "logback.xml"
     * configuration file, and if it does, reconfigure logging using that file.
     */
    @JvmStatic
    fun checkCustomConfig(context: Context) {
        savePid()

        val customConfig = File(context.filesDir, "logback.xml")
        if (customConfig.exists()) {
            LOG.ri("checkCustomConfig() found file={}", customConfig)
            reconfigure(customConfig)
        }

        // Try to call debug test messages if BuildConfig.DEBUG is true
        try {
            val debugField = Class.forName("cam.et.dashcamlog.BuildConfig").getField("DEBUG")
            val debug = debugField.getBoolean(null)
            if (debug) {
                LOG.v("test VERBOSE level")
                LOG.d("test DEBUG level")
                LOG.i("test INFO level")
                LOG.w("test WARN level")
                LOG.e("test ERROR level")
            }
        } catch (t: Throwable) {
            // ignore
        }
    }

    /** Reconfigure the logging using the logback.xml file in assets. */
    @JvmStatic
    fun configureFromAssets(context: Context) {
        configureFromAssets(context.assets)
    }

    /** Reconfigure the logging using the logback.xml file in assets. */
    @JvmStatic
    fun configureFromAssets(assets: AssetManager) {
        val stream: InputStream = try {
            assets.open("logback.xml")
        } catch (e: IOException) {
            LOG.rw("configureFromAssets() no asset/logback.xml")
            return
        }
        LOG.ri("configureFromAssets()")

        val loggerContext = getLoggerContext()
        try {
            val configurator = JoranConfigurator()
            configurator.context = loggerContext
            loggerContext.reset()
            configurator.doConfigure(stream)
        } catch (e: Exception) {
            LOG.re("configureFromAssets() failed", e)
        } finally {
            try {
                stream.close()
            } catch (e: IOException) {
                // ignore
            }
        }
    }

    /** Create and add custom appender with DEBUG level threshold. */
    @JvmStatic
    fun addCustomAppender(output: OutputStream): AsyncAppender {
        return addCustomAppender(output, Level.DEBUG)
    }

    /** Create and add custom appender for the given log level. */
    @JvmStatic
    fun addCustomAppender(output: OutputStream, level: Level): AsyncAppender {
        return addCustomAppender(output, level, "%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{pid}] [%thread] %-5level %logger{1} %msg%n")
    }

    /** Create and add custom appender for the given log level and pattern. */
    @JvmStatic
    fun addCustomAppender(output: OutputStream, level: Level, pattern: String): AsyncAppender {
        requireNotNull(output) { "output must not be null" }
        requireNotNull(level) { "level must not be null" }
        requireNotNull(pattern) { "pattern must not be null" }

        val context = getLoggerContext()

        val encoder = PatternLayoutEncoder()
        encoder.context = context
        encoder.pattern = pattern
        encoder.start()

        val appender = OutputStreamAppender<ILoggingEvent>()
        appender.context = context
        appender.encoder = encoder
        appender.outputStream = output
        appender.start()

        val filter = ThresholdFilter()
        filter.context = context
        filter.setLevel(level.levelStr)
        filter.start()

        val async = AsyncAppender()
        async.context = context
        async.addFilter(filter)
        async.addAppender(appender)
        async.start()

        val root = context.getLogger(Logger.ROOT_LOGGER_NAME)
        root.addAppender(async)

        return async
    }

    /** Remove previously created custom appender. */
    @JvmStatic
    fun removeCustomAppender(appender: AsyncAppender?) {
        if (appender != null) {
            val context = getLoggerContext()
            val root = context.getLogger(Logger.ROOT_LOGGER_NAME)
            if (root.detachAppender(appender)) {
                appender.stop()
                appender.detachAndStopAllAppenders()
            }
        }
    }

    /** Flush all file appenders. */
    @JvmStatic
    fun flushFileAppenders() {
        val context = getLoggerContext()
        val root = context.getLogger(Logger.ROOT_LOGGER_NAME)
        val it = root.iteratorForAppenders()
        while (it.hasNext()) {
            val appender = it.next()
            if (appender is FileAppender<*>) {
                flushFileAppender(appender as FileAppender<ILoggingEvent>)
            } else if (appender is AsyncAppender) {
                val itAsync = appender.iteratorForAppenders()
                while (itAsync.hasNext()) {
                    val fileAppender = itAsync.next()
                    if (fileAppender is FileAppender) {
                        flushFileAppender(fileAppender as FileAppender<ILoggingEvent>)
                    }
                }
            }
        }
    }

    private fun flushFileAppender(pAppender: FileAppender<ILoggingEvent>) {
        val o = pAppender.outputStream
        if (o != null) {
            try {
                o.flush()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}