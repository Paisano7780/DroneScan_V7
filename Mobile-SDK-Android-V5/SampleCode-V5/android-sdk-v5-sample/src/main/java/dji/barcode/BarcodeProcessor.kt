package dji.barcode

import android.graphics.Bitmap

/**
 * Clase base para procesar imágenes y detectar códigos QR o de barras.
 * Aquí deberás integrar una librería como ZXing para la detección real.
 */
class BarcodeProcessor {
    /**
     * Procesa una imagen y devuelve los datos decodificados de los códigos encontrados.
     * @param bitmap Imagen a analizar.
     * @return Lista de strings con los datos de los códigos detectados.
     */
    fun processImage(bitmap: Bitmap): List<String> {
        // TODO: Integrar ZXing o similar para decodificar códigos
        return emptyList()
    }
}
