package com.dronescan.msdksample.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dronescan.msdksample.debug.DebugLogger
import com.dronescan.msdksample.ptp.PtpPhotoManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class UsbDroneManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UsbDroneManager"
        private const val ACTION_USB_PERMISSION = "com.dronescan.USB_PERMISSION"
        
        // Modelos DJI específicos para UsbAccessory
        enum class UsbModel(private val value: String) {
            AG("AG410"),
            WM160("WM160"),
            LOGIC_LINK("com.dji.logiclink"),
            RM330("RM330"),
            DJI_RC("DJI RC"),
            UNKNOWN("Unknown");

            fun getModel(): String = value
        }
    }
    
    // Callbacks públicos
    var onConnectionStatusChanged: ((Boolean, String) -> Unit)? = null
    var onPhotoDetected: ((String) -> Unit)? = null
    
    // Variables privadas
    private val usbManager: UsbManager by lazy { 
        context.getSystemService(Context.USB_SERVICE) as UsbManager 
    }
    private var isDjiConnected = false
    private var currentModel = UsbModel.UNKNOWN
    private var monitoringDisposable: Disposable? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                    DebugLogger.d(TAG, "🔌 USB Accessory conectado")
                    checkForDJIAccessory()
                }
                UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                    DebugLogger.d(TAG, "🔌 USB Accessory desconectado")
                    handleDeviceDisconnected()
                }
                ACTION_USB_PERMISSION -> {
                    val accessory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY, UsbAccessory::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
                    }
                    
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        DebugLogger.d(TAG, "✅ Permisos USB otorgados")
                        accessory?.let { handlePermissionGranted(it) }
                    } else {
                        DebugLogger.w(TAG, "❌ Permisos USB denegados")
                    }
                }
            }
        }
    }

    fun initialize() {
        DebugLogger.d(TAG, "🚀 Inicializando UsbDroneManager")
        
        // Registrar receiver USB
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
        
        // Verificar si ya hay un accessory conectado
        checkForDJIAccessory()
        
        // Iniciar monitoreo periódico
        startPeriodicMonitoring()
    }

    private fun startPeriodicMonitoring() {
        monitoringDisposable = Observable.interval(2, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                checkForDJIAccessory()
            }, { error ->
                DebugLogger.e(TAG, "Error en monitoreo periódico: ${error.message}")
            })
    }

    private fun checkForDJIAccessory() {
        try {
            val accessories = usbManager.accessoryList
            DebugLogger.v(TAG, "⏰ Timer tick: ejecutando checkForDJIAccessory()")
            
            if (accessories?.isNotEmpty() == true) {
                for (accessory in accessories) {
                    val model = detectUsbModel(accessory)
                    if (model != UsbModel.UNKNOWN) {
                        if (!isDjiConnected || currentModel != model) {
                            DebugLogger.d(TAG, "🎯 DJI Accessory detectado: ${model.getModel()}")
                            handleDJIAccessoryDetected(accessory, model)
                        } else {
                            DebugLogger.d(TAG, "🛡️ ${model.getModel()} ya detectado - evitar redundancia")
                            if (model == UsbModel.RM330) {
                                DebugLogger.d(TAG, "💡 Manteniendo conexión ${model.getModel()} Host Port existente")
                            }
                        }
                        return
                    }
                }
            }
            
            if (isDjiConnected) {
                handleDeviceDisconnected()
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error verificando USB accessories: ${e.message}")
        }
    }

    private fun detectUsbModel(accessory: UsbAccessory): UsbModel {
        val description = accessory.description ?: ""
        val manufacturer = accessory.manufacturer ?: ""
        val model = accessory.model ?: ""
        
        DebugLogger.d(TAG, "📋 USB Accessory - Manufacturer: $manufacturer, Model: $model, Description: $description")
        
        return when {
            model.contains("RM330", ignoreCase = true) -> UsbModel.RM330
            model.contains("DJI RC", ignoreCase = true) -> UsbModel.DJI_RC
            model.contains("WM160", ignoreCase = true) -> UsbModel.WM160
            model.contains("AG410", ignoreCase = true) -> UsbModel.AG
            description.contains("com.dji.logiclink", ignoreCase = true) -> UsbModel.LOGIC_LINK
            manufacturer.contains("DJI", ignoreCase = true) -> UsbModel.DJI_RC
            else -> UsbModel.UNKNOWN
        }
    }

    private fun handleDJIAccessoryDetected(accessory: UsbAccessory, model: UsbModel) {
        currentModel = model
        
        if (usbManager.hasPermission(accessory)) {
            DebugLogger.d(TAG, "✅ Ya tiene permisos para ${model.getModel()}")
            handlePermissionGranted(accessory)
        } else {
            DebugLogger.d(TAG, "🔐 Solicitando permisos para ${model.getModel()}")
            requestUsbPermission(accessory)
        }
    }

    private fun requestUsbPermission(accessory: UsbAccessory) {
        val permissionIntent = PendingIntent.getBroadcast(
            context, 
            0, 
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        usbManager.requestPermission(accessory, permissionIntent)
    }

    private fun handlePermissionGranted(accessory: UsbAccessory) {
        isDjiConnected = true
        val modelName = currentModel.getModel()
        
        DebugLogger.d(TAG, "🚀 Conexión establecida con $modelName")
        onConnectionStatusChanged?.invoke(true, "✅ $modelName conectado")
        
        // Inicializar MediaManager para acceso a fotos del drone
        initializeDJIConnection()
    }

    private fun initializeDJIConnection() {
        try {
            DebugLogger.d(TAG, "🔗 Iniciando conexión DJI SDK...")
            
            // Obtener instancia singleton de PtpPhotoManager
            val ptpManager = PtpPhotoManager.getInstance()
            
            // Inicializar MediaManager para acceso a fotos del drone
            ptpManager.initialize(context) { success, errorMessage ->
                if (success) {
                    DebugLogger.d(TAG, "✅ DJI MediaManager inicializado correctamente")
                    
                    // Configurar callback de fotos detectadas
                    ptpManager.onPhotoDetected = { photoPath ->
                        onPhotoDetected?.invoke(photoPath)
                    }
                    
                    DebugLogger.d(TAG, "🎯 MediaManager listo para escaneo de fotos")
                } else {
                    DebugLogger.e(TAG, "❌ Error inicializando DJI MediaManager: $errorMessage")
                }
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error inicializando DJI SDK: ${e.message}")
        }
    }

    private fun handleDeviceDisconnected() {
        if (isDjiConnected) {
            isDjiConnected = false
            currentModel = UsbModel.UNKNOWN
            
            DebugLogger.d(TAG, "🔌 Dispositivo DJI desconectado")
            onConnectionStatusChanged?.invoke(false, "❌ Dispositivo desconectado")
            
            DebugLogger.d(TAG, "🔌 Limpiando conexión MediaManager")
        }
    }

    fun scanForPhotos() {
        DebugLogger.d(TAG, "📸 Iniciando escaneo de fotos vía MediaManager...")
        
        if (!isDjiConnected) {
            DebugLogger.w(TAG, "⚠️ No hay dispositivo DJI conectado para escanear fotos")
            onConnectionStatusChanged?.invoke(false, "❌ No hay dispositivo DJI conectado")
            return
        }
        
        // Usar el método disponible que escanea fotos
        PtpPhotoManager.getInstance().scanAndDownloadAllPhotos()
    }

    fun cleanup() {
        DebugLogger.d(TAG, "🧹 Limpiando UsbDroneManager")
        
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            DebugLogger.w(TAG, "Error unregistering receiver: ${e.message}")
        }
        
        monitoringDisposable?.dispose()
        PtpPhotoManager.getInstance().cleanup()
        
        isDjiConnected = false
        currentModel = UsbModel.UNKNOWN
    }
}
