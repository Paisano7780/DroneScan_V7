package com.dronescan.msdksample.ptp

import android.content.Context
import com.dronescan.msdksample.debug.DebugLogger
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks
import dji.sdk.camera.Camera
import dji.sdk.media.DownloadListener
import dji.sdk.media.MediaFile
import dji.sdk.media.MediaManager
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import java.io.File

/**
 * Manager para acceso a fotos del drone via DJI MediaManager
 * Basado en: https://developer.dji.com/api-reference/android-api/Components/Camera/DJIMediaManager.html
 */
class PtpPhotoManager private constructor() {
    
    companion object {
        private const val TAG = "PtpPhotoManager"
        
        // Singleton instance
        @Volatile
        private var INSTANCE: PtpPhotoManager? = null
        
        fun getInstance(): PtpPhotoManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PtpPhotoManager().also { INSTANCE = it }
            }
        }
    }
    
    private var mediaManager: MediaManager? = null
    private var downloadedPhotosDir: File? = null
    private var isInitialized = false
    
    // Callbacks
    var onPhotoDetected: ((String) -> Unit)? = null
    var onPhotoDownloaded: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    /**
     * Inicializar MediaManager con la cámara del drone conectado
     */
    fun initialize(context: Context, callback: (Boolean, String?) -> Unit) {
        DebugLogger.d(TAG, "🚀 Inicializando PtpPhotoManager con DJI MediaManager")
        
        // Crear directorio para fotos descargadas
        downloadedPhotosDir = File(context.getExternalFilesDir(null), "DroneScan/Photos")
        if (!downloadedPhotosDir!!.exists()) {
            downloadedPhotosDir!!.mkdirs()
        }
        
        // Obtener producto DJI conectado
        val product = DJISDKManager.getInstance().product
        if (product == null) {
            val error = "❌ No hay producto DJI conectado"
            DebugLogger.e(TAG, error)
            callback(false, error)
            return
        }
        
        // Verificar que es un aircraft (drone)
        if (product !is Aircraft) {
            val error = "❌ El producto conectado no es un drone"
            DebugLogger.e(TAG, error)
            callback(false, error)
            return
        }
        
        // Obtener cámara
        val camera = product.camera
        if (camera == null) {
            val error = "❌ No se puede acceder a la cámara del drone"
            DebugLogger.e(TAG, error)
            callback(false, error)
            return
        }
        
        // Configurar modo de cámara para acceso a archivos
        camera.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, object : CommonCallbacks.CompletionCallback<DJIError> {
            override fun onResult(error: DJIError?) {
                if (error == null) {
                    DebugLogger.d(TAG, "✅ Cámara configurada en modo MEDIA_DOWNLOAD")
                    initializeMediaManager(camera, callback)
                } else {
                    val errorMsg = "❌ Error configurando cámara: ${error.description}"
                    DebugLogger.e(TAG, errorMsg)
                    callback(false, errorMsg)
                }
            }
        })
    }
    
    /**
     * Inicializar MediaManager después de configurar la cámara
     */
    private fun initializeMediaManager(camera: Camera, callback: (Boolean, String?) -> Unit) {
        mediaManager = camera.mediaManager
        
        if (mediaManager == null) {
            val error = "❌ No se puede obtener MediaManager de la cámara"
            DebugLogger.e(TAG, error)
            callback(false, error)
            return
        }
        
        DebugLogger.d(TAG, "📂 Inicializando MediaManager...")
        
        // Refrescar lista de archivos en el drone
        mediaManager!!.refreshFileListOfStorageLocation(
            SettingsDefinitions.StorageLocation.SDCARD,
            object : CommonCallbacks.CompletionCallback<DJIError> {
                override fun onResult(error: DJIError?) {
                    if (error == null) {
                        isInitialized = true
                        DebugLogger.d(TAG, "✅ MediaManager inicializado correctamente")
                        callback(true, "MediaManager listo")
                    } else {
                        val errorMsg = "❌ Error refrescando archivos: ${error.description}"
                        DebugLogger.e(TAG, errorMsg)
                        callback(false, errorMsg)
                    }
                }
            }
        )
    }
    
    /**
     * Escanear y obtener todas las fotos del drone
     */
    fun getAllPhotos(callback: (List<MediaFile>) -> Unit) {
        if (!isInitialized || mediaManager == null) {
            DebugLogger.e(TAG, "❌ MediaManager no inicializado")
            onError?.invoke("MediaManager no inicializado")
            return
        }
        
        DebugLogger.d(TAG, "📸 Obteniendo lista de fotos del drone...")
        
        val mediaFileList = mediaManager!!.sdCardFileListSnapshot
        if (mediaFileList == null) {
            DebugLogger.w(TAG, "⚠️ Lista de archivos vacía o no disponible")
            callback(emptyList())
            return
        }
        
        // Filtrar solo fotos (JPEG)
        val photoFiles = mediaFileList.filter { mediaFile ->
            mediaFile.mediaType == MediaFile.MediaType.JPEG
        }
        
        DebugLogger.d(TAG, "📷 Encontradas ${photoFiles.size} fotos en el drone")
        callback(photoFiles)
    }
    
    /**
     * Descargar una foto específica del drone
     */
    fun downloadPhoto(mediaFile: MediaFile, callback: (File?) -> Unit) {
        if (!isInitialized || mediaManager == null || downloadedPhotosDir == null) {
            DebugLogger.e(TAG, "❌ MediaManager no inicializado para descarga")
            callback(null)
            return
        }
        
        val fileName = mediaFile.fileName
        val localFile = File(downloadedPhotosDir, fileName)
        
        DebugLogger.d(TAG, "⬇️ Descargando foto: $fileName")
        
        mediaFile.fetchFileData(localFile, null, object : DownloadListener<String> {
            override fun onStart() {
                DebugLogger.d(TAG, "🔄 Iniciando descarga de $fileName")
            }
            
            override fun onRateUpdate(total: Long, current: Long, persize: Long) {
                val progress = (current * 100 / total).toInt()
                DebugLogger.v(TAG, "📊 Descarga $fileName: $progress%")
            }
            
            override fun onProgress(total: Long, current: Long) {
                // Progreso de descarga
            }
            
            override fun onRealtimeDataUpdate(data: ByteArray?, offset: Long, isComplete: Boolean) {
                // Actualización de datos en tiempo real (requerido por la interfaz)
            }
            
            override fun onSuccess(filePath: String?) {
                DebugLogger.d(TAG, "✅ Foto descargada: $fileName -> $filePath")
                if (localFile.exists()) {
                    onPhotoDownloaded?.invoke(localFile)
                    callback(localFile)
                } else {
                    DebugLogger.e(TAG, "❌ Archivo descargado no encontrado: $filePath")
                    callback(null)
                }
            }
            
            override fun onFailure(error: DJIError) {
                val errorMsg = "❌ Error descargando $fileName: ${error.description}"
                DebugLogger.e(TAG, errorMsg)
                onError?.invoke(errorMsg)
                callback(null)
            }
        })
    }
    
    /**
     * Escanear y descargar todas las fotos nuevas
     */
    fun scanAndDownloadPhotos() {
        DebugLogger.d(TAG, "🔍 Iniciando escaneo y descarga de fotos...")
        
        getAllPhotos { photoFiles ->
            if (photoFiles.isEmpty()) {
                DebugLogger.w(TAG, "⚠️ No se encontraron fotos en el drone")
                return@getAllPhotos
            }
            
            DebugLogger.d(TAG, "📂 Procesando ${photoFiles.size} fotos...")
            
            // Descargar cada foto
            photoFiles.forEach { mediaFile ->
                downloadPhoto(mediaFile) { localFile ->
                    if (localFile != null) {
                        DebugLogger.d(TAG, "✅ Foto lista para análisis: ${localFile.absolutePath}")
                        onPhotoDetected?.invoke(localFile.absolutePath)
                    }
                }
            }
        }
    }
    
    /**
     * Limpiar recursos
     */
    fun cleanup() {
        DebugLogger.d(TAG, "🧹 Limpiando recursos de PtpPhotoManager")
        isInitialized = false
        mediaManager = null
        onPhotoDetected = null
        onPhotoDownloaded = null
        onError = null
    }
}
