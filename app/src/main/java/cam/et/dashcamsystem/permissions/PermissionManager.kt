package cam.et.dashcamsystem.permissions

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult

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

    // Launcher used to open the All files access settings on Android R+.
    private val manageStorageLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(StartActivityForResult()) {
            // After returning from settings, proceed to request runtime permissions
            // (READ/WRITE or scoped media). We re-run permission logic below.
            proceedWithRuntimePermissions()
        }

    private fun proceedWithRuntimePermissions() {
        val perms = requiredPermissions()
        val toRequest = perms.filter {
            ContextCompat.checkSelfPermission(activity, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isEmpty()) {
            callback?.onResult(true, emptyList())
            return
        }

        launcher.launch(toRequest.toTypedArray())
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
        // On Android R+ the app needs MANAGE_EXTERNAL_STORAGE (All files access)
        // if it wants to write to arbitrary locations like /mnt/sdcard.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                // Launch Settings screen that allows the user to grant All files access for this app.
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${activity.packageName}")
                    manageStorageLauncher.launch(intent)
                    return
                } catch (e: Exception) {
                    // Fallback: proceed with runtime perms even if we couldn't open settings
                }
            }
        }

        // If we reach here, either we don't need MANAGE_EXTERNAL_STORAGE or we already have it.
        proceedWithRuntimePermissions()
    }

    /**
     * Quick boolean check: are all required permissions granted?
     */
    fun allPermissionsGranted(): Boolean {
        val basic = requiredPermissions().all {
            ContextCompat.checkSelfPermission(activity, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (!basic) return false

        // On Android R+ also ensure MANAGE_EXTERNAL_STORAGE (all-files access) when needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (!android.os.Environment.isExternalStorageManager()) {
                    return false
                }
            } catch (_: Exception) {
                return false
            }
        }

        return true
    }
}
