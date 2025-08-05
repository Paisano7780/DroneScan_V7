package dji.sharing;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;

/**
 * Clase base para compartir archivos (por ejemplo, CSV) usando intents de Android.
 */
public class FileSharingHelper {

    private Context context;

    public FileSharingHelper(Context context) {
        this.context = context;
    }

    /**
     * Comparte un archivo usando un intent de Android.
     * @param file Archivo a compartir.
     */
    public void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        context.startActivity(Intent.createChooser(intent, "Compartir archivo CSV"));
    }
}