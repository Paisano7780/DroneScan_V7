package com.dronescan

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import com.dronescan.debug.DebugLogger
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
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            DebugLogger.d(TAG, "=== Iniciando registro DJI SDK ===")
            
            Thread {
                try {
                    DJISDKManager.getInstance().registerApp(applicationContext, object : DJISDKManager.SDKManagerCallback {
                        override fun onRegister(error: DJIError?) {
                            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                                DebugLogger.d(TAG, "✅ DJI SDK registrado exitosamente")
                                DJISDKManager.getInstance().startConnectionToProduct()
                                DebugLogger.d(TAG, "🔄 Iniciando conexión a producto DJI...")
                            } else {
                                DebugLogger.e(TAG, "❌ Error registrando DJI SDK: ${error?.description}")
                            }
                            isRegistrationInProgress.set(false)
                        }
                        
                        override fun onProductDisconnect() {
                            DebugLogger.d(TAG, "🔌 Producto DJI desconectado")
                        }
                        
                        override fun onProductConnect(product: BaseProduct?) {
                            DebugLogger.d(TAG, "🔌 Producto DJI conectado: ${product?.model}")
                        }
                        
                        override fun onProductChanged(product: BaseProduct?) {
                            DebugLogger.d(TAG, "🔄 Producto DJI cambiado: ${product?.model}")
                        }
                        
                        override fun onComponentChange(
                            key: BaseProduct.ComponentKey?,
                            oldComponent: BaseComponent?,
                            newComponent: BaseComponent?
                        ) {
                            DebugLogger.v(TAG, "🔧 Componente DJI cambió: $key")
                        }
                        
                        override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                            DebugLogger.v(TAG, "⚙️ DJI SDK inicializando: $totalProcess%")
                        }
                        
                        override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                            val progress = if (total > 0) (100 * current / total).toInt() else 0
                            if (progress % 25 == 0 || progress == 100) {
                                DebugLogger.d(TAG, "📦 Descargando BD DJI: $progress%")
                            }
                        }
                    })
                } catch (e: Exception) {
                    DebugLogger.e(TAG, "❌ Excepción registrando DJI SDK", e)
                    isRegistrationInProgress.set(false)
                }
            }.start()
        } else {
            DebugLogger.w(TAG, "⚠️ Registro DJI SDK ya en progreso")
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
