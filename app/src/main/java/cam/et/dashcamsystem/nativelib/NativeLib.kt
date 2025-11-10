package cam.et.dashcamsystem.nativelib

object NativeLib {
    init {
        // library already loaded in MainActivity but safe to load again
        System.loadLibrary("dashcamsystem")
    }

    // pixels in ARGB_8888 order
    external fun isImageDark(pixels: IntArray, width: Int, height: Int, threshold: Int): Boolean

    // Save raw bytes to path using POSIX file operations
    external fun saveBytesToFile(path: String, bytes: ByteArray): Boolean

    // Append a UTF-8 log line to a file
    external fun appendLog(path: String, line: String): Boolean
}

