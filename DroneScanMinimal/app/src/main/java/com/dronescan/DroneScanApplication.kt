package com.dronescan

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import com.dronescan.debug.DebugLogger

/**
 * DroneScanApplication - Basado en BridgeApplication.java de Android-Bridge-App
 * Implementa inicialización crítica y lifecycle callbacks para detectar USB
 */
class DroneScanApplication : Application(), Application.ActivityLifecycleCallbacks {
    
    companion object {
        private const val TAG = "DroneScanApplication"
        
        @JvmStatic
        var instance: DroneScanApplication? = null
            private set
            
        @JvmStatic
        fun getContext(): Context? = instance
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicialización crítica como BridgeApplication
        DebugLogger.init()
        instance = this
        registerActivityLifecycleCallbacks(this)
        
        DebugLogger.d(TAG, "=== DroneScanApplication onCreate() ===")
        DebugLogger.d(TAG, "✅ DebugLogger inicializado")
        DebugLogger.d(TAG, "✅ ActivityLifecycleCallbacks registrados")
        
        try {
            // Detectar violaciones de threads como Bridge App (solo en debug)
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyDropBox()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyDropBox()
                    .penaltyLog()
                    .build()
            )
            DebugLogger.d(TAG, "✅ StrictMode configurado")
        } catch (e: Exception) {
            DebugLogger.w(TAG, "No se pudo configurar StrictMode")
        }
        
        initializeApp()
    }
    
    private fun initializeApp() {
        try {
            DebugLogger.d(TAG, "DroneScan inicializado correctamente - Modo USB Nativo + Bridge Pattern")
            DebugLogger.d(TAG, "Listo para acceder a dispositivos USB y escanear códigos de barras")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error inicializando aplicación", e)
        }
    }

    //region Activity Lifecycle Callbacks (como BridgeApplication)
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        DebugLogger.v(TAG, "${activity.localClassName} Created")
    }

    override fun onActivityStarted(activity: Activity) {
        DebugLogger.v(TAG, "${activity.localClassName} Started")
    }

    override fun onActivityResumed(activity: Activity) {
        DebugLogger.v(TAG, "${activity.localClassName} Resumed")
    }

    override fun onActivityPaused(activity: Activity) {
        DebugLogger.v(TAG, "${activity.localClassName} Paused")
    }

    override fun onActivityStopped(activity: Activity) {
        DebugLogger.v(TAG, "${activity.localClassName} Stopped")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        DebugLogger.v(TAG, "${activity.localClassName} SaveInstance")
    }

    override fun onActivityDestroyed(activity: Activity) {
        DebugLogger.v(TAG, "${activity.localClassName} Destroyed")
    }
    //endregion
}
