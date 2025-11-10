package cam.et.dashcamsystem.device

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager

/**
 * Simple Location monitor using Android LocationManager.
 * Caller is responsible for ensuring location permissions are granted before calling start().
 */
class LocationMonitor(context: Context) {

    interface Listener {
        fun onLocation(location: Location)
        fun onProviderDisabled()
        fun onProviderEnabled()
    }

    var locationListener: Listener? = null
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var isRunning = false

    private val platformListener = object : android.location.LocationListener {
        override fun onLocationChanged(location: Location) {
            locationListener?.onLocation(location)
        }

        override fun onProviderDisabled(provider: String) {
            locationListener?.onProviderDisabled()
        }

        override fun onProviderEnabled(provider: String) {
            locationListener?.onProviderEnabled()
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
            // no-op
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return
        // This will throw SecurityException if permissions are missing; caller must ensure permissions.
        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        provider?.let {
            // request updates (use overload without Looper to match SDK variants)
            locationManager.requestLocationUpdates(it, 1000L, 0f, platformListener)
            isRunning = true
            // Try to send last known location immediately
            val last = locationManager.getLastKnownLocation(it)
            last?.let { loc -> locationListener?.onLocation(loc) }
        }
    }

    fun stop() {
        if (!isRunning) return
        locationManager.removeUpdates(platformListener)
        isRunning = false
    }
}
