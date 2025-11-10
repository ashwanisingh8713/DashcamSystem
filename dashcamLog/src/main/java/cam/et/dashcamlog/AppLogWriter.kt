package cam.et.dashcamlog

import org.slf4j.Logger

/**
 * A hook to be used with AppLog
 * Override this implementation and set it to AppLog to receive debug messages
 */
open class AppLogWriter {

    /** Write debug message */
    open fun write(logger: Logger, level: AppLog.Level, message: String) {

    }

    /** Write debug message with throwable */
    open fun write(logger: Logger, level: AppLog.Level, message: String, t: Throwable?) {

    }

    /** Return true if messages are required. */
    open fun isWriting(): Boolean {
        return false
    }

    /** Return true if release-type logs are required. */
    open fun isWritingReleaseLogs(): Boolean {
        return true
    }
}

