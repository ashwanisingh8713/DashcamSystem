package cam.et.dashcamsystem.device

import android.content.Context

/**
 * Facade that manages GPS (location), accelerometer, gyroscope and camera monitors.
 * Use startAll() / stopAll() to control sensors. Register listeners to receive updates.
 */
class SystemSensorMonitor(context: Context) {

    private val locationMonitor = LocationMonitor(context)
    private val motionMonitor = MotionSensorMonitor(context)
    private val cameraMonitor = CameraMonitor(context)

    // Location
    fun setLocationListener(listener: LocationMonitor.Listener?) {
        locationMonitor.locationListener = listener
    }

    /**
     * Control whether location monitor may fall back to NETWORK when GPS is unavailable.
     */
    fun setLocationFallbackEnabled(enabled: Boolean) {
        locationMonitor.setUseFallback(enabled)
    }

    // Motion
    fun setAccelerometerListener(listener: MotionSensorMonitor.AccelerometerListener?) {
        motionMonitor.accelerometerListener = listener
    }

    fun setGyroscopeListener(listener: MotionSensorMonitor.GyroscopeListener?) {
        motionMonitor.gyroscopeListener = listener
    }

    // Camera
    fun setCameraFrameListener(listener: CameraMonitor.FrameListener?) {
        cameraMonitor.frameListener = listener
    }

    // Start/stop all sensors
    fun startAll() {
        locationMonitor.start()
        motionMonitor.start()
        cameraMonitor.start()
    }

    fun stopAll() {
        cameraMonitor.stop()
        motionMonitor.stop()
        locationMonitor.stop()
    }

    // Expose individual controls if caller wants more granularity
    fun startLocation() = locationMonitor.start()
    fun stopLocation() = locationMonitor.stop()

    fun startMotion() = motionMonitor.start()
    fun stopMotion() = motionMonitor.stop()

    fun startCamera() = cameraMonitor.start()
    fun stopCamera() = cameraMonitor.stop()
}
