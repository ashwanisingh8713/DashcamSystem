package cam.et.dashcamsystem.app.services

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import cam.et.dashcamsystem.app.activities.MainActivity

class ImmortalService : Service() {

    companion object {
        const val CHANNEL_ID = "immortal_service_channel"
        const val URGENT_CHANNEL_ID = "immortal_service_urgent"
        const val NOTIFICATION_ID = 1001
        const val RESTART_ACTION = "cam.et.dashcamsystem.action.RESTART_IMMORTAL_SERVICE"
        const val START_ACTION = "cam.et.dashcamsystem.action.START_IMMORTAL_SERVICE"
        private const val RESTART_DELAY_MS = 5000L
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bringToFront = intent?.getBooleanExtra("bring_to_front", false) == true
        Log.i("ImmortalService", "onStartCommand: bringToFront=$bringToFront, intent=$intent")

        val notification = buildNotification(bringToFront)
        Log.i("ImmortalService", "Starting foreground with notification id=$NOTIFICATION_ID")
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scheduleRestart()
    }

    override fun onDestroy() {
        super.onDestroy()
        scheduleRestart()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        scheduleRestart()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) {
            scheduleRestart()
        }
    }

    private fun scheduleRestart() {
        try {
            val restartIntent = Intent(RESTART_ACTION).setClassName(this, "cam.et.dashcamsystem.app.receivers.RestartReceiver")

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pending = PendingIntent.getBroadcast(
                this,
                0,
                restartIntent,
                flags
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = SystemClock.elapsedRealtime() + RESTART_DELAY_MS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pending)
            }
            Log.i("ImmortalService", "Scheduled restart in ${RESTART_DELAY_MS}ms")
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Immortal Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keep the Dashcam immortal service running"
            }
            nm.createNotificationChannel(channel)

            val urgent = NotificationChannel(
                URGENT_CHANNEL_ID,
                "Immortal Service (urgent)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent channel used to bring the UI to the foreground"
                setShowBadge(false)
            }
            nm.createNotificationChannel(urgent)
            Log.i("ImmortalService", "Created notification channels: $CHANNEL_ID, $URGENT_CHANNEL_ID")
        }
    }

    private fun buildNotification(bringToFront: Boolean = false): Notification {
        val pm = packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        val pendingOpen = if (launchIntent != null) PendingIntent.getActivity(this, 0, launchIntent, flags) else null

        val channel = if (bringToFront) URGENT_CHANNEL_ID else CHANNEL_ID

        val builder = NotificationCompat.Builder(this, channel)
            .setContentTitle("Dashcam Running")
            .setContentText("Immortal background service is active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)

        if (pendingOpen != null) builder.setContentIntent(pendingOpen)

        if (bringToFront) {
            try {
                Log.i("ImmortalService", "Attempting to attach full-screen intent for boot launch")
                val activityIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                val fsPending = PendingIntent.getActivity(this, 0, activityIntent, flags)
                builder.setFullScreenIntent(fsPending, true)
                builder.priority = NotificationCompat.PRIORITY_HIGH
                builder.setCategory(NotificationCompat.CATEGORY_CALL)
                Log.i("ImmortalService", "Full-screen intent attached")
            } catch (ex: Exception) {
                Log.w("ImmortalService", "Failed to attach full-screen intent: ${ex.message}")
            }
        }

        return builder.build()
    }
}