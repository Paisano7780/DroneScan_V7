package dji.sampleV5.aircraft;

import android.app.Activity;
import android.os.Bundle;
import dji.ptp.PtpPhotoManager;
import dji.barcode.BarcodeProcessor;
import dji.csv.CsvExporter;
import dji.sharing.FileSharingHelper;

public class DroneScanActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drone_scan); // <-- enlaza el layout aquí
        
    }
}