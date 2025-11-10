package cam.et.dashcamsystem.device

import android.app.ActivityManager
import android.content.Context
import cam.et.dashcamsystem.util.FilePathManager
import kotlinx.coroutines.*
import java.io.RandomAccessFile
import kotlin.math.roundToInt


data class SystemUsage(
    val cpuPercent: Float,
    val usedMemMB: Long,
    val totalMemMB: Long,
    val availMemMB: Long,
    val storageAvailBytes: Long,
    val timestamp: Long
)

/**
 * Lightweight monitor that samples /proc/stat for CPU usage and ActivityManager for memory.
 * Call start(...) to begin periodic sampling and stop() to cancel.
 */
class SystemUsageMonitor(private val context: Context) {
    private var job: Job? = null
    private var storageJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // last snapshot values for CPU calculation
    private var lastTotal: Long = 0L
    private var lastIdle: Long = 0L

    // latest known storage available bytes (updated by storageJob)
    @Volatile
    private var storageAvailBytes: Long = 0L

    fun start(intervalMs: Long = 1000L, storagePollMs: Long = 60_000L, onUpdate: (SystemUsage) -> Unit) {
        stop()

        // Initialize last samples to reduce an incorrect first spike
        readProcStat()?.let { (total, idle) ->
            lastTotal = total
            lastIdle = idle
        }

        // Start storage poller if requested (> 0)
        if (storagePollMs > 0) {
            storageJob = scope.launch {
                while (isActive) {
                    storageAvailBytes = try {
                        FilePathManager.getAvailableSpaceBytes()
                    } catch (_: Exception) {
                        0L
                    }
                    delay(storagePollMs)
                }
            }
        }

        job = scope.launch {
            while (isActive) {
                val cpu = readCpuUsagePercent()
                val (used, total, avail) = readMemory()
                val usage = SystemUsage(cpu, used, total, avail, storageAvailBytes, System.currentTimeMillis())
                withContext(Dispatchers.Main) { onUpdate(usage) }
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        storageJob?.cancel()
        storageJob = null
    }

    private fun readMemory(): Triple<Long, Long, Long> {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(mi)
            val total = mi.totalMem / 1024 / 1024
            val avail = mi.availMem / 1024 / 1024
            val used = total - avail
            Triple(used, total, avail)
        } catch (_: Exception) {
            Triple(0L, 0L, 0L)
        }
    }

    private fun readProcStat(): Pair<Long, Long>? {
        try {
            RandomAccessFile("/proc/stat", "r").use { reader ->
                val line = reader.readLine() ?: return null
                if (!line.startsWith("cpu ")) return null
                val toks = line.split(Regex("\\s+"))
                // toks[0] is "cpu"; the rest are times
                val nums = toks.drop(1).map { it.toLongOrNull() ?: 0L }
                if (nums.isEmpty()) return null
                val idle = nums.getOrNull(3) ?: 0L // idle
                val iowait = nums.getOrNull(4) ?: 0L
                val idleAll = idle + iowait
                val total = nums.sum()
                return Pair(total, idleAll)
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun readCpuUsagePercent(): Float {
        val cur = readProcStat() ?: return 0f
        val curTotal = cur.first
        val curIdle = cur.second
        val totalDiff = curTotal - lastTotal
        val idleDiff = curIdle - lastIdle
        var usage = 0f
        if (totalDiff > 0) {
            usage = (1f - idleDiff.toFloat() / totalDiff.toFloat()) * 100f
        }
        lastTotal = curTotal
        lastIdle = curIdle
        if (usage.isNaN() || usage.isInfinite()) usage = 0f
        // round to one decimal place
        return (usage * 10).roundToInt() / 10f
    }
}
