package dji.barcode

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import java.io.File

class BarcodeScanActivity : Activity() {
    
    companion object {
        const val EXTRA_SCAN_RESULTS = "scan_results"
        const val EXTRA_SCAN_SUCCESS = "scan_success"
        const val EXTRA_IMAGE_PATH = "image_path"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        var scanSuccess = false
        var scanResults: List<String> = emptyList()
        
        if (imagePath != null) {
            val imgFile = File(imagePath)
            if (imgFile.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    if (bitmap != null) {
                        val processor = BarcodeProcessor()
                        scanResults = processor.processImage(bitmap)
                        
                        if (scanResults.isNotEmpty()) {
                            scanSuccess = true
                            val codesFound = scanResults.joinToString(", ")
                            Toast.makeText(this, "Códigos encontrados: $codesFound", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "No se encontraron códigos en la imagen", Toast.LENGTH_SHORT).show()
                        }
                        bitmap.recycle()
                    } else {
                        Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al procesar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Archivo de imagen no encontrado", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Ruta de imagen no especificada", Toast.LENGTH_SHORT).show()
        }
        
        // Devolver resultado
        val result = Intent().apply {
            putExtra(EXTRA_SCAN_SUCCESS, scanSuccess)
            putExtra(EXTRA_IMAGE_PATH, imagePath)
            putStringArrayListExtra(EXTRA_SCAN_RESULTS, ArrayList(scanResults))
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}
