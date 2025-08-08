package dji.barcode

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast

class BarcodeScanActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No UI, solo procesamiento automático
        val imagePath = intent.getStringExtra("image_path")
        var scanSuccess = false
        if (imagePath != null) {
            val imgFile = java.io.File(imagePath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                val processor = BarcodeProcessor()
                val results = processor.processImage(bitmap)
                if (!results.isNullOrEmpty()) {
                    scanSuccess = true
                    Toast.makeText(this, "Escaneo exitoso", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Escaneo fallido", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val result = Intent().apply {
            putExtra("scan_success", scanSuccess)
            putExtra("image_path", imagePath)
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}
