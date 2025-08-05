package dji.csv;

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Clase base para exportar datos a un archivo CSV.
 */
public class CsvExporter {

    private Context context;

    public CsvExporter(Context context) {
        this.context = context;
    }

    /**
     * Exporta una lista de datos a un archivo CSV.
     * @param data Lista de filas (cada fila es un array de strings).
     * @param fileName Nombre del archivo CSV a crear.
     * @return Archivo CSV generado.
     */
    public File exportToCsv(List<String[]> data, String fileName) throws IOException {
        File csvFile = new File(context.getExternalFilesDir(null), fileName);
        FileWriter writer = new FileWriter(csvFile);
        for (String[] row : data) {
            writer.append(String.join(",", row));
            writer.append("\n");
        }
        writer.flush();
        writer.close();
        return csvFile;
    }
}