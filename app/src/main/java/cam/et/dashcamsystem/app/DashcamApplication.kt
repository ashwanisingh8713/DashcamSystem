package cam.et.dashcamsystem.app

import android.app.Application
import android.content.Intent
import android.os.Build
import cam.et.dashcamsystem.MainActivity
import cam.et.dashcamsystem.app.services.BackgroundCameraService
import cam.et.dashcamsystem.app.services.ImmortalService
import cam.et.dashcamsystem.logger.DashcamLogConfigurator
import cam.et.dashcamsystem.logger.DashcamLog
import cam.et.dashcamsystem.device.FilePathManager

class DashcamApplication : Application() {

    companion object {
        private lateinit var sInstance: DashcamApplication
        fun getInstance(): DashcamApplication {
            return sInstance
        }
    }

    fun bringToForeground() {
        val intent = Intent(getInstance(), MainActivity::class.java).apply {
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
    }

    override fun onCreate() {
        super.onCreate()
        sInstance = this
        // Initialise FilePath Manager
        FilePathManager.init(this)

        DashcamLogConfigurator.checkDefaultConfig()
        DashcamLog.setDebugFromContext(this)

        startImmortalService()
        startBackgroundCameraService()


    }


    private fun startImmortalService() {
        try {
            val svcIntent = Intent(this, ImmortalService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(svcIntent)
            } else {
                startService(svcIntent)
            }
        } catch (t: Throwable) {
            // ignore failures here; startup is best-effort
            t.printStackTrace()
        }
    }

    private fun startBackgroundCameraService() {
        try {
            val bsvcIntent = Intent(this, BackgroundCameraService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(bsvcIntent)
            } else {
                startService(bsvcIntent)
            }
        } catch (t: Throwable) {
            // ignore failures here; startup is best-effort
            t.printStackTrace()
        }
    }
}
