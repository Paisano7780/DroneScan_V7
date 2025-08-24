package com.dronescan.msdksample

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import com.dronescan.msdksample.debug.DebugLogger
import dji.common.error.DJIError
import dji.common.error.DJISDKError  
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import java.util.concurrent.atomic.AtomicBoolean

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
        
        // Flag para evitar registros múltiples
        private val isRegistrationInProgress = AtomicBoolean(false)
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
        startDJISDKRegistration()
    }
    
    /**
     * Inicia el registro DJI SDK - Basado en el patrón del SDK original
     */
    private fun startDJISDKRegistration() {
        try {
            DebugLogger.d(TAG, "=== Iniciando registro DJI SDK ===")
            
            // Verificar disponibilidad del SDK antes de usar
            val sdkManagerClass = Class.forName("dji.sdk.sdkmanager.DJISDKManager")
            DebugLogger.d(TAG, "✅ Clase DJISDKManager encontrada: ${sdkManagerClass.name}")
            
            Thread {
                try {
                    DebugLogger.d(TAG, "🔄 DJI SDK registration TEMPORALMENTE DESHABILITADO")
                    DebugLogger.w(TAG, "⚠️ VerifyError evitado - USB detection funcionará independientemente")
                    DebugLogger.d(TAG, "✅ App iniciada sin DJI SDK - lista para USB testing")
                } catch (e: Exception) {
                    DebugLogger.e(TAG, "❌ Error en thread simplificado: ${e.message}")
                }
            }.start()
            
        } catch (e: ClassNotFoundException) {
            DebugLogger.e(TAG, "❌ Clase DJISDKManager no encontrada: ${e.message}")
        } catch (e: VerifyError) {
            DebugLogger.e(TAG, "❌ VerifyError al acceder a DJISDKManager: ${e.message}")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "❌ Error general al iniciar DJI SDK: ${e.message}")
        }
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
