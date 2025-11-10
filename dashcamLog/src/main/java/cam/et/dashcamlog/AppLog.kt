package cam.et.dashcamlog

import android.content.Context
import com.github.tony19.logback.android.BuildConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.helpers.FormattingTuple
import org.slf4j.helpers.MessageFormatter
import java.lang.reflect.Method


/**
 * Replacement for the android logger. Kotlin port of NemoLog.java
 */
class AppLog {

    init {
        AppLogConfigurator.checkDefaultConfig()
    }

    private val mLogger: Logger

    private constructor(clazz: Class<*>) {
        if (clazz == null) throw IllegalArgumentException()
        mLogger = LoggerFactory.getLogger(clazz)
    }

    private constructor(name: String) {
        if (name == null) throw IllegalArgumentException()
        mLogger = LoggerFactory.getLogger(name)
    }

    fun get(): Logger = mLogger

    enum class Level(c: Char, nblType: Int) {
        VERBOSE('V', 4),
        DEBUG('D', 4),
        WARNING('W', 3),
        INFO('I', 3),
        ERROR('E', 2),
        FATAL('F', 1);

        val levelChar: Char = c
        val nblTypeVal: Int = nblType
    }

    companion object {
        private var sLogWriter: AppLogWriter = AppLogWriter()
        private var sAssertOnFatal: Boolean = false

        init {
            AppLogConfigurator.checkDefaultConfig()
        }

        @JvmStatic
        fun isDebug(): Boolean = BuildConfig.DEBUG

        @JvmStatic
        fun isConnectedToFileWriter(): Boolean = sLogWriter.isWriting()

        @JvmStatic
        fun setLogWriter(writer: AppLogWriter) {
            sLogWriter = writer
        }

        @JvmStatic
        fun setAssertOnFatal(assertOnFatal: Boolean) {
            sAssertOnFatal = assertOnFatal
        }

        @JvmStatic
        fun get(clazz: Class<*>): AppLog = AppLog(clazz)

        @JvmStatic
        fun get(name: String): AppLog = AppLog(name)

        @JvmStatic
        fun init(context: Context) {
            AppLogConfigurator.checkDefaultConfig(context)
        }
    }

    /* l() overloads */
    fun l(level: Level, format: String, arg: Any?) {
        when (level) {
            Level.DEBUG -> d(format, arg)
            Level.ERROR -> e(format, arg)
            Level.INFO -> i(format, arg)
            Level.VERBOSE -> v(format, arg)
            Level.WARNING -> w(format, arg)
            Level.FATAL -> f(format, arg)
        }
    }

    fun l(level: Level, format: String, vararg arguments: Any?) {
        when (level) {
            Level.DEBUG -> d(format, *arguments)
            Level.ERROR -> e(format, *arguments)
            Level.INFO -> i(format, *arguments)
            Level.VERBOSE -> v(format, *arguments)
            Level.WARNING -> w(format, *arguments)
            Level.FATAL -> f(format, *arguments)
        }
    }

    fun l(level: Level, format: String, arg1: Any?, arg2: Any?) {
        when (level) {
            Level.DEBUG -> d(format, arg1, arg2)
            Level.ERROR -> e(format, arg1, arg2)
            Level.INFO -> i(format, arg1, arg2)
            Level.VERBOSE -> v(format, arg1, arg2)
            Level.WARNING -> w(format, arg1, arg2)
            Level.FATAL -> f(format, arg1, arg2)
        }
    }

    fun l(level: Level, msg: String, tr: Throwable?) {
        when (level) {
            Level.DEBUG -> d(msg, tr)
            Level.ERROR -> e(msg, tr)
            Level.INFO -> i(msg, tr)
            Level.VERBOSE -> v(msg, tr)
            Level.WARNING -> w(msg, tr)
            Level.FATAL -> f(msg, tr)
        }
    }

    fun l(levelOrdinal: Int, msg: String) {
        val l = Level.values()[levelOrdinal]
        l(l, msg)
    }

    /* VERBOSE */
    fun v(msg: String) {
        if (mLogger.isTraceEnabled) {
            if (BuildConfig.DEBUG) {
                mLogger.trace(msg)
            }
        }
    }

