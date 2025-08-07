package dji.barcode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;
import java.io.File;
import java.util.List;

public class BarcodeScanActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No UI, solo procesamiento automático
        Intent intent = getIntent();
        String imagePath = intent.getStringExtra("image_path");
        boolean scanSuccess = false;
        if (imagePath != null) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                BarcodeProcessor processor = new BarcodeProcessor();
                List<String> results = processor.processImage(bitmap);
                if (results != null && !results.isEmpty()) {
                    scanSuccess = true;
                    Toast.makeText(this, "Escaneo exitoso", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Escaneo fallido", Toast.LENGTH_SHORT).show();
                }
            }
        }
        Intent result = new Intent();
        result.putExtra("scan_success", scanSuccess);
        result.putExtra("image_path", imagePath);
        setResult(Activity.RESULT_OK, result);
        finish();
    }
}
