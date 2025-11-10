package cam.et.dashcamsystem.app.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import cam.et.dashcamsystem.R
import cam.et.dashcamsystem.nativelib.NativeLib
import kotlinx.coroutines.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.firstOrNull
import kotlin.run

class BackgroundCameraService : Service(), LocationListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var handler: Handler? = null
    private var locationManager: LocationManager? = null
    private var lastLocation: Location? = null

    override fun onCreate() {
        super.onCreate()
        val thread = HandlerThread("CameraBg")
        thread.start()
        handler = Handler(thread.looper)

        createNotificationChannel()
        startForeground(1, createNotification("Service starting"))

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, this)
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission missing")
        }

        openCamera()
        scope.launch {
            captureLoop()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        closeCamera()
        locationManager?.removeUpdates(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("bgcam", "Background Camera", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, "bgcam")
            .setContentTitle("BgCam Service")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun openCamera() {
        val cm = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cm.cameraIdList.firstOrNull() ?: return
            val map = cm.getCameraCharacteristics(cameraId)
            val configs = map.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val size = configs?.getOutputSizes(ImageFormat.JPEG)?.firstOrNull()
            val w = size?.width ?: 640
            val h = size?.height ?: 480
            imageReader = ImageReader.newInstance(w, h, ImageFormat.JPEG, 2)
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                handleImage(image)
                image.close()
            }, handler)

            try {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Camera permission not granted")
                    return
                }
                cm.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        createCaptureSession()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        camera.close()
                        cameraDevice = null
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        camera.close()
                        cameraDevice = null
                    }
                }, handler)
            } catch (e: SecurityException) {
                Log.w(TAG, "openCamera SecurityException: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "openCamera error", e)
        }
    }

    private fun createCaptureSession() {
        val camera = cameraDevice ?: return
        try {
            val target = imageReader!!.surface
            camera.createCaptureSession(listOf(target), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "configure failed")
                }
            }, handler)
        } catch (e: Exception) {
            Log.e(TAG, "createCaptureSession error", e)
        }
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
    }

    private suspend fun captureLoop() {
        while (currentCoroutineContext().isActive) {
            try {
                takePicture()
                delay(30_000L)
            } catch (e: Exception) {
                Log.e(TAG, "captureLoop error", e)
                delay(5_000L)
            }
        }
    }

    private fun takePicture() {
        val camera = cameraDevice ?: run {
            Log.w(TAG, "No camera available for capture")
            return
        }
        val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureBuilder.addTarget(imageReader!!.surface)
        captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            captureSession?.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {}, handler)
            Log.d(TAG, "Requested capture")
        } catch (e: Exception) {
            Log.e(TAG, "takePicture error", e)
        }
    }

    private fun handleImage(image: Image) {
        // Convert JPEG Image to byte[]
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Decode minimal ARGB pixels for luminance check using BitmapFactory
        try {
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val width = bmp.width
            val height = bmp.height
            val pixels = IntArray(width * height)
            bmp.getPixels(pixels, 0, width, 0, 0, width, height)

            val isDark = NativeLib.isImageDark(pixels, width, height, 40)

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "BgCam")
            if (!dir.exists()) dir.mkdirs()
            val filename = "IMG_${timestamp}.jpg"
            val file = File(dir, filename)

            val saved = NativeLib.saveBytesToFile(file.absolutePath, bytes)
            val lat = lastLocation?.latitude ?: 0.0
            val lon = lastLocation?.longitude ?: 0.0
            val logDir = File(getExternalFilesDir(null), "logs")
            if (!logDir.exists()) logDir.mkdirs()
            val logFile = File(logDir, "events.log")
            val line = "${filename},${System.currentTimeMillis()},${lat},${lon}\n"
            NativeLib.appendLog(logFile.absolutePath, line)

            if (!saved) {
                Log.w(TAG, "Failed to save image via native POSIX write")
            }

            if (isDark) {
                // Show an alert via notification
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(2, NotificationCompat.Builder(this, "bgcam").setContentTitle("Low Light Detected").setContentText("Camera capture was too dark: $filename").setSmallIcon(R.mipmap.ic_launcher).build())
            }

        } catch (e: Exception) {
            Log.e(TAG, "handleImage error", e)
        }
    }

    // LocationListener
    override fun onLocationChanged(location: Location) {
        lastLocation = location
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    companion object {
        private const val TAG = "BgCamService"
    }
}