    fun v(format: String, arg: Any?) {
        if (mLogger.isTraceEnabled) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg)
            val msg = ft.message
            if (BuildConfig.DEBUG) {
                mLogger.trace(msg)
            }
        }
    }

    fun v(format: String, arg1: Any?, arg2: Any?) {
        if (mLogger.isTraceEnabled) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
            val msg = ft.message
            if (BuildConfig.DEBUG) {
                mLogger.trace(msg)
            }
        }
    }

    fun v(format: String, vararg arguments: Any?) {
        if (mLogger.isTraceEnabled) {
            val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
            val msg = ft.message
            if (BuildConfig.DEBUG) {
                mLogger.trace(msg)
            }
        }
    }

    fun v(msg: String, tr: Throwable?) {
        if (mLogger.isTraceEnabled) {
            if (BuildConfig.DEBUG) {
                mLogger.trace(msg, tr)
            }
        }
    }

    fun rv(msg: String) {
        mLogger.trace(msg)
    }

    fun rv(format: String, arg: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg)
        val msg = ft.message
        mLogger.trace(msg)
    }

    fun rv(format: String, arg1: Any?, arg2: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
        val msg = ft.message
        mLogger.trace(msg)
    }

    fun rv(format: String, vararg arguments: Any?) {
        val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
        val msg = ft.message
        mLogger.trace(msg)
    }

    fun rv(msg: String, tr: Throwable?) {
        mLogger.trace(msg, tr)
    }

    /* DEBUG */
    fun d(msg: String) {
        if (mLogger.isDebugEnabled || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg)
            if (BuildConfig.DEBUG) {
                mLogger.debug(msg)
            }
        }
    }

    fun d(format: String, arg: Any?) {
        if (mLogger.isDebugEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.DEBUG, msg)
            if (BuildConfig.DEBUG) {
                mLogger.debug(msg)
            }
        }
    }

    fun d(format: String, arg1: Any?, arg2: Any?) {
        if (mLogger.isDebugEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.DEBUG, msg)
            if (BuildConfig.DEBUG) {
                mLogger.debug(msg)
            }
        }
    }

    fun d(format: String, vararg arguments: Any?) {
        if (mLogger.isDebugEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.DEBUG, msg)
            if (BuildConfig.DEBUG) {
                mLogger.debug(msg)
            }
        }
    }

    fun d(msg: String, tr: Throwable?) {
        if (mLogger.isDebugEnabled || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg, tr)
            if (BuildConfig.DEBUG) {
                mLogger.debug(msg, tr)
            }
        }
    }

    fun rd(msg: String) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg)
        }
        mLogger.debug(msg)
    }

    fun rd(format: String, arg: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg)
        }
        mLogger.debug(msg)
    }

    fun rd(format: String, arg1: Any?, arg2: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg)
        }
        mLogger.debug(msg)
    }

    fun rd(format: String, vararg arguments: Any?) {
        val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg)
        }
        mLogger.debug(msg)
    }

    fun rd(msg: String, tr: Throwable?) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.DEBUG, msg, tr)
        }
        mLogger.debug(msg, tr)
    }

    /* INFO */
    fun i(msg: String) {
        if (mLogger.isInfoEnabled || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.INFO, msg)
            if (BuildConfig.DEBUG) {
                mLogger.info(msg)
            }
        }
    }

    fun i(format: String, arg: Any?) {
        if (mLogger.isInfoEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.INFO, msg)
            if (BuildConfig.DEBUG) {
                mLogger.info(msg)
            }
        }
    }

    fun i(format: String, arg1: Any?, arg2: Any?) {
        if (mLogger.isInfoEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.INFO, msg)
            if (BuildConfig.DEBUG) {
                mLogger.info(msg)
            }
        }
    }

    fun i(format: String, vararg arguments: Any?) {
        if (mLogger.isInfoEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.INFO, msg)
            if (BuildConfig.DEBUG) {
                mLogger.info(msg)
            }
        }
    }

    fun i(msg: String, tr: Throwable?) {
        if (mLogger.isInfoEnabled || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.INFO, msg, tr)
            if (BuildConfig.DEBUG) {
                mLogger.info(msg, tr)
            }
        }
    }

    fun ri(msg: String) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.INFO, msg)
        }
        mLogger.info(msg)
    }

    fun ri(format: String, arg: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.INFO, msg)
        }
        mLogger.info(msg)
    }

    fun ri(format: String, arg1: Any?, arg2: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.INFO, msg)
        }
        mLogger.info(msg)
    }

    fun ri(format: String, vararg arguments: Any?) {
        val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.INFO, msg)
        }
        mLogger.info(msg)
    }

    fun ri(msg: String, tr: Throwable?) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.INFO, msg, tr)
        }
        mLogger.info(msg, tr)
    }

    /* WARN */
    fun w(msg: String) {
        if (mLogger.isWarnEnabled || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.WARNING, msg)
            if (BuildConfig.DEBUG) {
                mLogger.warn(msg)
            }
        }
    }

    fun w(format: String, arg: Any?) {
        if (mLogger.isWarnEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.WARNING, msg)
            if (BuildConfig.DEBUG) {
                mLogger.warn(msg)
            }
        }
    }

    fun w(format: String, arg1: Any?, arg2: Any?) {
        if (mLogger.isWarnEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.WARNING, msg)
            if (BuildConfig.DEBUG) {
                mLogger.warn(msg)
            }
        }
    }

    fun w(format: String, vararg arguments: Any?) {
        if (mLogger.isWarnEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.WARNING, msg)
            if (BuildConfig.DEBUG) {
                mLogger.warn(msg)
            }
        }
    }

    fun w(msg: String, tr: Throwable?) {
        if (mLogger.isWarnEnabled || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.WARNING, msg, tr)
            if (BuildConfig.DEBUG) {
                mLogger.warn(msg, tr)
            }
        }
    }

    fun rw(msg: String) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.WARNING, msg)
        }
        mLogger.warn(msg)
    }

    fun rw(format: String, arg: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.WARNING, msg)
        }
        mLogger.warn(msg)
    }

    fun rw(format: String, arg1: Any?, arg2: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.WARNING, msg)
        }
        mLogger.warn(msg)
    }

    fun rw(format: String, vararg arguments: Any?) {
        val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.WARNING, msg)
        }
        mLogger.warn(msg)
    }

    fun rw(msg: String, tr: Throwable?) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.WARNING, msg, tr)
        }
        mLogger.warn(msg, tr)
    }

    /* ERROR */
    fun e(msg: String) {
        if (mLogger.isErrorEnabled || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.ERROR, msg)
            if (BuildConfig.DEBUG) {
                mLogger.error(msg)
            }
        }
    }

    fun e(format: String, arg: Any?) {
        if (mLogger.isErrorEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.ERROR, msg)
            if (BuildConfig.DEBUG) {
                mLogger.error(msg)
            }
        }
    }

    fun e(format: String, arg1: Any?, arg2: Any?) {
        if (mLogger.isErrorEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.ERROR, msg)
            if (BuildConfig.DEBUG) {
                mLogger.error(msg)
            }
        }
    }

    fun e(format: String, vararg arguments: Any?) {
        if (mLogger.isErrorEnabled || sLogWriter.isWriting()) {
            val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
            val msg = ft.message
            sLogWriter.write(mLogger, Level.ERROR, msg)
            if (BuildConfig.DEBUG) {
                mLogger.error(msg)
            }
        }
    }

    fun e(msg: String, tr: Throwable?) {
        if (mLogger.isErrorEnabled || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.ERROR, msg, tr)
            if (BuildConfig.DEBUG) {
                mLogger.error(msg, tr)
            }
        }
    }

    fun re(msg: String) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.ERROR, msg)
        }
        mLogger.error(msg)
    }

    fun re(format: String, arg: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.ERROR, msg)
        }
        mLogger.error(msg)
    }

    fun re(format: String, arg1: Any?, arg2: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.ERROR, msg)
        }
        mLogger.error(msg)
    }

    fun re(format: String, vararg arguments: Any?) {
        val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
        val msg = ft.message
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.ERROR, msg)
        }
        mLogger.error(msg)
    }

    fun re(msg: String, tr: Throwable?) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.ERROR, msg, tr)
        }
        mLogger.error(msg, tr)
    }

    /* FATAL */
    fun f(msg: String) {
        if (mLogger.isErrorEnabled || sLogWriter.isWriting() || sAssertOnFatal) {
            val msg2 = msg + getStackTraceWhenNotAsserting()
            sLogWriter.write(mLogger, Level.FATAL, msg2)
            if (BuildConfig.DEBUG) {
                mLogger.error(msg2)
            }
            if (sAssertOnFatal) {
                // no-op for now
            }
        }
    }

    fun f(format: String, arg: Any?) {
        if (mLogger.isErrorEnabled || sLogWriter.isWriting() || sAssertOnFatal) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg)
            val msg = ft.message + getStackTraceWhenNotAsserting()
            sLogWriter.write(mLogger, Level.FATAL, msg)
            if (BuildConfig.DEBUG) {
                mLogger.error(msg)
            }
        }
    }

    fun f(format: String, arg1: Any?, arg2: Any?) {
        if (mLogger.isErrorEnabled || sLogWriter.isWriting() || sAssertOnFatal) {
            val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
            val msg = ft.message + getStackTraceWhenNotAsserting()
            sLogWriter.write(mLogger, Level.FATAL, msg)
            if (BuildConfig.DEBUG) {
                mLogger.error(msg)
            }
        }
    }

    fun f(format: String, vararg arguments: Any?) {
        if (mLogger.isErrorEnabled || sLogWriter.isWriting() || sAssertOnFatal) {
            val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
            val msg = ft.message + getStackTraceWhenNotAsserting()
            sLogWriter.write(mLogger, Level.FATAL, msg)
            if (BuildConfig.DEBUG) {
                mLogger.error(msg)
            }
            if (sAssertOnFatal) {
                // no-op
            }
        }
    }

    fun f(msg: String, tr: Throwable?) {
        if (mLogger.isErrorEnabled || sLogWriter.isWriting()) {
            sLogWriter.write(mLogger, Level.FATAL, msg, tr)
            if (BuildConfig.DEBUG) {
                mLogger.error(msg, tr)
            }
            if (sAssertOnFatal) {
                // no-op
            }
        }
    }

    fun rf(msg: String) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.FATAL, msg + getStackTraceWhenNotAsserting())
        }
        mLogger.error(msg)
        if (sAssertOnFatal) {
            // no-op
        }
    }

    fun rf(format: String, arg: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg)
        val msg = ft.message + getStackTraceWhenNotAsserting()
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.FATAL, msg)
        }
        mLogger.error(msg)
    }

    fun rf(format: String, arg1: Any?, arg2: Any?) {
        val ft: FormattingTuple = MessageFormatter.format(format, arg1, arg2)
        val msg = ft.message + getStackTraceWhenNotAsserting()
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.FATAL, msg)
        }
        mLogger.error(msg)
    }

    fun rf(format: String, vararg arguments: Any?) {
        val ft: FormattingTuple = MessageFormatter.arrayFormat(format, arguments)
        val msg = ft.message + getStackTraceWhenNotAsserting()
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.FATAL, msg)
        }
        mLogger.error(msg)
    }

    fun rf(msg: String, tr: Throwable?) {
        if (sLogWriter.isWritingReleaseLogs()) {
            sLogWriter.write(mLogger, Level.FATAL, msg, tr)
        }
        mLogger.error(msg, tr)
    }

    private fun getStackTraceWhenNotAsserting(): String {
        if (sAssertOnFatal) return ""
        val stack = Thread.currentThread().stackTrace
        val b = StringBuilder()
        for (element in stack) {
            b.append('\n').append(element.toString())
        }
        return b.toString()
    }

    companion object ReflectionHelpers {
        @JvmStatic
        fun methodNameGet(): String? {
            val methods: Array<Method> = AppLog::class.java.declaredMethods
            for (m in methods) {
                val rType = m.returnType
                val pTypes = m.parameterTypes
                val onlyParam = if (pTypes.size == 1) pTypes[0] else null
                if (rType == AppLog::class.java && pTypes.size == 1 && pTypes[0] == String::class.java) {
                    return m.name
                }
            }
            return null
        }

        @JvmStatic
        fun methodNameL(): String? {
            val methods: Array<Method> = AppLog::class.java.declaredMethods
            for (m in methods) {
                val name = m.name
                val rType = m.returnType
                val pTypes = m.parameterTypes
                if (rType == Void.TYPE && pTypes.size == 2 && pTypes[0] == Int::class.javaPrimitiveType && pTypes[1] == String::class.java) {
                    return name
                }
            }
            return null
        }
    }
}