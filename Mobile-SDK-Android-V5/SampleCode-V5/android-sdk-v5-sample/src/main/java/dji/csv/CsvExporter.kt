package dji.csv

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Clase base para exportar datos a un archivo CSV.
 */
class CsvExporter(private val context: Context) {
    /**
     * Exporta una lista de datos a un archivo CSV.
     * @param data Lista de filas (cada fila es un array de strings).
     * @param fileName Nombre del archivo CSV a crear.
     * @return Archivo CSV generado.
     */
    @Throws(IOException::class)
    fun exportToCsv(data: List<Array<String>>, fileName: String): File {
        val csvFile = File(context.getExternalFilesDir(null), fileName)
        FileWriter(csvFile).use { writer ->
            for (row in data) {
                writer.append(row.joinToString(","))
                writer.append("\n")
            }
        }
        return csvFile
    }
}
