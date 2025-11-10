package cam.et.dashcamsystem.device

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Singleton utility to provide and manage app-scoped directories and files for logs, results and images.
 *
 * Use FilePathManager.init(applicationContext) once (for example in Application.onCreate()).
 * After that you can call the helper methods anywhere without creating an instance.
 */
@SuppressLint("StaticFieldLeak")
object FilePathManager {

    private const val DIR_LOGS = "logs"
    private const val DIR_RESULTS = "results"
    private const val DIR_IMAGES = "images"

    @Volatile
    private var NAME_TS_LOCALE: Locale = Locale.getDefault()
    @Volatile
    private var NAME_TS_FORMAT: SimpleDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", NAME_TS_LOCALE)
    private val tsFormatLock = Any()

    // Application context (set once)
    private var ctx: Context? = null

    /** Initialize the manager with application context. Call once early (e.g. Application.onCreate()). */
    fun init(context: Context) {
        ctx = context.applicationContext
    }

    private fun requireCtx(): Context = ctx ?: throw IllegalStateException("FilePathManager not initialized. Call FilePathManager.init(context) first.")

    // Base directory for app files. Prefer app-specific external files dir; fall back to internal filesDir.
    private fun baseStorageDir(): File {
        val context = requireCtx()
        val ext = context.getExternalFilesDir(null)
        return ext ?: context.filesDir
    }

    // Ensure application root directory named "DashcamSystem".
    // Try a few common external storage roots (covers many device models). If none are usable,
    // fall back to the app-specific storage directory.
    private fun appRootDir(): File {
        val candidates = buildList {
            add("/mnt/sdcard")
            add("/storage/emulated/0")
            try {
                if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                    Environment.getExternalStorageDirectory()?.absolutePath?.let { add(it) }
                }
            } catch (_: Exception) {
            }
        }

        for (basePath in candidates.distinct()) {
            val root = File(basePath, "DashcamSystem")
            if (ensureWritable(root)) return root
        }

        // Fallback to app-specific storage
        return ensureDir(File(baseStorageDir(), "DashcamSystem"))
    }

    // Try to create the directory and verify writability by creating a small probe file.
    private fun ensureWritable(dir: File): Boolean {
        try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            if (!dir.exists()) return false
            if (dir.canWrite()) return true
            // Probe by creating and removing a small file
            val probe = File(dir, ".probe")
            return try {
                probe.createNewFile() && probe.exists().also { probe.delete() }
            } catch (_: Exception) {
                false
            }
        } catch (_: Exception) {
            return false
        }
    }

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
            synchronized(tsFormatLock) {
                if (localeNow != NAME_TS_LOCALE) {
                    NAME_TS_LOCALE = localeNow
                    NAME_TS_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", NAME_TS_LOCALE)
                }
            }
        }
        val ts = synchronized(tsFormatLock) { NAME_TS_FORMAT.format(Date()) }
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

    /** Get a File for an image file. If name is null, a timestamped name will be used ("image_yyyyMMdd_HHmmss.jpg"). */
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


}