package cam.et.dashcamsystem.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cam.et.dashcamsystem.device.SystemUsageMonitor
import cam.et.dashcamsystem.logger.DashcamLog

@Composable
fun SystemUsageCard(modifier: Modifier = Modifier, storagePollMs: Long) {
    // Logger that will write system usage updates to the NemoLog-backed logging system
    val LOG = remember { DashcamLog.get("SystemUsageMonitor") }
    // Compose state to hold latest usage
    var systemUsage by remember { mutableStateOf(cam.et.dashcamsystem.device.SystemUsage(0f, 0, 0, 0, 0, 0)) }
    val ctx = LocalContext.current

    // Start/stop monitor with the composable lifecycle (fast cadence for CPU/memory)
    DisposableEffect(ctx) {
        val monitor = SystemUsageMonitor(ctx)
        monitor.start(1000L, storagePollMs) { newUsage ->
            systemUsage = newUsage
            // Log updates; storage is included in newUsage.storageAvailBytes
            LOG.d("CPU: ${newUsage.cpuPercent}%, Memory: ${newUsage.usedMemMB}MB/${newUsage.totalMemMB}MB (avail ${newUsage.availMemMB}MB), StorageAvail: ${newUsage.storageAvailBytes}, ts: ${newUsage.timestamp}")
        }
        onDispose {
            monitor.stop()
        }
    }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "CPU: ${systemUsage.cpuPercent}%", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Memory: ${systemUsage.usedMemMB}MB / ${systemUsage.totalMemMB}MB (avail ${systemUsage.availMemMB}MB)", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Storage available: ${formatBytes(systemUsage.storageAvailBytes)}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Updated: ${cam.et.dashcamsystem.device.TimestampFormatter.format(systemUsage.timestamp)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Format bytes into a human-readable string with appropriate unit.
 */
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(java.util.Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Preview(showBackground = true)
@Composable
fun SystemUsageCardPreview() {
    // sample preview data
    val sample = cam.et.dashcamsystem.device.SystemUsage(12.5f, 512, 2048, 1536, 32L * 1024 * 1024 * 1024, System.currentTimeMillis())
    Card(modifier = Modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "CPU: ${sample.cpuPercent}%", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Memory: ${sample.usedMemMB}MB / ${sample.totalMemMB}MB (avail ${sample.availMemMB}MB)", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Storage available: ${formatBytes(sample.storageAvailBytes)}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Updated: ${cam.et.dashcamsystem.device.TimestampFormatter.format(sample.timestamp)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SystemUsage(modifier: Modifier = Modifier.fillMaxWidth(), storagePollMs: Long = 60000L) {
    // Public entrypoint requested by the user; delegates to the card implementation.
    SystemUsageCard(modifier = modifier, storagePollMs = storagePollMs)
}

@Preview(showBackground = true)
@Composable
fun SystemUsagePreview() {
    SystemUsage()
}
