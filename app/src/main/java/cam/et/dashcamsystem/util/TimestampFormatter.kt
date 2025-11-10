package cam.et.dashcamsystem.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small helper to format timestamps in a consistent yyyy-MM-dd HH:mm:ss form.
 * Caches a SimpleDateFormat and recreates it if Locale changes. SimpleDateFormat
 * isn't thread-safe; this implementation synchronizes access to the formatter.
 */
object TimestampFormatter {
    @Volatile
    private var locale: Locale = Locale.getDefault()
    @Volatile
    private var sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)

    fun format(timeMs: Long): String {
        val nowLoc = Locale.getDefault()
        if (nowLoc != locale) {
            synchronized(this) {
                if (nowLoc != locale) {
                    locale = nowLoc
                    sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)
                }
            }
        }
        return synchronized(sdf) { sdf.format(Date(timeMs)) }
    }
}