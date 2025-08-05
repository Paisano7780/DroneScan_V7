package dji.barcode;

import android.graphics.Bitmap;
import java.util.List;
import java.util.ArrayList;

/**
 * Clase base para procesar imágenes y detectar códigos QR o de barras.
 * Aquí deberás integrar una librería como ZXing para la detección real.
 */
public class BarcodeProcessor {

    /**
     * Procesa una imagen y devuelve los datos decodificados de los códigos encontrados.
     * @param bitmap Imagen a analizar.
     * @return Lista de strings con los datos de los códigos detectados.
     */
    public List<String> processImage(Bitmap bitmap) {
        // TODO: Integrar ZXing o similar para decodificar códigos
        return new ArrayList<>();
    }
}