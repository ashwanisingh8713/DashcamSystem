package cam.et.dashcamsystem.permissions

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * PermissionManager centralizes runtime permission requests for the app.
 * Usage:
 * val pm = PermissionManager(activity)
 * pm.requestPermissions(object: PermissionManager.Callback { ... })
 */
class PermissionManager(private val activity: ComponentActivity) {

    interface Callback {
        fun onResult(allGranted: Boolean, denied: List<String>)
    }

    private var callback: Callback? = null

    private val launcher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filter { !it.value }.map { it.key }
        callback?.onResult(denied.isEmpty(), denied)
    }

    /**
     * Returns the list of runtime permissions this app needs on this device.
     */
    private fun requiredPermissions(): List<String> {
        val perms = mutableListOf<String>()
        // Core permissions
        perms.add(Manifest.permission.CAMERA)
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION)

        // Storage / media permissions vary by Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ scoped media
            perms.add(Manifest.permission.READ_MEDIA_IMAGES)
            perms.add(Manifest.permission.READ_MEDIA_VIDEO)
            // Notifications permission (Android 13+)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Legacy read/write storage for older devices
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // Foreground service camera/location granular permissions exist on Android 14+
        if (Build.VERSION.SDK_INT >= 34) {
            perms.add("android.permission.FOREGROUND_SERVICE_CAMERA")
            perms.add("android.permission.FOREGROUND_SERVICE_LOCATION")
        }

        return perms
    }

    /**
     * Checks which of the required permissions are not yet granted and requests them.
     * If everything is already granted, the callback is invoked immediately with allGranted=true.
     */
    fun requestPermissions(cb: Callback) {
        callback = cb
        val perms = requiredPermissions()
        val toRequest = perms.filter {
            ContextCompat.checkSelfPermission(activity, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isEmpty()) {
            cb.onResult(true, emptyList())
            return
        }

        // Launch the activity result permission request
        launcher.launch(toRequest.toTypedArray())
    }

    /**
     * Quick boolean check: are all required permissions granted?
     */
    fun allPermissionsGranted(): Boolean {
        return requiredPermissions().all {
            ContextCompat.checkSelfPermission(activity, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

