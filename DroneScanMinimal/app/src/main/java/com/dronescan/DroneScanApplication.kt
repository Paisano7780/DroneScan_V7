package com.dronescan

import android.app.Application
import android.content.Context
import android.util.Log

class DroneScanApplication : Application() {
    
    companion object {
        private const val TAG = "DroneScanApp"
        
        @JvmStatic
        var instance: DroneScanApplication? = null
            private set
            
        @JvmStatic
        fun getContext(): Context? = instance
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "Inicializando aplicación DroneScan con acceso USB nativo")
        initializeApp()
    }
    
    private fun initializeApp() {
        try {
            Log.d(TAG, "DroneScan inicializado correctamente - Modo USB Nativo")
            Log.d(TAG, "Listo para acceder a dispositivos USB y escanear códigos de barras")
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando aplicación: ${e.message}", e)
        }
    }
}
