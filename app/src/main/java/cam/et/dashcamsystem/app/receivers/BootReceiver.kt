package cam.et.dashcamsystem.app.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import cam.et.dashcamsystem.app.services.ImmortalService
import cam.et.dashcamsystem.app.DashcamApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.i("BootReceiver", "Received boot broadcast: ${intent.action}")

            // Start the immortal foreground service
            val svcIntent = Intent(context, ImmortalService::class.java).apply {
                action = ImmortalService.START_ACTION
                // Ask the service to bring the UI to front (full-screen notification) when started from boot
                putExtra("bring_to_front", true)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(svcIntent)
                } else {
                    context.startService(svcIntent)
                }
            } catch (_: SecurityException) {
                // If we don't have permission to start foreground service at this time, schedule a retry via AlarmManager
                scheduleServiceRetry(context)
            } catch (_: Throwable) {
                // unexpected failure — schedule a retry as a best-effort
                scheduleServiceRetry(context)
            }
            // Launching the MainActivity in foreground
            DashcamApplication.getInstance().bringToForeground()
        }
    }

    private fun scheduleServiceRetry(context: Context) {
        try {
            val retryIntent = Intent(context, RestartReceiver::class.java).apply {
                action = ImmortalService.RESTART_ACTION
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pending = PendingIntent.getBroadcast(context, 0, retryIntent, flags)
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = SystemClock.elapsedRealtime() + 5000L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pending)
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pending)
            }
        } catch (_: Exception) {
            // swallow — best-effort
        }
    }

}
