package dji.csv

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity para exportar datos de códigos escaneados a CSV.
 */
class CsvExporter : Activity() {
    
    companion object {
        const val EXTRA_IMAGE_PATH = "image_path"
        const val EXTRA_SCAN_RESULTS = "scan_results"
        private const val CSV_FILENAME = "DroneScan_Codes.csv"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        val scanResults = intent.getStringArrayListExtra(EXTRA_SCAN_RESULTS) ?: emptyList()
        
        if (scanResults.isNotEmpty()) {
            try {
                val csvFile = exportToCsv(scanResults, imagePath ?: "")
                Toast.makeText(this, "CSV exportado: ${csvFile.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error al exportar CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No hay códigos para exportar", Toast.LENGTH_SHORT).show()
        }
        
        finish()
    }
    
    /**
     * Exporta códigos escaneados a un archivo CSV.
     * @param codes Lista de códigos encontrados.
     * @param imagePath Ruta de la imagen procesada.
     * @return Archivo CSV generado.
     */
    @Throws(IOException::class)
    private fun exportToCsv(codes: List<String>, imagePath: String): File {
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
            codes.forEach { code ->
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
