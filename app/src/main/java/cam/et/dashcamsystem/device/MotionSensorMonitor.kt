package cam.et.dashcamsystem.device

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Monitors accelerometer and gyroscope sensors. Simple listeners surface raw sensor values.
 * Start/stop control registers and unregisters listeners with the SensorManager.
 */
class MotionSensorMonitor(context: Context) {

    interface AccelerometerListener {
        fun onAccelerometer(x: Float, y: Float, z: Float, timestampNs: Long)
    }

    interface GyroscopeListener {
        fun onGyroscope(x: Float, y: Float, z: Float, timestampNs: Long)
    }

    var accelerometerListener: AccelerometerListener? = null
    var gyroscopeListener: GyroscopeListener? = null

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var isRunning = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    if (event.values.size >= 3) {
                        accelerometerListener?.onAccelerometer(event.values[0], event.values[1], event.values[2], event.timestamp)
                    }
                }
                Sensor.TYPE_GYROSCOPE -> {
                    if (event.values.size >= 3) {
                        gyroscopeListener?.onGyroscope(event.values[0], event.values[1], event.values[2], event.timestamp)
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // no-op
        }
    }

    fun start() {
        if (isRunning) return
        accel?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME) }
        gyro?.let { sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_GAME) }
        isRunning = true
    }

    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(sensorListener)
        isRunning = false
    }
}
