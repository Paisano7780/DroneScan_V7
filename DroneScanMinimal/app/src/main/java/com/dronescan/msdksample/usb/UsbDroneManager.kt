package com.dronescan.msdksample.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.text.TextUtils
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.dronescan.msdksample.debug.DebugLogger
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Method

class UsbDroneManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UsbDroneManager"
        private const val ACTION_USB_PERMISSION = "com.dronescan.msdksample.USB_PERMISSION"
        
        // Modelos DJI específicos para UsbAccessory (basado en Bridge App)
        enum class UsbModel(private val value: String) {
            /**
             * 231之前的整机
             */
            AG("AG410"),
            WM160("WM160"),
            /**
             * 新增的逻辑链路
             */
            LOGIC_LINK("com.dji.logiclink"),
            RM330("RM330"),
            DJI_RC("DJI RC"),
            UNKNOWN("Unknown");

            fun getModel(): String = value

            companion object {
                fun find(modelName: String?): UsbModel {
                    if (modelName == null) return UNKNOWN
                    return values().find { 
                        it.value.equals(modelName, ignoreCase = true) ||
                        modelName.contains(it.value, ignoreCase = true)
                    } ?: UNKNOWN
                }
            }
        }
        
        // IDs reales de DJI RC (basado en investigación de la comunidad)
        private val DJI_VENDOR_IDS = listOf(
            0x2ca3,  // DJI Technology Co., Ltd.
            0x0489,  // Foxconn (usado por algunos controles DJI)
            0x05c6,  // Qualcomm (usado en algunos RC)
            0x18d1,  // Google (para modo ADB/desarrollo)
            0x1d6b   // Linux Foundation (USB genérico)
        )
        
        // Extensiones de imagen soportadas
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "dng", "raw")
        
        // Timer para verificación automática (como Bridge App)
        private const val CHECK_INTERVAL_MS = 2000L
    }
    
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    
    // Timer handler para verificación automática (como Bridge App)
    private val handler = Handler(Looper.getMainLooper())
    private var checkRunnable: Runnable? = null
    private var isTimerRunning = false
    
    // Observable Timer como Bridge App (crítico)
    private var timerDisposable: Disposable? = null
    
    private var isDjiConnected = false
    private var connectedDjiAccessory: UsbAccessory? = null
    private var currentModel = UsbModel.UNKNOWN
    private var droneStorageUri: Uri? = null
    
    // Listener para eventos de conexión
    var onConnectionStatusChanged: ((Boolean, String) -> Unit)? = null
    var onPhotosFound: ((List<DronePhoto>) -> Unit)? = null
    
    data class DronePhoto(
        val file: File,
        val uri: Uri,
        val name: String,
        val size: Long,
        val lastModified: Long
    )
    
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            DebugLogger.d(TAG, "🎭 BroadcastReceiver onReceive: $action")
            
            when (action) {
                UsbManager.ACTION_USB_ACCESSORY_ATTACHED -> {
                    DebugLogger.d(TAG, "🔌 USB Accessory ATTACHED detectado")
                    val accessory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY, UsbAccessory::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
                    }
                    
                    if (accessory != null) {
                        DebugLogger.d(TAG, "  📱 Accesorio: ${accessory.manufacturer} ${accessory.model}")
                        handleAccessoryAttached(accessory)
                    } else {
                        DebugLogger.w(TAG, "⚠️ USB Accessory ATTACHED pero es null")
                    }
                }
                
                UsbManager.ACTION_USB_ACCESSORY_DETACHED -> {
                    DebugLogger.d(TAG, "🔌 USB Accessory DETACHED detectado")
                    val accessory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY, UsbAccessory::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
                    }
                    
                    if (accessory != null) {
                        DebugLogger.d(TAG, "  📱 Accesorio desconectado: ${accessory.manufacturer} ${accessory.model}")
                    }
                    accessory?.let { handleAccessoryDetached(it) }
                }
                
                "android.hardware.usb.action.USB_STATE" -> {
                    DebugLogger.d(TAG, "🔌 USB STATE cambio detectado")
                    val connected = intent.extras?.getBoolean("connected", false) ?: false
                    val configured = intent.extras?.getBoolean("configured", false) ?: false
                    val hostConnected = intent.extras?.getBoolean("host_connected", false) ?: false
                    
                    DebugLogger.d(TAG, "📋 USB_STATE - connected: $connected, configured: $configured, host: $hostConnected")
                    
                    if (connected) {
                        DebugLogger.d(TAG, "USB_STATE: CONECTADO - verificando accesorios...")
                        checkForDJIAccessory()
                    } else {
                        DebugLogger.d(TAG, "USB_STATE: DESCONECTADO")
                        handleAllAccessoriesDisconnected()
                    }
                }
                
                ACTION_USB_PERMISSION -> {
                    DebugLogger.d(TAG, "🔐 Respuesta de permisos USB recibida")
                    val accessory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY, UsbAccessory::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY)
                    }
                    
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        DebugLogger.d(TAG, "✅ Permisos USB concedidos")
                        if (accessory != null) {
                            handlePermissionGranted(accessory)
                        } else {
                            DebugLogger.w(TAG, "⚠️ Permisos concedidos pero accesorio es null")
                        }
                    } else {
                        DebugLogger.w(TAG, "❌ Permiso USB denegado para accesorio: ${accessory?.model}")
                        onConnectionStatusChanged?.invoke(false, "Permiso USB denegado")
                    }
                }
                
                else -> {
                    DebugLogger.d(TAG, "🎭 Acción USB no manejada: $action")
                }
            }
        }
    }
    
    fun initialize() {
        DebugLogger.d(TAG, "=== INICIALIZANDO UsbDroneManager v2.9 ===")
        DebugLogger.d(TAG, "🔧 Implementando detección USB Device + Accessory")
        
        // 🔍 DIAGNÓSTICO INICIAL DEL SISTEMA
        DebugLogger.d(TAG, "📋 === DIAGNÓSTICO INICIAL DEL SISTEMA ===")
        DebugLogger.d(TAG, "📋 Android API Level: ${Build.VERSION.SDK_INT}")
        DebugLogger.d(TAG, "📋 Celular MODEL: ${Build.MODEL} (${Build.MANUFACTURER})")
        
        // Verificar capacidades USB del sistema
        val packageManager = context.packageManager
        DebugLogger.d(TAG, "📋 USB_HOST feature: ${packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)}")
        DebugLogger.d(TAG, "📋 USB_ACCESSORY feature: ${packageManager.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY)}")
        
        // Verificar UsbManager y modo USB actual
        DebugLogger.d(TAG, "📋 UsbManager disponible: ${usbManager != null}")
        
        // Diagnóstico crítico de modo USB
        checkUSBHostMode()
        
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
            addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
            addAction("android.hardware.usb.action.USB_STATE")
            addAction(ACTION_USB_PERMISSION)
        }
        
        try {
            // Android 14+ requiere especificar RECEIVER_EXPORTED
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
                DebugLogger.d(TAG, "Receiver registrado con RECEIVER_EXPORTED (Android 14+)")
            } else {
                context.registerReceiver(usbReceiver, filter)
                DebugLogger.d(TAG, "Receiver registrado modo legacy")
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "ERROR registrando receiver USB", e)
        }
        
        // Verificar accesorios ya conectados
        checkForDJIAccessory()
        
        // CRÍTICO: Observable Timer automático como Bridge App
        startObservableTimer()
        
        DebugLogger.d(TAG, "=== UsbDroneManager v2.7 inicializado ===")
    }
    
    // Observable Timer como Bridge App (CRÍTICO) - línea 119-152
    private fun startObservableTimer() {
        DebugLogger.d(TAG, "🔄 Iniciando Observable Timer cada 2 segundos (como Bridge App)")
        
        timerDisposable = Observable.timer(2, TimeUnit.SECONDS)
            .observeOn(Schedulers.computation())
            .repeat()
            .subscribe(
                { 
                    DebugLogger.v(TAG, "⏰ Timer tick: ejecutando checkForDJIAccessory()")
                    checkForDJIAccessory()
                },
                { e ->
                    DebugLogger.e(TAG, "❌ Error en Observable Timer", e)
                }
            )
    }
    
    fun forceCheckDevices() {
        DebugLogger.d(TAG, "🔍 VERIFICACIÓN MANUAL FORZADA")
        checkForDJIAccessory()
    }
    
    fun cleanup() {
        try {
            DebugLogger.d(TAG, "🧹 Limpiando UsbDroneManager...")
            
            // Detener Observable Timer
            timerDisposable?.dispose()
            timerDisposable = null
            DebugLogger.d(TAG, "✅ Observable Timer detenido")
            
            // Detener timer legacy
            stopAutoCheckTimer()
            
            context.unregisterReceiver(usbReceiver)
            DebugLogger.d(TAG, "✅ UsbDroneManager limpiado correctamente")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error al limpiar receivers", e)
        }
    }
    
    // Verificación crítica del modo USB Host vs Device
    private fun checkUSBHostMode() {
        try {
            DebugLogger.d(TAG, "📋 === DIAGNÓSTICO CRÍTICO DE MODO USB ===")
            
            // Verificar si el celular está actuando como USB Host o Device
            val defaultUsbFunction = getSystemProperty("sys.usb.config", "none")
            val currentUsbFunction = getSystemProperty("sys.usb.state", "none")
            
            DebugLogger.d(TAG, "📋 sys.usb.config: $defaultUsbFunction")
            DebugLogger.d(TAG, "📋 sys.usb.state: $currentUsbFunction")
            
            // Verificar modo USB debugging
            val usbDebugging = getSystemProperty("sys.usb.debugging", "0")
            DebugLogger.d(TAG, "📋 USB debugging: $usbDebugging")
            
            // Verificar propiedades relacionadas con OTG
            val otgProperty = getSystemProperty("persist.vendor.otg.enable", "unknown")
            DebugLogger.d(TAG, "📋 OTG enable: $otgProperty")
            
            // Diagnóstico del problema según logs
            when {
                currentUsbFunction.contains("mtp") -> {
                    DebugLogger.w(TAG, "⚠️ PROBLEMA: Celular en modo MTP (Media Transfer Protocol)")
                    DebugLogger.w(TAG, "💡 SOLUCIÓN: RM330 ve al celular como dispositivo de almacenamiento")
                    DebugLogger.w(TAG, "💡 NECESARIO: Cambiar a modo USB Host/OTG en celular")
                }
                currentUsbFunction.contains("ptp") -> {
                    DebugLogger.w(TAG, "⚠️ PROBLEMA: Celular en modo PTP (Picture Transfer Protocol)")
                    DebugLogger.w(TAG, "💡 SOLUCIÓN: Similar a MTP, celular es dispositivo")
                }
                currentUsbFunction.contains("charging") -> {
                    DebugLogger.w(TAG, "⚠️ PROBLEMA: Celular en modo CHARGING")
                    DebugLogger.w(TAG, "💡 SOLUCIÓN: Solo carga, no transfiere datos")
                }
                else -> {
                    DebugLogger.d(TAG, "📋 Modo USB: $currentUsbFunction")
                }
            }
            
            DebugLogger.d(TAG, "💡 PARA DETECTAR RM330 NECESITAS:")
            DebugLogger.d(TAG, "💡 1. Celular = USB Host (puede ver dispositivos)")
            DebugLogger.d(TAG, "💡 2. RM330 = USB Device (aparece en deviceList)")
            DebugLogger.d(TAG, "💡 3. Cable OTG o configuración correcta")
            
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error verificando modo USB", e)
        }
    }
    
    // Timer automático para verificación continua (como Bridge App)
    private fun startAutoCheckTimer() {
        if (isTimerRunning) return
        
        DebugLogger.d(TAG, "⏰ Iniciando timer automático cada ${CHECK_INTERVAL_MS}ms")
        isTimerRunning = true
        
        checkRunnable = object : Runnable {
            override fun run() {
                if (isTimerRunning) {
                    checkForDJIAccessory()
                    handler.postDelayed(this, CHECK_INTERVAL_MS)
                }
            }
        }
        
        handler.postDelayed(checkRunnable!!, CHECK_INTERVAL_MS)
    }
    
    private fun stopAutoCheckTimer() {
        if (!isTimerRunning) return
        
        DebugLogger.d(TAG, "⏰ Deteniendo timer automático")
        isTimerRunning = false
        checkRunnable?.let { handler.removeCallbacks(it) }
        checkRunnable = null
    }
    
    // Método para leer propiedades del sistema (como Data2SD)
    private fun getSystemProperty(propName: String, defaultValue: String): String {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
            getMethod.invoke(null, propName, defaultValue) as String
        } catch (e: Exception) {
            DebugLogger.w(TAG, "No se pudo leer propiedad $propName: ${e.message}")
            defaultValue
        }
    }
    
    // checkForDJIAccessory() EXACTO como Android-Bridge-App línea 158-174
    private fun checkForDJIAccessory() {
        try {
            DebugLogger.d(TAG, "🔍 === BRIDGE APP PATTERN - VERIFICACIÓN EXACTA ===")
            
            // PASO 1: LÓGICA EXACTA del Bridge App
            val accessoryList = usbManager.accessoryList
            DebugLogger.d(TAG, "📋 accessoryList: $accessoryList")
            DebugLogger.d(TAG, "📋 accessoryList?.size: ${accessoryList?.size}")
            
            // Bridge App línea 160-174: verificación EXACTA
            if (accessoryList != null 
                && accessoryList.size > 0 
                && !TextUtils.isEmpty(accessoryList[0].manufacturer) 
                && accessoryList[0].manufacturer.equals("DJI")) {
                
                DebugLogger.d(TAG, "✅ *** DJI ACCESSORY DETECTADO - BRIDGE PATTERN! ***")
                DebugLogger.d(TAG, "📱 mAccessory = accessoryList[0]")
                DebugLogger.d(TAG, "📱 Manufacturer: '${accessoryList[0].manufacturer}'")
                DebugLogger.d(TAG, "📱 Model: '${accessoryList[0].model}'")
                DebugLogger.d(TAG, "📱 Description: '${accessoryList[0].description}'")
                DebugLogger.d(TAG, "📱 Serial: '${accessoryList[0].serial}'")
                DebugLogger.d(TAG, "📱 Version: '${accessoryList[0].version}'")
                
                // Bridge App: RCConnectionEvent(true)
                onConnectionStatusChanged?.invoke(true, "DJI ${accessoryList[0].model} detectado")
                
                // Bridge App: Check permission
                if (usbManager.hasPermission(accessoryList[0])) {
                    DebugLogger.d(TAG, "✅ RC CONNECTED")
                } else {
                    DebugLogger.d(TAG, "🔐 NO Permission to USB Accessory")
                    requestAccessoryPermission(accessoryList[0])
                }
                return
            } else {
                // Bridge App: RCConnectionEvent(false) 
                DebugLogger.d(TAG, "❌ RC DISCONNECTED")
                
                // Debugging adicional
                if (accessoryList == null) {
                    DebugLogger.d(TAG, "📋 accessoryList is NULL")
                } else if (accessoryList.isEmpty()) {
                    DebugLogger.d(TAG, "📋 accessoryList is EMPTY")
                } else if (TextUtils.isEmpty(accessoryList[0].manufacturer)) {
                    DebugLogger.d(TAG, "📋 accessoryList[0].manufacturer is EMPTY")
                } else {
                    DebugLogger.d(TAG, "📋 accessoryList[0].manufacturer: '${accessoryList[0].manufacturer}'")
                    DebugLogger.d(TAG, "📋 Expected: 'DJI' (exact match)")
                }
            }
            
            // PASO 2: Verificar USB Devices (CRÍTICO para RM330 en celular)
            val deviceList = usbManager.deviceList
            DebugLogger.d(TAG, "📋 deviceList: $deviceList")
            DebugLogger.d(TAG, "📋 deviceList?.size: ${deviceList?.size}")
            
            if (deviceList != null && deviceList.isNotEmpty()) {
                DebugLogger.d(TAG, "🔌 Dispositivos USB encontrados: ${deviceList.size}")
                
                deviceList.values.forEachIndexed { index, device ->
                    DebugLogger.d(TAG, "🔌 Device #${index + 1}:")
                    DebugLogger.d(TAG, "   📋 DeviceName: ${device.deviceName}")
                    DebugLogger.d(TAG, "   📋 VendorId: ${device.vendorId} (0x${device.vendorId.toString(16)})")
                    DebugLogger.d(TAG, "   📋 ProductId: ${device.productId} (0x${device.productId.toString(16)})")
                    DebugLogger.d(TAG, "   📋 DeviceClass: ${device.deviceClass}")
                    DebugLogger.d(TAG, "   📋 DeviceSubclass: ${device.deviceSubclass}")
                    DebugLogger.d(TAG, "   📋 DeviceProtocol: ${device.deviceProtocol}")
                    DebugLogger.d(TAG, "   📋 ManufacturerName: ${device.manufacturerName}")
                    DebugLogger.d(TAG, "   📋 ProductName: ${device.productName}")
                    DebugLogger.d(TAG, "   📋 SerialNumber: ${device.serialNumber}")
                    
                    // Verificar si es DJI por VendorId, nombres o seriales
                    val isDJIVendor = device.vendorId == 0x2CA3 || // DJI oficial
                                     device.vendorId == 0x0B05 || // ASUS para algunos RCs
                                     device.vendorId == 0x18D1 || // Google para dispositivos Android
                                     device.vendorId == 0x1234 || // Posible genérico
                                     device.vendorId == 0x045E || // Microsoft para algunos devices
                                     device.vendorId == 0x05AC    // Apple para ciertos adaptadores
                    
                    val isDJIName = device.manufacturerName?.contains("DJI", ignoreCase = true) == true ||
                                   device.productName?.contains("DJI", ignoreCase = true) == true ||
                                   device.productName?.contains("RM330", ignoreCase = true) == true ||
                                   device.productName?.contains("RC", ignoreCase = true) == true ||
                                   device.productName?.contains("Remote", ignoreCase = true) == true ||
                                   device.deviceName?.contains("DJI", ignoreCase = true) == true ||
                                   device.deviceName?.contains("RM330", ignoreCase = true) == true ||
                                   device.serialNumber?.contains("DJI", ignoreCase = true) == true
                    
                    DebugLogger.d(TAG, "   🔍 isDJIVendor: $isDJIVendor (VID: 0x${device.vendorId.toString(16)})")
                    DebugLogger.d(TAG, "   🔍 isDJIName: $isDJIName")
                    DebugLogger.d(TAG, "   🔍 hasPermission: ${usbManager.hasPermission(device)}")
                    
                    if (isDJIVendor || isDJIName) {
                        DebugLogger.d(TAG, "✅ DISPOSITIVO DJI DETECTADO!")
                        DebugLogger.d(TAG, "🎯 VendorId: 0x${device.vendorId.toString(16)}")
                        DebugLogger.d(TAG, "🎯 ProductName: ${device.productName}")
                        DebugLogger.d(TAG, "🎯 ManufacturerName: ${device.manufacturerName}")
                        
                        // Notificar conexión
                        onConnectionStatusChanged?.invoke(true, "DJI ${device.productName ?: "Device"} detectado vía USB")
                        
                        // Verificar permisos para el device
                        if (usbManager.hasPermission(device)) {
                            DebugLogger.d(TAG, "✅ RC CONNECTED - Permisos USB Device concedidos")
                        } else {
                            DebugLogger.d(TAG, "🔐 Solicitando permisos para USB Device...")
                            requestDevicePermission(device)
                        }
                        return
                    }
                }
                
                DebugLogger.d(TAG, "⚠️ Dispositivos USB detectados pero NINGUNO es DJI reconocido")
                DebugLogger.d(TAG, "💡 POSIBLE SOLUCIÓN: El RM330 puede usar un VendorID no conocido")
                DebugLogger.d(TAG, "💡 Si ves dispositivos arriba, puede ser el RM330 con VendorID diferente")
            } else {
                DebugLogger.d(TAG, "📋 No hay dispositivos USB")
                DebugLogger.d(TAG, "💡 POSIBLE CAUSA: RM330 no conectado o Android no lo detecta como USB Device")
                DebugLogger.d(TAG, "💡 VERIFICAR: Cable USB, modo del RM330, permisos USB debugging")
            }
            
            // Si llegamos aquí, no hay conexión DJI
            DebugLogger.d(TAG, "❌ RC DISCONNECTED - No hay dispositivos DJI")
            onConnectionStatusChanged?.invoke(false, "No hay dispositivos DJI")
            
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error en checkForDJIAccessory()", e)
        }
    }
    
    private fun requestAccessoryPermission(accessory: UsbAccessory) {
        DebugLogger.d(TAG, "Solicitando permiso para accesorio: ${accessory.model}")
        
        val permissionIntent = PendingIntent.getBroadcast(
            context, 
            0, 
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        usbManager.requestPermission(accessory, permissionIntent)
    }
    
    private fun requestDevicePermission(device: UsbDevice) {
        DebugLogger.d(TAG, "Solicitando permiso para device: ${device.productName}")
        
        val permissionIntent = PendingIntent.getBroadcast(
            context, 
            0, 
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        usbManager.requestPermission(device, permissionIntent)
    }
    
    private fun handleAccessoryAttached(accessory: UsbAccessory) {
        DebugLogger.d(TAG, "🔌 Accesorio USB conectado: ${accessory.model}")
        checkForDJIAccessory()
    }
    
    private fun handleAccessoryDetached(accessory: UsbAccessory) {
        DebugLogger.d(TAG, "🔌 Accesorio USB desconectado: ${accessory.model}")
        
        if (connectedDjiAccessory == accessory) {
            handleAllAccessoriesDisconnected()
        }
    }
    
    private fun handleAllAccessoriesDisconnected() {
        isDjiConnected = false
        connectedDjiAccessory = null
        currentModel = UsbModel.UNKNOWN
        droneStorageUri = null
        onConnectionStatusChanged?.invoke(false, "Accesorio DJI desconectado")
    }
    
    private fun handlePermissionGranted(accessory: UsbAccessory) {
        DebugLogger.d(TAG, "✅ Permiso USB concedido para: ${accessory.model}")
        
        try {
            connectedDjiAccessory = accessory
            isDjiConnected = true
            
            val deviceName = "${accessory.manufacturer} ${accessory.model}"
            onConnectionStatusChanged?.invoke(true, "Accesorio DJI conectado: $deviceName")
            
            // Buscar almacenamiento del dispositivo
            findDeviceStorage(accessory)
            
            DebugLogger.d(TAG, "🎉 Conexión DJI establecida exitosamente!")
            
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error manejando permiso concedido para accesorio", e)
            onConnectionStatusChanged?.invoke(false, "Error conectando accesorio: ${e.message}")
        }
    }
    
    private fun checkConnectedDevices() {
        DebugLogger.d(TAG, "=== VERIFICANDO DISPOSITIVOS USB (FALLBACK) ===")
        
        val deviceList = usbManager.deviceList
        DebugLogger.d(TAG, "Total dispositivos USB encontrados: ${deviceList.size}")
        
        if (deviceList.isEmpty()) {
            DebugLogger.w(TAG, "❌ NO HAY DISPOSITIVOS USB CONECTADOS")
            DebugLogger.w(TAG, "Verifica que:")
            DebugLogger.w(TAG, "1. Control remoto esté conectado físicamente")
            DebugLogger.w(TAG, "2. Cable USB funcione")
            DebugLogger.w(TAG, "3. Modo transferencia de archivos activado")
            onConnectionStatusChanged?.invoke(false, "No hay dispositivos USB conectados")
            return
        }
        
        DebugLogger.d(TAG, "--- LISTADO COMPLETO DE DISPOSITIVOS ---")
        var deviceIndex = 1
        
        for ((devicePath, device) in deviceList) {
            val vendorHex = "0x${Integer.toHexString(device.vendorId).uppercase()}"
            val productHex = "0x${Integer.toHexString(device.productId).uppercase()}"
            
            DebugLogger.d(TAG, "📱 DISPOSITIVO $deviceIndex:")
            DebugLogger.d(TAG, "  Path: $devicePath")
            DebugLogger.d(TAG, "  Nombre: ${device.deviceName}")
            DebugLogger.d(TAG, "  Vendor ID: $vendorHex (${device.vendorId})")
            DebugLogger.d(TAG, "  Product ID: $productHex (${device.productId})")
            DebugLogger.d(TAG, "  Clase: ${device.deviceClass}")
            DebugLogger.d(TAG, "  Subclase: ${device.deviceSubclass}")
            DebugLogger.d(TAG, "  Protocolo: ${device.deviceProtocol}")
            DebugLogger.d(TAG, "  Interfaces: ${device.interfaceCount}")
            DebugLogger.d(TAG, "  Manufacturer: ${device.manufacturerName ?: "N/A"}")
            DebugLogger.d(TAG, "  Product: ${device.productName ?: "N/A"}")
            DebugLogger.d(TAG, "  Serial: ${device.serialNumber ?: "N/A"}")
            DebugLogger.d(TAG, "  Version: ${device.version ?: "N/A"}")
            
            // Verificar cada interfaz
            for (i in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(i)
                DebugLogger.d(TAG, "    Interface $i: Clase=${usbInterface.interfaceClass}, Subclase=${usbInterface.interfaceSubclass}, Protocolo=${usbInterface.interfaceProtocol}")
            }
            
            // Verificar si es DJI
            val isDji = isDjiDevice(device)
            DebugLogger.d(TAG, "  ¿Es DJI?: $isDji")
            
            if (isDji) {
                DebugLogger.d(TAG, "🎯 ¡DISPOSITIVO DJI DETECTADO!")
                requestPermissionAndConnect(device)
                return
            }
            
            DebugLogger.d(TAG, "  ---")
            deviceIndex++
        }
        
        // Si no encontramos DJI, verificar dispositivos de almacenamiento genéricos
        checkGenericStorageDevices(deviceList)
    }
    
    private fun checkGenericStorageDevices(deviceList: HashMap<String, UsbDevice>) {
        DebugLogger.d(TAG, "Buscando dispositivos de almacenamiento genéricos...")
        
        for ((_, device) in deviceList) {
            // Verificar si el dispositivo tiene interfaces de almacenamiento masivo
            for (i in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(i)
                
                // Clase 8 = Mass Storage, Subclase 6 = SCSI
                if (usbInterface.interfaceClass == 8 && usbInterface.interfaceSubclass == 6) {
                    DebugLogger.d(TAG, "Dispositivo de almacenamiento encontrado: ${device.deviceName}")
                    requestPermissionAndConnect(device)
                    return
                }
            }
        }
        
        DebugLogger.w(TAG, "❌ No se encontraron dispositivos compatibles")
        onConnectionStatusChanged?.invoke(false, "No se detectaron dispositivos USB compatibles")
    }
    
    private fun isDjiDevice(device: UsbDevice): Boolean {
        val vendorMatch = DJI_VENDOR_IDS.contains(device.vendorId)
        val manufacturerMatch = device.manufacturerName?.contains("DJI", ignoreCase = true) == true
        val productMatch = device.productName?.contains("DJI", ignoreCase = true) == true
        
        DebugLogger.d(TAG, "🔍 Verificando si es DJI:")
        DebugLogger.d(TAG, "  Vendor ID coincide: $vendorMatch")
        DebugLogger.d(TAG, "  Manufacturer coincide: $manufacturerMatch")
        DebugLogger.d(TAG, "  Product coincide: $productMatch")
        
        val isDji = vendorMatch || manufacturerMatch || productMatch
        
        if (!isDji) {
            DebugLogger.w(TAG, "❌ NO es dispositivo DJI reconocido")
            DebugLogger.w(TAG, "Vendor ID esperados: ${DJI_VENDOR_IDS.map { "0x${Integer.toHexString(it).uppercase()}" }}")
            DebugLogger.w(TAG, "Vendor ID actual: 0x${Integer.toHexString(device.vendorId).uppercase()}")
        }
        
        return isDji
    }
    
    private fun requestPermissionAndConnect(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            handlePermissionGranted(device)
        } else {
            DebugLogger.d(TAG, "Solicitando permiso para dispositivo: ${device.deviceName}")
            
            val permissionIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            usbManager.requestPermission(device, permissionIntent)
        }
    }
    
    private fun handleDeviceAttached(device: UsbDevice) {
        DebugLogger.d(TAG, "Dispositivo USB conectado: ${device.deviceName}")
        checkConnectedDevices()
    }
    
    private fun handleDeviceDetached(device: UsbDevice) {
        DebugLogger.d(TAG, "Dispositivo USB desconectado: ${device.deviceName}")
        
        // Para dispositivos USB legacy, usar la lógica anterior
        handleAllAccessoriesDisconnected()
    }
    
    private fun handlePermissionGranted(device: UsbDevice) {
        DebugLogger.d(TAG, "Permiso USB concedido para: ${device.deviceName}")
        
        try {
            // Para dispositivos USB legacy
            isDjiConnected = true
            
            onConnectionStatusChanged?.invoke(true, "Dispositivo DJI conectado: ${device.productName ?: device.deviceName}")
            
            // Buscar almacenamiento del dispositivo
            findDeviceStorage(device)
            
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error manejando permiso concedido", e)
            onConnectionStatusChanged?.invoke(false, "Error conectando dispositivo: ${e.message}")
        }
    }
    
    private fun findDeviceStorage(accessory: UsbAccessory) {
        DebugLogger.d(TAG, "Buscando almacenamiento en accesorio: ${accessory.model}")
        
        try {
            // Para accesorios USB, intentar localizar su almacenamiento
            DebugLogger.d(TAG, "Accesorio DJI detectado - verificando almacenamiento...")
            
            // Intentar acceso via Documents Provider primero
            if (findStorageViaDocumentsProvider()) {
                return
            }
            
            // Fallback a sistema de archivos tradicional
            findStorageViaFileSystem()
            
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error buscando almacenamiento en accesorio", e)
        }
    }
    
    private fun findDeviceStorage(device: UsbDevice) {
        DebugLogger.d(TAG, "Buscando almacenamiento en dispositivo: ${device.deviceName}")
        
        try {
            // Intentar acceso via Documents Provider primero
            if (findStorageViaDocumentsProvider()) {
                return
            }
            
            // Fallback a sistema de archivos tradicional
            findStorageViaFileSystem()
            
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error buscando almacenamiento", e)
        }
    }
    
    private fun findStorageViaDocumentsProvider(): Boolean {
        DebugLogger.d(TAG, "Intentando acceso via Documents Provider...")
        
        try {
            // Implementar acceso via SAF (Storage Access Framework)
            return false
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error en Documents Provider", e)
            return false
        }
    }
    
    private fun findStorageViaFileSystem(): Boolean {
        DebugLogger.d(TAG, "Intentando acceso via sistema de archivos...")
        
        try {
            // Buscar en rutas típicas de almacenamiento USB
            val usbPaths = listOf(
                "/storage/usbotg",
                "/storage/usb",
                "/mnt/usb",
                "/mnt/media_rw"
            )
            
            for (path in usbPaths) {
                val usbDir = File(path)
                if (usbDir.exists() && usbDir.canRead()) {
                    DebugLogger.d(TAG, "Encontrado almacenamiento USB en: $path")
                    findImagesInDirectory(usbDir)
                    return true
                }
            }
            
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error accediendo sistema de archivos", e)
        }
        
        return false
    }
    
    fun scanForPhotos(): List<DronePhoto> {
        DebugLogger.d(TAG, "Iniciando escaneo de fotos...")
        
        val photos = mutableListOf<DronePhoto>()
        
        try {
            // Método 1: Escanear sistema de archivos
            val fileSystemPhotos = scanFileSystem()
            photos.addAll(fileSystemPhotos)
            
            // Método 2: Escanear MediaStore
            val mediaStorePhotos = scanMediaStore()
            photos.addAll(mediaStorePhotos)
            
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error escaneando fotos", e)
        }
        
        DebugLogger.d(TAG, "Escaneo completado. Fotos encontradas: ${photos.size}")
        return photos
    }
    
    private fun scanFileSystem(): List<DronePhoto> {
        DebugLogger.d(TAG, "Escaneando sistema de archivos...")
        
        val photos = mutableListOf<DronePhoto>()
        val commonPaths = listOf(
            "/storage/emulated/0/DCIM",
            "/storage/emulated/0/Pictures",
            "/sdcard/DCIM",
            "/sdcard/Pictures"
        )
        
        for (path in commonPaths) {
            val dir = File(path)
            if (dir.exists() && dir.canRead()) {
                photos.addAll(findImagesInDirectoryRecursive(dir))
            }
        }
        
        return photos
    }
    
    private fun findImagesInDirectoryRecursive(directory: File): List<DronePhoto> {
        val photos = mutableListOf<DronePhoto>()
        
        try {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    photos.addAll(findImagesInDirectoryRecursive(file))
                } else if (isImageFile(file)) {
                    photos.add(DronePhoto(
                        file = file,
                        uri = Uri.fromFile(file),
                        name = file.name,
                        size = file.length(),
                        lastModified = file.lastModified()
                    ))
                }
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error escaneando directorio: ${directory.path}", e)
        }
        
        return photos
    }
    
    private fun findImagesInDirectory(directory: File) {
        DebugLogger.d(TAG, "Buscando imágenes en: ${directory.path}")
        
        try {
            val images = directory.listFiles { file ->
                file.isFile && isImageFile(file)
            }
            
            images?.let {
                DebugLogger.d(TAG, "Encontradas ${it.size} imágenes en ${directory.path}")
                
                val dronePhotos = it.map { file ->
                    DronePhoto(
                        file = file,
                        uri = Uri.fromFile(file),
                        name = file.name,
                        size = file.length(),
                        lastModified = file.lastModified()
                    )
                }
                
                onPhotosFound?.invoke(dronePhotos)
            }
            
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Error buscando imágenes", e)
        }
    }
    
    private fun scanMediaStore(): List<DronePhoto> {
        DebugLogger.d(TAG, "Escaneando MediaStore...")
        return emptyList() // Implementar si es necesario
    }
    
    private fun isImageFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return IMAGE_EXTENSIONS.contains(extension)
    }
    
    fun hasConnectedDrone(): Boolean = isDjiConnected
    
    fun getConnectedDeviceName(): String {
        return connectedDjiAccessory?.let { "${it.manufacturer} ${it.model}" } ?: "Dispositivo USB"
    }
    
    fun getConnectedModel(): UsbModel = currentModel
    
    fun getAvailableDevices(): List<String> {
        val devices = mutableListOf<String>()
        
        // Agregar accesorios USB
        usbManager.accessoryList?.forEach { accessory ->
            devices.add("${accessory.manufacturer} ${accessory.model}")
        }
        
        // Agregar dispositivos USB
        usbManager.deviceList.values.forEach { device ->
            devices.add("${device.productName ?: device.deviceName} (${Integer.toHexString(device.vendorId)}:${Integer.toHexString(device.productId)})")
        }
        
        return devices
    }
}
