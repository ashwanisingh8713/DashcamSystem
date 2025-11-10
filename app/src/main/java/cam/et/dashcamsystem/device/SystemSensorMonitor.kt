package cam.et.dashcamsystem.device

import android.content.Context

/**
 * Facade that manages GPS (location), accelerometer, gyroscope and camera monitors.
 * Use startAll() / stopAll() to control sensors. Register listeners to receive updates.
 */
class SystemSensorMonitor(context: Context) {

    private val locationMonitor = cam.et.dashcamsystem.device.LocationMonitor(context)
    private val motionMonitor = cam.et.dashcamsystem.device.MotionSensorMonitor(context)
    private val cameraMonitor = cam.et.dashcamsystem.device.CameraMonitor(context)

    // Location
    fun setLocationListener(listener: cam.et.dashcamsystem.device.LocationMonitor.Listener?) {
        locationMonitor.locationListener = listener
    }

    // Motion
    fun setAccelerometerListener(listener: cam.et.dashcamsystem.device.MotionSensorMonitor.AccelerometerListener?) {
        motionMonitor.accelerometerListener = listener
    }

    fun setGyroscopeListener(listener: cam.et.dashcamsystem.device.MotionSensorMonitor.GyroscopeListener?) {
        motionMonitor.gyroscopeListener = listener
    }

    // Camera
    fun setCameraFrameListener(listener: cam.et.dashcamsystem.device.CameraMonitor.FrameListener?) {
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
