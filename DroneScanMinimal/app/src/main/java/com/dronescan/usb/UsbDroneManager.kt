package com.dronescan.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class UsbDroneManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UsbDroneManager"
        private const val ACTION_USB_PERMISSION = "com.dronescan.USB_PERMISSION"
        
        // Identificadores conocidos de DJI RC
        private val DJI_VENDOR_IDS = listOf(0x2ca3, 0x1234) // Agregar IDs reales de DJI
        private val DJI_PRODUCT_IDS = listOf(0x001f, 0x5678) // Agregar IDs reales
        
        // Extensiones de imagen soportadas
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "dng", "raw")
    }
    
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    
    private var isDjiConnected = false
    private var connectedDjiDevice: UsbDevice? = null
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
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let { handleDeviceAttached(it) }
                }
                
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let { handleDeviceDetached(it) }
                }
                
                ACTION_USB_PERMISSION -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { handlePermissionGranted(it) }
                    } else {
                        Log.w(TAG, "Permiso USB denegado para dispositivo: ${device?.deviceName}")
                        onConnectionStatusChanged?.invoke(false, "Permiso USB denegado")
                    }
                }
            }
        }
    }
    
    fun initialize() {
        Log.d(TAG, "Inicializando UsbDroneManager...")
        
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        
        context.registerReceiver(usbReceiver, filter)
        
        // Verificar dispositivos ya conectados
        checkConnectedDevices()
        
        Log.d(TAG, "UsbDroneManager inicializado")
    }
    
    fun cleanup() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error al limpiar receivers", e)
        }
    }
    
    private fun checkConnectedDevices() {
        Log.d(TAG, "Verificando dispositivos USB conectados...")
        
        val deviceList = usbManager.deviceList
        Log.d(TAG, "Dispositivos encontrados: ${deviceList.size}")
        
        for ((_, device) in deviceList) {
            Log.d(TAG, "Dispositivo: ${device.deviceName}, VendorID: 0x${Integer.toHexString(device.vendorId)}, ProductID: 0x${Integer.toHexString(device.productId)}")
            
            if (isDjiDevice(device)) {
                Log.d(TAG, "¡Dispositivo DJI detectado!")
                requestPermissionAndConnect(device)
                return
            }
        }
        
        // Si no encontramos DJI, verificar dispositivos de almacenamiento genéricos
        checkGenericStorageDevices(deviceList)
    }
    
    private fun checkGenericStorageDevices(deviceList: HashMap<String, UsbDevice>) {
        Log.d(TAG, "Buscando dispositivos de almacenamiento genéricos...")
        
        for ((_, device) in deviceList) {
            // Verificar si el dispositivo tiene interfaces de almacenamiento masivo
            for (i in 0 until device.interfaceCount) {
                val usbInterface = device.getInterface(i)
                
                // Clase 8 = Mass Storage, Subclase 6 = SCSI
                if (usbInterface.interfaceClass == 8 && usbInterface.interfaceSubclass == 6) {
                    Log.d(TAG, "Dispositivo de almacenamiento encontrado: ${device.deviceName}")
                    requestPermissionAndConnect(device)
                    return
                }
            }
        }
        
        onConnectionStatusChanged?.invoke(false, "No se encontraron dispositivos compatibles")
    }
    
    private fun isDjiDevice(device: UsbDevice): Boolean {
        return DJI_VENDOR_IDS.contains(device.vendorId) || 
               device.manufacturerName?.contains("DJI", ignoreCase = true) == true ||
               device.productName?.contains("DJI", ignoreCase = true) == true
    }
    
    private fun requestPermissionAndConnect(device: UsbDevice) {
        if (usbManager.hasPermission(device)) {
            handlePermissionGranted(device)
        } else {
            Log.d(TAG, "Solicitando permiso para dispositivo: ${device.deviceName}")
            
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
        Log.d(TAG, "Dispositivo USB conectado: ${device.deviceName}")
        
        if (isDjiDevice(device) || hasStorageInterface(device)) {
            requestPermissionAndConnect(device)
        }
    }
    
    private fun handleDeviceDetached(device: UsbDevice) {
        Log.d(TAG, "Dispositivo USB desconectado: ${device.deviceName}")
        
        if (device == connectedDjiDevice) {
            isDjiConnected = false
            connectedDjiDevice = null
            droneStorageUri = null
            onConnectionStatusChanged?.invoke(false, "Dispositivo desconectado")
        }
    }
    
    private fun handlePermissionGranted(device: UsbDevice) {
        Log.d(TAG, "Permiso USB concedido para: ${device.deviceName}")
        
        connectedDjiDevice = device
        isDjiConnected = true
        
        // Intentar encontrar el almacenamiento del dispositivo
        findDeviceStorage(device)
        
        onConnectionStatusChanged?.invoke(true, "Dispositivo conectado: ${device.deviceName}")
    }
    
    private fun hasStorageInterface(device: UsbDevice): Boolean {
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            if (usbInterface.interfaceClass == 8) { // Mass Storage
                return true
            }
        }
        return false
    }
    
    private fun findDeviceStorage(device: UsbDevice) {
        Log.d(TAG, "Buscando almacenamiento en dispositivo: ${device.deviceName}")
        
        try {
            // Método 1: Usar DocumentsProvider si está disponible
            findStorageViaDocumentsProvider()
            
            // Método 2: Buscar en rutas de almacenamiento conocidas
            if (droneStorageUri == null) {
                findStorageViaFileSystem()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando almacenamiento", e)
            onConnectionStatusChanged?.invoke(false, "Error accediendo al almacenamiento: ${e.message}")
        }
    }
    
    private fun findStorageViaDocumentsProvider() {
        // Intentar acceder vía Storage Access Framework
        Log.d(TAG, "Intentando acceso vía DocumentsProvider...")
        
        // Este método se expandirá cuando implementemos el selector de carpetas
        // Por ahora, marcamos como no encontrado
        Log.d(TAG, "DocumentsProvider: Implementación pendiente")
    }
    
    private fun findStorageViaFileSystem() {
        Log.d(TAG, "Buscando almacenamiento vía sistema de archivos...")
        
        // Buscar en rutas comunes de dispositivos USB
        val commonPaths = listOf(
            "/storage/usb",
            "/storage/usbdisk",
            "/storage/usb1",
            "/storage/usb2",
            "/mnt/usb",
            "/mnt/usb_storage",
            "/mnt/media_rw"
        )
        
        for (path in commonPaths) {
            val dir = File(path)
            if (dir.exists() && dir.isDirectory) {
                Log.d(TAG, "Ruta de almacenamiento encontrada: $path")
                
                // Buscar subcarpetas que contengan imágenes
                findImagesInDirectory(dir)
                return
            }
        }
        
        Log.d(TAG, "No se encontraron rutas de almacenamiento USB estándar")
    }
    
    fun scanForPhotos(): List<File> {
        Log.d(TAG, "Iniciando escaneo de fotos...")
        
        return try {
            val photos = mutableListOf<File>()
            
            // Método 1: Escanear vía sistema de archivos
            val dronePhotos = scanFileSystem()
            photos.addAll(dronePhotos.map { it.file })
            
            // Método 2: Escanear vía MediaStore (para dispositivos montados)
            val mediaPhotos = scanMediaStore()
            photos.addAll(mediaPhotos.map { it.file })
            
            Log.d(TAG, "Escaneo completado: ${photos.size} fotos encontradas")
            photos.distinctBy { it.absolutePath } // Eliminar duplicados
            
        } catch (e: Exception) {
            Log.e(TAG, "Error escaneando fotos", e)
            emptyList()
        }
    }
    
    private fun scanFileSystem(): List<DronePhoto> {
        val photos = mutableListOf<DronePhoto>()
        
        // Buscar en rutas comunes de almacenamiento USB
        val searchPaths = listOf(
            "/storage/",
            "/mnt/",
            "/sdcard/",
            "/storage/emulated/0/"
        )
        
        for (basePath in searchPaths) {
            val baseDir = File(basePath)
            if (baseDir.exists()) {
                findImagesInDirectoryRecursive(baseDir, photos, maxDepth = 3)
            }
        }
        
        return photos
    }
    
    private fun findImagesInDirectory(directory: File) {
        Log.d(TAG, "Buscando imágenes en: ${directory.absolutePath}")
        
        try {
            val files = directory.listFiles() ?: return
            
            for (file in files) {
                if (file.isFile && isImageFile(file)) {
                    Log.d(TAG, "Imagen encontrada: ${file.name}")
                } else if (file.isDirectory) {
                    // Buscar en subdirectorios comunes de drones
                    val dirName = file.name.lowercase()
                    if (dirName.contains("dcim") || dirName.contains("photo") || dirName.contains("image")) {
                        findImagesInDirectory(file)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando en directorio: ${directory.absolutePath}", e)
        }
    }
    
    private fun findImagesInDirectoryRecursive(directory: File, photos: MutableList<DronePhoto>, maxDepth: Int, currentDepth: Int = 0) {
        if (currentDepth > maxDepth) return
        
        try {
            val files = directory.listFiles() ?: return
            
            for (file in files) {
                if (file.isFile && isImageFile(file)) {
                    photos.add(DronePhoto(
                        file = file,
                        uri = Uri.fromFile(file),
                        name = file.name,
                        size = file.length(),
                        lastModified = file.lastModified()
                    ))
                } else if (file.isDirectory && currentDepth < maxDepth) {
                    findImagesInDirectoryRecursive(file, photos, maxDepth, currentDepth + 1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error escaneando directorio: ${directory.absolutePath}", e)
        }
    }
    
    private fun scanMediaStore(): List<DronePhoto> {
        // Implementación para escanear vía MediaStore
        // Se expandirá en futuras versiones
        return emptyList()
    }
    
    private fun isImageFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return IMAGE_EXTENSIONS.contains(extension)
    }
    
    fun getConnectionStatus(): Boolean = isDjiConnected
    
    fun hasConnectedDrone(): Boolean = isDjiConnected
    
    fun getAvailableDevices(): List<UsbDevice> {
        return usbManager.deviceList.values.filter { device ->
            isDjiDevice(device) || hasStorageInterface(device)
        }
    }
    
    fun getConnectedDeviceName(): String? = connectedDjiDevice?.deviceName
    
    fun copyPhotoToLocalStorage(dronePhoto: DronePhoto, destinationDir: File): File? {
        try {
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }
            
            val destinationFile = File(destinationDir, dronePhoto.name)
            
            dronePhoto.file.inputStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            Log.d(TAG, "Foto copiada: ${dronePhoto.name} -> ${destinationFile.absolutePath}")
            return destinationFile
            
        } catch (e: IOException) {
            Log.e(TAG, "Error copiando foto: ${dronePhoto.name}", e)
            return null
        }
    }
}
