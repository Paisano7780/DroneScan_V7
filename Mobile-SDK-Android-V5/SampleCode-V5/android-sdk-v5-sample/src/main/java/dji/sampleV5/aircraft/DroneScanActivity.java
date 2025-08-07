
package dji.sampleV5.aircraft;

import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.datacenter.media.MediaFileType;
import dji.v5.manager.datacenter.media.MediaFileListDataSource;
import dji.sdk.keyvalue.value.camera.CameraStorageLocation;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.common.callback.CommonCallbacks;

import dji.v5.manager.datacenter.media.MediaFile;
import dji.v5.manager.datacenter.media.MediaFileListState;
import dji.v5.manager.datacenter.media.MediaFileListStateListener;
import dji.v5.manager.datacenter.media.MediaFileListData;
import dji.v5.manager.datacenter.media.PullMediaFileListParam;
import dji.v5.manager.datacenter.media.MediaFileDownloadListener;
import dji.v5.common.error.IDJIError;
import android.net.Uri;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.TextView;
import dji.ptp.PtpPhotoManager;
import dji.barcode.BarcodeProcessor;
import dji.csv.CsvExporter;
import dji.sharing.FileSharingHelper;

public class DroneScanActivity extends Activity {
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int REQUEST_MANAGE_ALL_FILES = 1002;
    private TextView resultTextView;
    private PtpPhotoManager ptpPhotoManager;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                if (resultTextView != null) {
                    resultTextView.setText("Dispositivo USB conectado. Escaneando fotos...");
                }
                scanAndShowPhotos();
            }
        }
    };

    private IMediaManager mediaManager;
    private MediaFileListStateListener mediaFileListStateListener;
    private MediaFileDownloadListener downloadListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drone_scan);
        resultTextView = findViewById(R.id.resultTextView);
        ptpPhotoManager = new PtpPhotoManager(this);
        // Registrar receiver para detectar conexión USB
        IntentFilter filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        registerReceiver(usbReceiver, filter);
        checkAndRequestStoragePermission();
        setupMediaManager();
    }

    private void setupMediaManager() {
        mediaManager = MediaDataCenter.getInstance().getMediaManager();
        if (mediaManager == null) {
            if (resultTextView != null) resultTextView.setText("MediaManager no disponible");
            return;
        }
        // Configurar la fuente de datos (SDCARD y cámara principal por defecto)
        MediaFileListDataSource source = new MediaFileListDataSource.Builder()
                .setLocation(CameraStorageLocation.SDCARD)
                .setIndexType(ComponentIndexType.LEFT_OR_MAIN)
                .build();
        mediaManager.setMediaFileDataSource(source);

        // Habilitar el modo de gestión de archivos
        mediaManager.enable(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                if (resultTextView != null) resultTextView.setText("MediaManager habilitado");
                // Listener para cambios en la lista de archivos
                mediaFileListStateListener = new MediaFileListStateListener() {
                    @Override
                    public void onUpdate(MediaFileListState state) {
                        if (state == MediaFileListState.UP_TO_DATE) {
                            pullAndDownloadLatestPhoto();
                        }
                    }
                };
                mediaManager.addMediaFileListStateListener(mediaFileListStateListener);
                // Usar el builder para el parámetro
                PullMediaFileListParam param = new PullMediaFileListParam.Builder().build();
                mediaManager.pullMediaFileListFromCamera(param, null);
            }
            @Override
            public void onFailure(IDJIError error) {
                if (resultTextView != null) resultTextView.setText("Fallo al habilitar MediaManager: " + error.description());
            }
        });
    }

    private void pullAndDownloadLatestPhoto() {
        MediaFileListData listData = mediaManager.getMediaFileListData();
        List<MediaFile> mediaFiles = (listData != null) ? listData.getData() : null;
        if (mediaFiles == null || mediaFiles.isEmpty()) {
            if (resultTextView != null) resultTextView.setText("No hay fotos en la SD");
            return;
        }
        // Filtrar solo fotos JPEG usando getFileType() == MediaFileType.JPEG
        List<MediaFile> photoFiles = new java.util.ArrayList<>();
        for (MediaFile mf : mediaFiles) {
            if (mf.getFileType() == MediaFileType.JPEG) {
                photoFiles.add(mf);
            }
        }
        if (photoFiles.isEmpty()) {
            if (resultTextView != null) resultTextView.setText("No se encontró foto nueva");
            return;
        }
        // Ordenar por fecha de creación descendente (más nueva primero)
        Collections.sort(photoFiles, new Comparator<MediaFile>() {
            @Override
            public int compare(MediaFile o1, MediaFile o2) {
                long t1 = o1.getCreateTime();
                long t2 = o2.getCreateTime();
                return Long.compare(t2, t1);
            }
        });
        MediaFile latestPhoto = photoFiles.get(0);
        if (latestPhoto == null) {
            if (resultTextView != null) resultTextView.setText("No se encontró foto nueva");
            return;
        }
        if (resultTextView != null) resultTextView.setText("Descargando: " + latestPhoto.getFileName());
        downloadLatestPhoto(latestPhoto);
    }

    private void downloadLatestPhoto(MediaFile photo) {
        downloadListener = new MediaFileDownloadListener() {
            @Override
            public void onStart() {
                // Opcional: mostrar inicio
            }
            @Override
            public void onProgress(long total, long current) {
                int progress = (total > 0) ? (int) (current * 100 / total) : 0;
                if (resultTextView != null) resultTextView.setText("Descargando: " + progress + "%");
            }
            @Override
            public void onSuccess(@NonNull File file) {
                if (resultTextView != null) resultTextView.setText("Descarga exitosa: " + file.getAbsolutePath());
                // Lanzar actividad de escaneo de código de barras con la foto descargada
                Intent scanIntent = new Intent(DroneScanActivity.this, dji.barcode.BarcodeScanActivity.class);
                scanIntent.putExtra("image_path", file.getAbsolutePath());
                // Usar startActivityForResult para saber cuándo termina el escaneo
                startActivityForResult(scanIntent, 2001);
            }
            @Override
            public void onFailure(@NonNull IDJIError error) {
                if (resultTextView != null) resultTextView.setText("Descarga fallida de la foto: " + error.description());
            }
            @Override
            public void onFinish() {
                // Requerido por la interfaz, puede dejarse vacío
            }
        };
        // Usar offset 0 para descargar desde el inicio
        photo.pullOriginalMediaFileFromCamera(0, downloadListener);
    }

    private void checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_STORAGE_PERMISSION);
            } else {
                scanAndShowPhotos();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11/12
            if (!android.os.Environment.isExternalStorageManager()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES);
            } else {
                scanAndShowPhotos();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_ALL_FILES) {
            if (android.os.Environment.isExternalStorageManager()) {
                scanAndShowPhotos();
            } else {
                if (resultTextView != null) {
                    resultTextView.setText("Permiso de acceso a todos los archivos denegado");
                }
            }
        } else if (requestCode == 2001) { // Resultado del escaneo de código de barras
            // Mostrar mensaje según resultado del escaneo y solo generar CSV si fue exitoso
            if (data != null && data.hasExtra("scan_success")) {
                boolean scanSuccess = data.getBooleanExtra("scan_success", false);
                String imagePath = data.getStringExtra("image_path");
                if (scanSuccess) {
                    if (resultTextView != null) resultTextView.setText("Escaneo exitoso");
                    Intent csvIntent = new Intent(DroneScanActivity.this, CsvExporter.class);
                    csvIntent.putExtra("image_path", imagePath);
                    startActivity(csvIntent);
                } else {
                    if (resultTextView != null) resultTextView.setText("Escaneo fallido, no se generará CSV");
                }
            } else {
                if (resultTextView != null) resultTextView.setText("Escaneo fallido, no se generará CSV");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
        if (mediaManager != null && mediaFileListStateListener != null) {
            mediaManager.removeMediaFileListStateListener(mediaFileListStateListener);
        }
        // Deshabilitar el modo de gestión de archivos
        if (mediaManager != null) {
            mediaManager.disable(null);
        }
    }
}