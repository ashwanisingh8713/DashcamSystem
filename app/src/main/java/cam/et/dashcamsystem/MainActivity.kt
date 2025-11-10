package cam.et.dashcamsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cam.et.dashcamsystem.app.presentation.components.SystemUsage
import cam.et.dashcamsystem.ui.theme.DashcamSystemTheme
import cam.et.dashcamsystem.permissions.PermissionManager
import android.widget.Toast
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionManager = PermissionManager(this)

        // If all permissions are not granted, invoke callback behavior directly.
        if (!permissionManager.allPermissionsGranted()) {
            permissionManager.requestPermissions(object : PermissionManager.Callback {
                override fun onResult(allGranted: Boolean, denied: List<String>) {
                    if (!allGranted) {
                        // Simple feedback; the PermissionManager handles the requests.
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Permissions denied: ${denied.joinToString()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        // Nothing needed here; UI handles runtime checks as well.
                    }
                }
            })
        }

        setContent {
            DashcamSystemTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
                        Greeting(name = "Assignment")
                        Spacer(modifier = Modifier.height(12.dp))

                        // System usage card (existing)
                        SystemUsage()

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sensor controls & status UI
                        SensorControls(permissionManager = permissionManager)
                    }
                }
            }
        }
    }
}

@Composable
fun SensorControls(permissionManager: PermissionManager) {
    // Create the monitor tied to the application context and remember across recompositions
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val monitor = remember { cam.et.dashcamsystem.device.SystemSensorMonitor(ctx.applicationContext) }

    var permissionsGranted by remember { mutableStateOf(permissionManager.allPermissionsGranted()) }
    var sensorsRunning by remember { mutableStateOf(false) }
    var locationText by remember { mutableStateOf("No location yet") }
    var accelText by remember { mutableStateOf("No accel data") }
    var gyroText by remember { mutableStateOf("No gyro data") }
    var cameraFrames by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            // Ensure monitoring is stopped when the UI leaves composition
            monitor.stopAll()
        }
    }

    Column(modifier = Modifier.padding(8.dp)) {
        Text(text = "Permissions: ${if (permissionsGranted) "GRANTED" else "MISSING"}")
        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                // Request permissions via the existing permission manager
                permissionManager.requestPermissions(object : PermissionManager.Callback {
                    override fun onResult(allGranted: Boolean, denied: List<String>) {
                        permissionsGranted = allGranted
                        if (!allGranted) {
                            // show toast from current context
                            Toast.makeText(ctx, "Permissions denied: ${denied.joinToString()}", Toast.LENGTH_LONG).show()
                        }
                    }
                })
            }) {
                Text(text = "Request Permissions")
            }

            Button(onClick = {
                if (!permissionsGranted) {
                    Toast.makeText(ctx, "Please grant permissions first", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // Register listeners
                monitor.setLocationListener(object : cam.et.dashcamsystem.device.LocationMonitor.Listener {
                    override fun onLocation(location: android.location.Location) {
                        locationText = "${location.latitude}, ${location.longitude} (acc ${location.accuracy})"
                    }

                    override fun onProviderDisabled() {
                        locationText = "Provider disabled"
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
        Text(text = "Location: $locationText", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Accel: $accelText", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Gyro: $gyroText", style = MaterialTheme.typography.bodyMedium)
        Text(text = "Camera frames: $cameraFrames", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "EdgeTensor $name",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DashcamSystemTheme {
        Greeting("Android")
    }
}