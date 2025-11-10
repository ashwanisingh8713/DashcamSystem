package cam.et.dashcamsystem.device

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Small example showing how an Activity could use SystemSensorMonitor.
 * This file is illustrative only and is not wired into the app automatically.
 */
object SensorIntegrationExample {

    private const val REQUEST_PERMISSIONS = 42
    private val REQUIRED = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun hasPermissions(activity: Activity): Boolean {
        return REQUIRED.all { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }
    }

    fun requestPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(activity, REQUIRED, REQUEST_PERMISSIONS)
    }

    // Example instantiation + lifecycle usage (call from Activity)
    fun createMonitor(activity: Activity): SystemSensorMonitor {
        val monitor = SystemSensorMonitor(activity.applicationContext)
        monitor.setLocationListener(object : LocationMonitor.Listener {
            override fun onLocation(location: android.location.Location) {
                // handle location
            }

            override fun onProviderDisabled() {}
            override fun onProviderEnabled() {}
        })

        monitor.setAccelerometerListener(object : MotionSensorMonitor.AccelerometerListener {
            override fun onAccelerometer(x: Float, y: Float, z: Float, timestampNs: Long) {
                // handle accel
            }
        })

        monitor.setGyroscopeListener(object : MotionSensorMonitor.GyroscopeListener {
            override fun onGyroscope(x: Float, y: Float, z: Float, timestampNs: Long) {
                // handle gyro
            }
        })

        monitor.setCameraFrameListener(object : CameraMonitor.FrameListener {
            override fun onFrame(image: android.media.ImageReader) {
                // IMPORTANT: acquire and close images to avoid memory leak
                val img = try { image.acquireNextImage() } catch (e: Exception) { null }
                img?.close()
            }
        })

        return monitor
    }
}

