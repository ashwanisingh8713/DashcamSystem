package cam.et.dashcamsystem.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.annotation.RequiresPermission

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

    // Allow fallback to NETWORK provider when GPS is unavailable
    private var allowFallbackToNetwork = false

    // Track which provider we registered with so we can remove updates correctly
    private val registeredProviders = mutableListOf<String>()

    private val gpsProvider: String = LocationManager.GPS_PROVIDER
    private val netProvider: String = LocationManager.NETWORK_PROVIDER

    @SuppressLint("MissingPermission")
    private val platformListener: android.location.LocationListener = object : android.location.LocationListener {
        override fun onLocationChanged(location: Location) {
            locationListener?.onLocation(location)
        }

        override fun onProviderDisabled(provider: String) {
            // If GPS became disabled while running and fallback is allowed, try to switch to NETWORK
            if (provider == gpsProvider && allowFallbackToNetwork) {
                try {
                    val netEnabled: Boolean = try { locationManager.isProviderEnabled(netProvider) } catch (_: Exception) { false }
                    if (netEnabled) {
                        // switch subscriptions: remove GPS updates and request NETWORK updates
                        try { locationManager.removeUpdates(platformListener) } catch (_: Exception) {}
                        registeredProviders.clear()
                        try {
                            locationManager.requestLocationUpdates(netProvider, 1000L, 0f, platformListener, Looper.getMainLooper())
                            registeredProviders.add(netProvider)
                            isRunning = true
                            val last: Location? = try { locationManager.getLastKnownLocation(netProvider) } catch (_: Exception) { null }
                            last?.let { locationListener?.onLocation(it) }
                            return
                        } catch (_: Exception) {
                            // fall through to notify disabled
                        }
                    }
                } catch (_: Exception) {
                    // ignore and notify disabled
                }
            }

            // Otherwise notify caller that a provider was disabled
            locationListener?.onProviderDisabled()
        }

        @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        override fun onProviderEnabled(provider: String) {
            // If GPS becomes available and we were using NETWORK, switch back to GPS
            if (provider == gpsProvider) {
                // If currently running and using network, switch to GPS
                if (isRunning && registeredProviders.contains(netProvider)) {
                    try {
                        try { locationManager.removeUpdates(platformListener) } catch (_: Exception) {}
                        registeredProviders.clear()
                        locationManager.requestLocationUpdates(gpsProvider, 1000L, 0f, platformListener, Looper.getMainLooper())
                        registeredProviders.add(gpsProvider)
                        val last: Location? = try { locationManager.getLastKnownLocation(gpsProvider) } catch (_: Exception) { null }
                        last?.let { locationListener?.onLocation(it) }
                    } catch (_: Exception) {
                        // ignore
                    }
                }
                locationListener?.onProviderEnabled()
                return
            }

            locationListener?.onProviderEnabled()
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
            // no-op
        }
    }

    /**
     * Control whether the monitor may fall back to NETWORK provider when GPS is unavailable.
     * Default is false (GPS-only).
     */
    fun setUseFallback(allowFallback: Boolean) {
        allowFallbackToNetwork = allowFallback
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRunning) return

        registeredProviders.clear()

        // Check GPS availability
        val gpsEnabled: Boolean = try {
            locationManager.isProviderEnabled(gpsProvider)
        } catch (_: Exception) {
            false
        }

        if (gpsEnabled) {
            // Register GPS
            try {
                locationManager.requestLocationUpdates(gpsProvider, 1000L, 0f, platformListener, Looper.getMainLooper())
                registeredProviders.add(gpsProvider)
                isRunning = true

                val last: Location? = try { locationManager.getLastKnownLocation(gpsProvider) } catch (_: Exception) { null }
                last?.let { locationListener?.onLocation(it) }
            } catch (_: SecurityException) {
                // Caller must ensure permissions; swallow to avoid crash
            } catch (_: Exception) {
                // unexpected: clean up and signal provider disabled
                try { locationManager.removeUpdates(platformListener) } catch (_: Exception) {}
                registeredProviders.clear()
                isRunning = false
                locationListener?.onProviderDisabled()
            }
            return
        }

        // GPS not enabled. If fallback is allowed, try network provider.
        if (allowFallbackToNetwork) {
            val netEnabled: Boolean = try {
                locationManager.isProviderEnabled(netProvider)
            } catch (_: Exception) {
                false
            }

            if (netEnabled) {
                try {
                    locationManager.requestLocationUpdates(netProvider, 1000L, 0f, platformListener, Looper.getMainLooper())
                    registeredProviders.add(netProvider)
                    isRunning = true

                    val last: Location? = try { locationManager.getLastKnownLocation(netProvider) } catch (_: Exception) { null }
                    last?.let { locationListener?.onLocation(it) }
                } catch (_: SecurityException) {
                    // ignore
                } catch (_: Exception) {
                    try { locationManager.removeUpdates(platformListener) } catch (_: Exception) {}
                    registeredProviders.clear()
                    isRunning = false
                    locationListener?.onProviderDisabled()
                }
                return
            }
        }

        // Neither GPS nor (allowed) network provider available
        locationListener?.onProviderDisabled()
    }

    fun stop() {
        if (!isRunning) return
        try {
            locationManager.removeUpdates(platformListener)
        } catch (_: Exception) {
            // ignore
        }
        registeredProviders.clear()
        isRunning = false
    }
}
