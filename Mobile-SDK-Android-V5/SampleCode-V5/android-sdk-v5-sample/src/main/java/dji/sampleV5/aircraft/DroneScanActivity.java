
package dji.sampleV5.aircraft;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
import android.widget.TextView;
import dji.ptp.PtpPhotoManager;
import dji.barcode.BarcodeProcessor;
import dji.csv.CsvExporter;
import dji.sharing.FileSharingHelper;

public class DroneScanActivity extends Activity {
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private TextView resultTextView;
    private PtpPhotoManager ptpPhotoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drone_scan);
        resultTextView = findViewById(R.id.resultTextView); // Asegúrate de que exista este TextView en tu layout
        ptpPhotoManager = new PtpPhotoManager(this);
        checkAndRequestStoragePermission();
    }

    private void checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                scanAndShowPhotos();
            }
        } else {
            scanAndShowPhotos();
        }
    }

    private void scanAndShowPhotos() {
        // Escanea fotos usando PtpPhotoManager
        java.util.List<Uri> photos = ptpPhotoManager.scanForNewPhotos();
        if (resultTextView != null) {
            if (photos != null && !photos.isEmpty()) {
                resultTextView.setText("Fotos encontradas: " + photos.size());
            } else {
                resultTextView.setText("No se encontraron fotos nuevas");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanAndShowPhotos();
            } else {
                if (resultTextView != null) {
                    resultTextView.setText("Permiso de almacenamiento denegado");
                }
            }
        }
    }
}