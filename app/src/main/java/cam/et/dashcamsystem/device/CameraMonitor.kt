package cam.et.dashcamsystem.device

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface

/**
 * Lightweight Camera2 wrapper that opens the first camera, creates an ImageReader and delivers frames.
 * Caller must ensure CAMERA permission is granted before calling start(). This is intentionally minimal
 * — it provides raw Image objects via the FrameListener to keep integration simple.
 */
class CameraMonitor(private val context: Context) {

    interface FrameListener {
        fun onFrame(image: ImageReader)
    }

    var frameListener: FrameListener? = null

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var isRunning = false

    private fun startBackgroundThread() {
        if (backgroundThread != null) return
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        backgroundThread?.join()
        backgroundThread = null
        backgroundHandler = null
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return
        startBackgroundThread()

        // choose the first BACK-facing camera if possible
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val lens = characteristics.get(CameraCharacteristics.LENS_FACING)
            lens == CameraCharacteristics.LENS_FACING_BACK
        } ?: cameraManager.cameraIdList.firstOrNull()

        cameraId ?: return

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val streamConfig = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val outputSizes = streamConfig?.getOutputSizes(ImageFormat.YUV_420_888) ?: arrayOf(Size(640, 480))
        val chosen = outputSizes.firstOrNull() ?: Size(640, 480)

        imageReader = ImageReader.newInstance(chosen.width, chosen.height, ImageFormat.YUV_420_888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            frameListener?.onFrame(reader)
        }, backgroundHandler)

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                try {
                    val surface: Surface = imageReader!!.surface
                    camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            this@CameraMonitor.session = session
                            val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                            requestBuilder.addTarget(surface)
                            requestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                            session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)
                            isRunning = true
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            // no-op for now
                        }
                    }, backgroundHandler)
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
            }
        }, backgroundHandler)
    }

    fun stop() {
        if (!isRunning) return
        try {
            session?.close()
            cameraDevice?.close()
            imageReader?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            session = null
            cameraDevice = null
            imageReader = null
            stopBackgroundThread()
            isRunning = false
        }
    }
}
