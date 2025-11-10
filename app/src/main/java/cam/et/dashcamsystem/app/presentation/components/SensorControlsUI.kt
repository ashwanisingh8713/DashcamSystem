package cam.et.dashcamsystem.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale
import android.text.format.DateFormat
import java.util.Date

@Composable
fun SensorControlsCard(modifier: Modifier = Modifier) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val monitor = remember { cam.et.dashcamsystem.device.SystemSensorMonitor(ctx.applicationContext) }

    var sensorsRunning by remember { mutableStateOf(false) }
    // Location state now broken out so we can display provider, timestamp, and accuracy separately
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLon by remember { mutableStateOf<Double?>(null) }
    var locationProvider by remember { mutableStateOf<String?>(null) }
    var locationTime by remember { mutableStateOf<String?>(null) }
    var locationAccuracy by remember { mutableStateOf<String?>(null) }
    // Fallback toggle state (false = GPS-only, true = allow NETWORK fallback)
    var fallbackEnabled by remember { mutableStateOf(false) }

    var accelText by remember { mutableStateOf("No accel data") }
    var gyroText by remember { mutableStateOf("No gyro data") }
    var cameraFrames by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            // Ensure monitoring is stopped when the UI leaves composition
            monitor.stopAll()
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            // Row with toggle for fallback behavior
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Allow NETWORK fallback:")
                Switch(checked = fallbackEnabled, onCheckedChange = { checked ->
                    fallbackEnabled = checked
                    monitor.setLocationFallbackEnabled(checked)
                }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    // Register listeners
                    monitor.setLocationListener(object : cam.et.dashcamsystem.device.LocationMonitor.Listener {
                        override fun onLocation(location: android.location.Location) {
                            locationLat = location.latitude
                            locationLon = location.longitude
                            locationProvider = location.provider ?: "unknown"
                            locationAccuracy = String.format(Locale.US, "%.1f m", location.accuracy)
                            // Format timestamp in a readable form (local device time)
                            val ts = try {
                                DateFormat.format("yyyy-MM-dd HH:mm:ss", Date(location.time)).toString()
                            } catch (_: Exception) {
                                null
                            }
                            locationTime = ts
                        }

                        override fun onProviderDisabled() {
                            // reflect provider disabled state
                            locationProvider = "disabled"
                        }

                        override fun onProviderEnabled() {
                            // no-op
                        }
                    })

                    monitor.setAccelerometerListener(object : cam.et.dashcamsystem.device.MotionSensorMonitor.AccelerometerListener {
                        override fun onAccelerometer(x: Float, y: Float, z: Float, timestampNs: Long) {
                            accelText = String.format(Locale.US, "%.2f, %.2f, %.2f", x, y, z)
                        }
                    })

                    monitor.setGyroscopeListener(object : cam.et.dashcamsystem.device.MotionSensorMonitor.GyroscopeListener {
                        override fun onGyroscope(x: Float, y: Float, z: Float, timestampNs: Long) {
                            gyroText = String.format(Locale.US, "%.2f, %.2f, %.2f", x, y, z)
                        }
                    })

                    monitor.setCameraFrameListener(object : cam.et.dashcamsystem.device.CameraMonitor.FrameListener {
                        override fun onFrame(image: android.media.ImageReader) {
                            // Acquire & close quickly to avoid blocking the producer
                            val img = try { image.acquireNextImage() } catch (_: Exception) { null }
                            img?.close()
                            cameraFrames += 1
                        }
                    })

                    monitor.startAll()
                    sensorsRunning = true
                }) {
                    Text(text = "Start Sensors")
                }

                Button(onClick = {
                    monitor.stopAll()
                    sensorsRunning = false
                }) {
                    Text(text = "Stop Sensors")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Sensors running: ${if (sensorsRunning) "YES" else "NO"}")
            Spacer(modifier = Modifier.height(4.dp))

            // Location display: show coords (if available), accuracy, provider and timestamp
            val coordsText = if (locationLat != null && locationLon != null) {
                String.format(Locale.US, "%.6f, %.6f", locationLat, locationLon)
            } else {
                "No location yet"
            }

            Text(text = "Location: $coordsText", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Accuracy: ${locationAccuracy ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
            // If provider is disabled, show red text to highlight the issue
            val providerText = locationProvider ?: "N/A"
            val providerColor = if (providerText == "disabled") Color.Red else MaterialTheme.colorScheme.onSurface
            Text(text = "Provider: $providerText", style = MaterialTheme.typography.bodyMedium, color = providerColor)
             Text(text = "Timestamp: ${locationTime ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)

            Text(text = "Accel: $accelText", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Gyro: $gyroText", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Camera frames: $cameraFrames", style = MaterialTheme.typography.bodyMedium)
        }
    }
}