package com.dronescan.msdksample.barcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.oned.Code128Reader
import com.google.zxing.oned.Code39Reader
import com.google.zxing.oned.EAN13Reader
import com.google.zxing.oned.UPCAReader
import java.io.File

/**
 * Procesador completo de códigos QR y de barras usando ZXing.
 * Soporta QR, Code128, Code39, EAN13, UPCA y otros formatos.
 */
class BarcodeProcessor(private val context: Context) {
    
    private val readers = listOf(
        QRCodeReader(),
        Code128Reader(),
        Code39Reader(),
        EAN13Reader(),
        UPCAReader()
    )

    /**
     * Procesa un archivo de imagen y devuelve los datos decodificados de los códigos encontrados.
     * @param imagePath Ruta del archivo de imagen a analizar.
     * @return Lista de strings con los datos de los códigos detectados.
     */
    fun processImage(imagePath: String): List<String> {
        val file = File(imagePath)
        if (!file.exists()) {
            return emptyList()
        }
        
        val bitmap = BitmapFactory.decodeFile(imagePath)
        return if (bitmap != null) {
            processImage(bitmap)
        } else {
            emptyList()
        }
    }

    /**
     * Procesa una imagen y devuelve los datos decodificados de los códigos encontrados.
     * @param bitmap Imagen a analizar.
     * @return Lista de strings con los datos de los códigos detectados.
     */
    fun processImage(bitmap: Bitmap): List<String> {
        val results = mutableListOf<String>()
        
        try {
            // Convertir bitmap a formato ZXing
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            
            // Intentar con cada tipo de lector
            for (reader in readers) {
                try {
                    val result = reader.decode(binaryBitmap)
                    results.add(result.text)
                } catch (e: Exception) {
                    // Continuar con el siguiente lector
                }
            }
            
            // Si no se encontró nada con lectores específicos, usar MultiFormatReader
            if (results.isEmpty()) {
                try {
                    val multiReader = MultiFormatReader()
                    val result = multiReader.decode(binaryBitmap)
                    results.add(result.text)
                } catch (e: Exception) {
                    // No se encontró ningún código
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return results.distinct() // Eliminar duplicados
    }
}
