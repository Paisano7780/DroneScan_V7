package com.dronescan.msdksample.csv

import android.content.Context
import android.os.Environment
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Exportador de datos de códigos escaneados a CSV.
 */
class CsvExporter(private val context: Context) {
    
    companion object {
        private const val CSV_FILENAME = "DroneScan_Codes.csv"
    }
    
    // Lista de resultados acumulados
    private val scanResults = mutableListOf<ScanResult>()
    
    data class ScanResult(
        val photoName: String,
        val barcodes: List<String>,
        val timestamp: Date = Date()
    )
    
    /**
     * Añadir resultado de escaneo para ser incluido en el CSV.
     */
    fun addScanResult(photoName: String, barcodes: List<String>) {
        scanResults.add(ScanResult(photoName, barcodes))
    }    /**
     * Exporta todos los resultados acumulados a CSV.
     */
    @Throws(IOException::class)
    fun exportToCsv(): File {
        // Crear directorio DroneScan en Documents
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val droneScanDir = File(documentsDir, "DroneScan")
        if (!droneScanDir.exists()) {
            droneScanDir.mkdirs()
        }
        
        val csvFile = File(droneScanDir, CSV_FILENAME)
        val isNewFile = !csvFile.exists()
        
        CSVWriter(FileWriter(csvFile, true)).use { writer ->
            // Escribir encabezados si es archivo nuevo
            if (isNewFile) {
                writer.writeNext(arrayOf(
                    "Timestamp",
                    "Photo_Name", 
                    "Code_Type",
                    "Code_Data",
                    "Format",
                    "Latitude",
                    "Longitude",
                    "Altitude"
                ))
            }
            
            // Escribir cada resultado
            scanResults.forEach { result ->
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(result.timestamp)
                
                result.barcodes.forEach { barcode ->
                    writer.writeNext(arrayOf(
                        timestamp,
                        result.photoName,
                        detectCodeType(barcode),
                        barcode,
                        "UNKNOWN", // Tipo de formato no disponible con este approach
                        "0.0", // TODO: Obtener coordenadas GPS del drone
                        "0.0", 
                        "0.0"
                    ))
                }
            }
        }
        
        return csvFile
    }
    
    /**
     * Exporta códigos escaneados a un archivo CSV.
     * @param codes Lista de códigos encontrados.
     * @param photos Lista de archivos de fotos procesadas.
     * @return Archivo CSV generado.
     */
    @Throws(IOException::class)
    fun exportBarcodes(codes: List<String>, photos: List<File>): File {
        // Crear directorio DroneScan en Documents
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val droneScanDir = File(documentsDir, "DroneScan")
        if (!droneScanDir.exists()) {
            droneScanDir.mkdirs()
        }
        
        val csvFile = File(droneScanDir, CSV_FILENAME)
        val isNewFile = !csvFile.exists()
        
        CSVWriter(FileWriter(csvFile, true)).use { writer ->
            // Escribir encabezados si es archivo nuevo
            if (isNewFile) {
                writer.writeNext(arrayOf(
                    "Timestamp",
                    "Image_Path", 
                    "Code_Type",
                    "Code_Data",
                    "Latitude",
                    "Longitude",
                    "Altitude"
                ))
            }
            
            // Timestamp actual
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            
            // Escribir cada código encontrado
            codes.forEachIndexed { index, code ->
                val imagePath = if (index < photos.size) photos[index].absolutePath else "Unknown"
                writer.writeNext(arrayOf(
                    timestamp,
                    imagePath,
                    detectCodeType(code),
                    code,
                    "0.0", // TODO: Obtener coordenadas GPS del drone
                    "0.0", 
                    "0.0"
                ))
            }
        }
        
        return csvFile
    }
    
    /**
     * Detecta el tipo de código basado en su contenido.
     */
    private fun detectCodeType(code: String): String {
        return when {
            code.startsWith("http") -> "QR_URL"
            code.contains("@") -> "QR_EMAIL"
            code.matches(Regex("\\d+")) -> "BARCODE_NUMERIC"
            code.length == 13 && code.matches(Regex("\\d+")) -> "EAN13"
            code.length == 12 && code.matches(Regex("\\d+")) -> "UPC"
            else -> "QR_TEXT"
        }
    }
}
