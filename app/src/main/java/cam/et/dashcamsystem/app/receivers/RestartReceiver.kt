package cam.et.dashcamsystem.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import cam.et.dashcamsystem.app.services.ImmortalService

class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        if (intent.action == ImmortalService.Companion.RESTART_ACTION) {
            val svcIntent = Intent(context, ImmortalService::class.java).apply {
                action = ImmortalService.Companion.START_ACTION
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(svcIntent)
            } else {
                context.startService(svcIntent)
            }
        }
    }
}

