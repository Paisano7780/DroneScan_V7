package dji.sampleV5.aircraft

import android.app.Application
import dji.sampleV5.aircraft.util.ErrorLogger

class DroneScanApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Handler global para cualquier excepción no capturada
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            ErrorLogger.log(throwable, "UncaughtException")
            // Relanzar la excepción para que el sistema muestre el crash
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }
    }
}
