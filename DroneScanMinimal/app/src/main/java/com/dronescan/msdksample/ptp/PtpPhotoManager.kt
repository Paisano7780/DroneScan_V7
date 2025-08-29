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
 * Manager para acceso a fotos del drone via DJI MediaManager (OFICIAL v2.5)
 * Basado en documentación oficial: MediaManager + fetchFileData() para archivos completos
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
    
    private var camera: Camera? = null
    private var mediaManager: MediaManager? = null
    private var downloadedPhotosDir: File? = null
    private var isInitialized = false
    
    // Callbacks públicos
    var onPhotoDetected: ((String) -> Unit)? = null
    var onPhotoDownloaded: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    /**
     * Inicializar MediaManager con la cámara del drone conectado
     */
    fun initialize(context: Context, callback: (Boolean, String?) -> Unit) {
        DebugLogger.d(TAG, "🚀 Inicializando PtpPhotoManager v2.5 con DJI MediaManager oficial")
        
        try {
            // Crear directorio para fotos descargadas
            val documentsDir = File(context.getExternalFilesDir(null), "DroneScan")
            downloadedPhotosDir = File(documentsDir, "Photos")
            if (!downloadedPhotosDir!!.exists()) {
                downloadedPhotosDir!!.mkdirs()
                DebugLogger.d(TAG, "📁 Directorio creado: ${downloadedPhotosDir!!.absolutePath}")
            }
            
            // Obtener la cámara del drone conectado
            val product = DJISDKManager.getInstance().product
            if (product !is Aircraft) {
                val error = "❌ Producto no es Aircraft"
                DebugLogger.e(TAG, error)
                callback(false, error)
                return
            }
            
            camera = product.camera
            if (camera == null) {
                val error = "❌ Cámara no encontrada"
                DebugLogger.e(TAG, error)
                callback(false, error)
                return
            }
            
            // Inicializar MediaManager
            initializeMediaManager(camera!!, callback)
            
        } catch (e: Exception) {
            val error = "Error inicializando PtpPhotoManager: ${e.message}"
            DebugLogger.e(TAG, error)
            callback(false, error)
        }
    }
    
    private fun initializeMediaManager(camera: Camera, callback: (Boolean, String?) -> Unit) {
        DebugLogger.d(TAG, "📱 Configurando MediaManager oficial")
        
        // Cambiar cámara a modo MEDIA_DOWNLOAD
        camera.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, object : CommonCallbacks.CompletionCallback<DJIError> {
            override fun onResult(error: DJIError?) {
                if (error == null) {
                    DebugLogger.d(TAG, "✅ Cámara cambiada a modo MEDIA_DOWNLOAD")
                    
                    // Obtener MediaManager
                    mediaManager = camera.mediaManager
                    if (mediaManager != null) {
                        isInitialized = true
                        DebugLogger.d(TAG, "✅ MediaManager inicializado correctamente")
                        callback(true, null)
                    } else {
                        val errorMsg = "❌ MediaManager no disponible"
                        DebugLogger.e(TAG, errorMsg)
                        callback(false, errorMsg)
                    }
                } else {
                    val errorMsg = "❌ Error cambiando a modo MEDIA_DOWNLOAD: ${error.description}"
                    DebugLogger.e(TAG, errorMsg)
                    callback(false, errorMsg)
                }
            }
        })
    }
    
    /**
     * Obtener todas las fotos usando MediaManager oficial
     */
    fun getAllPhotos() {
        if (!isInitialized || mediaManager == null) {
            DebugLogger.e(TAG, "❌ MediaManager no inicializado")
            onError?.invoke("MediaManager no inicializado")
            return
        }
        
        DebugLogger.d(TAG, "📸 Actualizando lista de archivos de SD...")
        
        // Primero actualizar la lista de archivos (método oficial confirmado)
        mediaManager!!.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, object : CommonCallbacks.CompletionCallback<DJIError> {
            override fun onResult(error: DJIError?) {
                if (error == null) {
                    DebugLogger.d(TAG, "✅ Lista de archivos actualizada")
                    getFileListFromSD()
                } else {
                    val errorMsg = "❌ Error actualizando lista de archivos: ${error.description}"
                    DebugLogger.e(TAG, errorMsg)
                    onError?.invoke(errorMsg)
                }
            }
        })
    }
    
    private fun getFileListFromSD() {
        // Obtener la lista de archivos de la SD (método oficial confirmado)
        val mediaFiles = mediaManager!!.sdCardFileListSnapshot
        
        if (mediaFiles.isNullOrEmpty()) {
            DebugLogger.w(TAG, "⚠️ No se encontraron archivos en la SD")
            return
        }
        
        DebugLogger.d(TAG, "� Encontrados ${mediaFiles.size} archivos en SD")
        
        // Procesar cada archivo media
        for (mediaFile in mediaFiles) {
            DebugLogger.d(TAG, "📷 Procesando: ${mediaFile.fileName}")
            
            // Filtrar solo fotos (JPEG, DNG) - usar MediaType oficial
            if (mediaFile.mediaType == MediaFile.MediaType.JPEG || 
                mediaFile.mediaType == MediaFile.MediaType.RAW_DNG) {
                
                onPhotoDetected?.invoke(mediaFile.fileName)
                downloadPhotoFile(mediaFile)
            }
        }
    }
    
    /**
     * Descargar archivo de foto usando fetchFileData (método oficial para archivos completos)
     */
    private fun downloadPhotoFile(mediaFile: MediaFile) {
        if (downloadedPhotosDir == null) {
            DebugLogger.e(TAG, "❌ Directorio de descarga no configurado")
            return
        }
        
        val destinationFile = File(downloadedPhotosDir, mediaFile.fileName)
        
        DebugLogger.d(TAG, "📥 Descargando archivo completo: ${mediaFile.fileName}")
        
        // Usar DownloadListener oficial según documentación
        val downloadListener = object : DownloadListener<String> {
            override fun onStart() {
                DebugLogger.d(TAG, "� Iniciando descarga de ${mediaFile.fileName}")
            }
            
            override fun onRateUpdate(total: Long, current: Long, persize: Long) {
                val progress = if (total > 0) (current * 100 / total) else 0
                DebugLogger.v(TAG, "📊 Velocidad ${mediaFile.fileName}: $progress%")
            }
            
            override fun onProgress(total: Long, current: Long) {
                val progress = if (total > 0) (current * 100 / total) else 0
                DebugLogger.d(TAG, "📈 Progreso ${mediaFile.fileName}: $progress%")
            }
            
            override fun onRealtimeDataUpdate(data: ByteArray?, offset: Long, isComplete: Boolean) {
                // Datos en tiempo real durante la descarga (opcional)
                if (isComplete) {
                    DebugLogger.d(TAG, "📦 Datos completos recibidos para ${mediaFile.fileName}")
                }
            }
            
            override fun onSuccess(filePath: String?) {
                DebugLogger.d(TAG, "✅ Descarga exitosa: ${mediaFile.fileName}")
                if (filePath != null) {
                    val file = File(filePath)
                    if (file.exists()) {
                        onPhotoDownloaded?.invoke(file)
                    } else {
                        DebugLogger.e(TAG, "❌ Archivo descargado no existe: $filePath")
                    }
                }
            }
            
            override fun onFailure(error: DJIError) {
                DebugLogger.e(TAG, "❌ Error descargando ${mediaFile.fileName}: ${error.description}")
                onError?.invoke("Error descargando ${mediaFile.fileName}: ${error.description}")
            }
        }
        
        // Usar fetchFileData para archivos completos (método oficial confirmado)
        mediaFile.fetchFileData(destinationFile, null, downloadListener)
    }
    
    /**
     * Escanear y descargar todas las fotos automáticamente
     */
    fun scanAndDownloadAllPhotos() {
        DebugLogger.d(TAG, "🔍 Iniciando escaneo y descarga automática de fotos v2.5...")
        getAllPhotos()
    }
    
    fun cleanup() {
        DebugLogger.d(TAG, "🧹 Limpiando PtpPhotoManager v2.5")
        
        if (camera != null && isInitialized) {
            // Salir del modo MEDIA_DOWNLOAD y volver a SHOOT_PHOTO
            camera!!.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, object : CommonCallbacks.CompletionCallback<DJIError> {
                override fun onResult(error: DJIError?) {
                    if (error == null) {
                        DebugLogger.d(TAG, "✅ Cámara vuelta a modo SHOOT_PHOTO")
                    } else {
                        DebugLogger.w(TAG, "⚠️ Error volviendo a modo SHOOT_PHOTO: ${error.description}")
                    }
                }
            })
        }
        
        isInitialized = false
        camera = null
        mediaManager = null
    }
}
