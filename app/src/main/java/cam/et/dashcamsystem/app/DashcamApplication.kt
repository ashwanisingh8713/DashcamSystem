package cam.et.dashcamsystem.app

import android.app.Application
import cam.et.dashcamlog.DashcamLogConfigurator
import cam.et.dashcamlog.DashcamLog

class DashcamApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        DashcamLogConfigurator.checkDefaultConfig()
        DashcamLog.setDebugFromContext(DashcamApplication@this)
    }
}
