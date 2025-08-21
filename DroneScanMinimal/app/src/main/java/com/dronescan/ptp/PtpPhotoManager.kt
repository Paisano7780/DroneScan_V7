package com.dronescan.ptp

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Gestor para la detección y transferencia de fotos vía PTP/MTP desde drones.
 */
class PtpPhotoManager(private val context: Context) {
    
    /**
     * Obtiene fotos del dispositivo conectado (simulación para desarrollo).
     * En una implementación real, esto escaneará el dispositivo USB/PTP.
     * @return Lista de archivos de fotos encontradas.
     */
    fun getPhotosFromDevice(): List<File> {
        val photos = mutableListOf<File>()
        
        try {
            // Por ahora, simulamos obteniendo fotos de la galería local
            // En implementación real, esto escaneará el dispositivo PTP/MTP
            val dcimDir = File(Environment.getExternalStorageDirectory(), "DCIM/Camera")
            if (dcimDir.exists() && dcimDir.isDirectory) {
                val imageFiles = dcimDir.listFiles { file ->
                    file.isFile && (file.name.lowercase().endsWith(".jpg") || 
                                   file.name.lowercase().endsWith(".jpeg") ||
                                   file.name.lowercase().endsWith(".png"))
                }
                imageFiles?.let { photos.addAll(it) }
            }
            
            // También buscar en Downloads por si hay fotos ahí
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir.exists() && downloadsDir.isDirectory) {
                val downloadedImages = downloadsDir.listFiles { file ->
                    file.isFile && (file.name.lowercase().endsWith(".jpg") || 
                                   file.name.lowercase().endsWith(".jpeg") ||
                                   file.name.lowercase().endsWith(".png"))
                }
                downloadedImages?.let { photos.addAll(it) }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return photos
    }
    
    /**
     * Escanea el dispositivo conectado por USB en busca de nuevas fotos.
     * @return Lista de URIs de las fotos encontradas.
     */
    fun scanForNewPhotos(): List<Uri> {
        // TODO: Implementar lógica real de escaneo PTP/MTP
        val photoFiles = getPhotosFromDevice()
        return photoFiles.map { Uri.fromFile(it) }
    }

    /**
     * Transfiere una foto específica al almacenamiento local.
     * @param photoUri URI de la foto a transferir.
     * @return Ruta local del archivo transferido.
     */
    fun transferPhoto(photoUri: Uri): String? {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(photoUri)
            inputStream?.let { stream ->
                // Crear directorio DroneScan en Pictures
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val droneScanDir = File(picturesDir, "DroneScan")
                if (!droneScanDir.exists()) {
                    droneScanDir.mkdirs()
                }
                
                // Crear archivo destino con timestamp
                val timestamp = System.currentTimeMillis()
                val fileName = "drone_photo_$timestamp.jpg"
                val destFile = File(droneScanDir, fileName)
                
                // Copiar archivo
                FileOutputStream(destFile).use { outputStream ->
                    stream.copyTo(outputStream)
                }
                
                return destFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    /**
     * Verifica si hay un dispositivo compatible conectado.
     * @return true si hay un dispositivo PTP/MTP disponible.
     */
    fun isDeviceConnected(): Boolean {
        // TODO: Implementar detección real de dispositivo PTP/MTP
        return true // Simulado para desarrollo
    }
}
