package cam.et.dashcamsystem.device

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility to provide and manage app-scoped directories and files for logs, results and images.
 *
 * Responsibilities:
 * - Provide directories (logs/results/images) under app-specific storage (externalFilesDir when available, otherwise internal filesDir).
 * - Create timestamped files and helpers to save text and bitmaps.
 * - Small housekeeping utilities like deleting old files and checking available space.
 *
 * Notes:
 * - Uses app-specific external directories returned by Context.getExternalFilesDir(null) which do not require external storage permission.
 * - SimpleDateFormat is only used on the caller (main) thread for generating names; methods are safe to call from background threads as they do not mutate shared state.
 */
class FilePathManager(private val context: Context, private val preferExternal: Boolean = true) {

    companion object {
        private const val DIR_LOGS = "logs"
        private const val DIR_RESULTS = "results"
        private const val DIR_IMAGES = "images"
        @Volatile
        private var NAME_TS_LOCALE: Locale = Locale.getDefault()
        // Keep a cached SimpleDateFormat for filename generation; recreate when locale changes.
        @Volatile
        private var NAME_TS_FORMAT: SimpleDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", NAME_TS_LOCALE)
    }

    /**
     * Base directory for app files. If external files dir is available and preferred, uses it; otherwise falls back to internal filesDir.
     */
    // The on-disk storage root we control (external files dir or internal files dir).
    private fun baseStorageDir(): File {
        if (preferExternal) {
            val ext = context.getExternalFilesDir(null)
            if (ext != null) return ext
        }
        return context.filesDir
    }

    // All our folders must live inside this application root directory named "DashcamSystem".
    private fun appRootDir(): File = ensureDir(File(baseStorageDir(), "DashcamSystem"))

    private fun ensureDir(dir: File): File {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /** Directory for logs (app-specific). */
    fun getLogsDir(): File = ensureDir(File(appRootDir(), DIR_LOGS))

    /** Directory for result files (app-specific). */
    fun getResultsDir(): File = ensureDir(File(appRootDir(), DIR_RESULTS))

    /** Directory for saved images (app-specific). */
    fun getImagesDir(): File = ensureDir(File(appRootDir(), DIR_IMAGES))

    /** Generate a timestamped filename with optional prefix and extension. Example: prefix_20250101_123012.ext */
    fun timestampedName(prefix: String = "file", extension: String? = null): String {
        val localeNow = Locale.getDefault()
        if (localeNow != NAME_TS_LOCALE) {
            synchronized(this) {
                if (localeNow != NAME_TS_LOCALE) {
                    NAME_TS_LOCALE = localeNow
                    NAME_TS_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", NAME_TS_LOCALE)
                }
            }
        }
        val ts = synchronized(NAME_TS_FORMAT) { NAME_TS_FORMAT.format(Date()) }
        return if (extension.isNullOrBlank()) "${prefix}_${ts}" else "${prefix}_${ts}.$extension"
    }

    /** Get a File for a new log file. If name is null, a timestamped name will be used ("log_yyyyMMdd_HHmms.txt"). */
    fun getLogFile(name: String? = null): File {
        val fname = name ?: timestampedName("log", "txt")
        return File(getLogsDir(), fname)
    }

    /** Get a File for a result file. If name is null, a timestamped name will be used ("result_yyyyMMdd_HHmms.json"). */
    fun getResultFile(name: String? = null, extension: String = "json"): File {
        val fname = name ?: timestampedName("result", extension)
        return File(getResultsDir(), fname)
    }

    /** Get a File for an image file. If name is null, a timestamped name will be used ("image_yyyyMMdd_HHmms.jpg"). */
    fun getImageFile(name: String? = null, extension: String = "jpg"): File {
        val fname = name ?: timestampedName("image", extension)
        return File(getImagesDir(), fname)
    }

    /** Save a text/string into the provided file. Returns true on success. */
    @Throws(IOException::class)
    fun saveText(file: File, text: String): Boolean {
        ensureDir(file.parentFile ?: getResultsDir())
        return try {
            file.writeText(text)
            true
        } catch (e: IOException) {
            throw e
        }
    }

    /**
     * Save a bitmap into an image file (JPEG by default). Returns the written file on success or throws.
     * The caller should call this from a background thread if the bitmap is large.
     */
    @Throws(IOException::class)
    fun saveBitmap(bitmap: Bitmap, file: File, quality: Int = 85): File {
        ensureDir(file.parentFile ?: getImagesDir())
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(file)
            val format = when (file.extension.lowercase(Locale.getDefault())) {
                "png" -> Bitmap.CompressFormat.PNG
                "webp" -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }
            bitmap.compress(format, quality, out)
            out.flush()
            return file
        } catch (e: IOException) {
            throw e
        } finally {
            try {
                out?.close()
            } catch (_: Exception) {
            }
        }
    }

    /** Return available bytes on the partition containing the given dir (or baseDir if null). */
    fun getAvailableSpaceBytes(dir: File? = null): Long {
        val d = dir ?: baseStorageDir()
        return try {
            d.usableSpace
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Delete files older than [olderThanMs] milliseconds in the given directory.
     * Returns the number of files deleted.
     */
    fun deleteFilesOlderThan(directory: File, olderThanMs: Long): Int {
        if (!directory.exists() || !directory.isDirectory) return 0
        val now = System.currentTimeMillis()
        var deleted = 0
        directory.listFiles()?.forEach { f ->
            try {
                if (f.isFile && (now - f.lastModified()) > olderThanMs) {
                    if (f.delete()) deleted++
                }
            } catch (_: Exception) {
            }
        }
        return deleted
    }

    /** Convenience: delete files older than given days from the logs directory. */
    fun cleanLogsOlderThanDays(days: Int): Int {
        val ms = days * 24L * 3600L * 1000L
        return deleteFilesOlderThan(getLogsDir(), ms)
    }

    /**
     * Try to create the legacy sdcard folder `/mnt/sdcard/DashcamSystem/logs` and write
     * a small timestamped text file into it. If creation or write fails (permissions,
     * storage restrictions), fall back to the app-specific logs directory returned by
     * [getLogsDir()].
     *
     * This method performs the write synchronously; callers should invoke it from a
     * background thread if they don't want to block startup.
     *
     * @return The File written on success (either on /mnt/sdcard path or app logs dir), or null on failure.
eturn */
    fun createLegacySdLogsDirAndWriteTimestampedFile(): File? {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val content = "Initialized logging at $timestamp\n"

        // Target legacy path
        val legacyDir = File("/mnt/sdcard/DashcamSystem/$DIR_LOGS")
        try {
            if (!legacyDir.exists()) {
                legacyDir.mkdirs()
            }
            if (legacyDir.exists() && legacyDir.isDirectory && legacyDir.canWrite()) {
                val fname = "log_${timestamp.replace(":", "-").replace(" ", "_")}.txt"
                val f = File(legacyDir, fname)
                try {
                    f.writeText(content)
                    return f
                } catch (e: Exception) {
                    // fallthrough to fallback below
                }
            }
        } catch (e: Exception) {
            // ignore and fallback
        }

        // Fallback to app-specific logs dir
        try {
            val appLogFile = getLogFile()
            try {
                saveText(appLogFile, content)
                return appLogFile
            } catch (e: Exception) {
                return null
            }
        } catch (e: Exception) {
            return null
        }
    }

}