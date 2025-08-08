package dji.sampleV5.aircraft

import android.Manifest
import android.app.Activity
import android.content.*
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dji.barcode.BarcodeProcessor
import dji.csv.CsvExporter
import dji.ptp.PtpPhotoManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.MediaFileDownloadListener
import dji.v5.manager.datacenter.media.MediaFileListDataSource
import dji.v5.manager.datacenter.media.MediaFileListState
import dji.v5.manager.datacenter.media.MediaFileListStateListener
import dji.v5.manager.datacenter.media.MediaManager
import dji.v5.manager.datacenter.media.PullMediaFileListParam
import dji.sdk.keyvalue.value.camera.MediaFileType
import dji.sdk.keyvalue.value.camera.CameraStorageLocation
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.v5.manager.key.KeyManager
import dji.sdk.keyvalue.key.camera.CameraKey
import dji.sdk.keyvalue.value.camera.GeneratedMediaFileInfo
import java.io.File

class DroneScanActivity : Activity() {
    private val REQUEST_STORAGE_PERMISSION = 1001
    private val REQUEST_MANAGE_ALL_FILES = 1002
    private var resultTextView: TextView? = null
    private lateinit var ptpPhotoManager: PtpPhotoManager

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
                resultTextView?.text = "Dispositivo USB conectado. Escaneando fotos..."
                scanAndShowPhotos()
            }
        }
    }

    private val mediaManager: MediaManager? by lazy {
        val manager = MediaDataCenter.getInstance().mediaManager
        if (manager is MediaManager) manager else null
    }
    private var mediaFileListStateListener: MediaFileListStateListener? = null
    private var downloadListener: MediaFileDownloadListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drone_scan)
        resultTextView = findViewById(R.id.resultTextView)
        ptpPhotoManager = PtpPhotoManager(this)
        val filter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        registerReceiver(usbReceiver, filter)
        checkAndRequestStoragePermission()
        setupMediaManager()
        setupKeyManagerListener()
    }

    private fun setupMediaManager() {
        // Configurar la fuente de datos (SDCARD y cámara principal por defecto)

        val source = MediaFileListDataSource.Builder()
            .setLocation(CameraStorageLocation.SDCARD)
            .setIndexType(ComponentIndexType.LEFT_OR_MAIN)
            .build()
    mediaManager?.setMediaFileDataSource(source)

        // Habilitar el modo de gestión de archivos
        mediaManager?.enable(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                resultTextView?.text = "MediaManager habilitado"
                mediaFileListStateListener = object : MediaFileListStateListener {
                    override fun onUpdate(state: MediaFileListState) {
                        if (state == MediaFileListState.UP_TO_DATE) {
                            pullAndDownloadLatestPhoto()
                        }
                    }
                }
                mediaManager?.addMediaFileListStateListener(mediaFileListStateListener!!)
                val param = PullMediaFileListParam.Builder().build()
                mediaManager?.pullMediaFileListFromCamera(param, null)
            }
            override fun onFailure(error: IDJIError) {
                resultTextView?.text = "Fallo al habilitar MediaManager: ${error.description()}"
            }
        })
    }

    // Ya no se usará pullAndDownloadLatestPhoto, la lógica será por KeyManager

    private fun setupKeyManagerListener() {
        // Escuchar el último archivo multimedia generado por la cámara
        val key = CameraKey.create(CameraKey.KeyNewlyGeneratedMediaFile)
        KeyManager.getInstance().addListener(key) { value ->
            if (value is GeneratedMediaFileInfo) {
                // Solo descargar si es una foto JPEG
                if (value.fileType == MediaFileType.JPEG) {
                    resultTextView?.text = "Nueva foto detectada: ${value.fileName}, descargando..."
                    // Crear un objeto MediaFile temporal para usar el método de descarga
                    val mediaFile = MediaFile(
                        value.fileName,
                        value.fileType,
                        value.fileSize,
                        value.createTime,
                        value.storageLocation,
                        value.index,
                        value.relativePath
                    )
                    downloadLatestPhoto(mediaFile)
                }
            }
        }
    }

    private fun downloadLatestPhoto(photo: MediaFile) {
        downloadListener = object : MediaFileDownloadListener {
            override fun onStart() {
                resultTextView?.text = "Iniciando descarga..."
            }
            override fun onProgress(total: Long, current: Long) {
                val progress = if (total > 0) (current * 100 / total).toInt() else 0
                resultTextView?.text = "Descargando: $progress%"
            }
            override fun onSuccess(file: File) {
                resultTextView?.text = "Descarga exitosa: ${file.absolutePath}"
                val scanIntent = Intent(this@DroneScanActivity, dji.barcode.BarcodeScanActivity::class.java)
                scanIntent.putExtra("image_path", file.absolutePath)
                startActivityForResult(scanIntent, 2001)
            }
            override fun onFailure(error: IDJIError) {
                resultTextView?.text = "Descarga fallida de la foto: ${error.description()}"
            }
            override fun onFinish() {}
            override fun onRealtimeDataUpdate(data: ByteArray?, position: Long) {
                // Implementación vacía, requerida por la interfaz
            }
        }
        photo.pullOriginalMediaFileFromCamera(0, downloadListener)
    }

    private fun checkAndRequestStoragePermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_IMAGES), REQUEST_STORAGE_PERMISSION)
                } else {
                    scanAndShowPhotos()
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (!android.os.Environment.isExternalStorageManager()) {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES)
                } else {
                    scanAndShowPhotos()
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
                } else {
                    scanAndShowPhotos()
                }
            }
            else -> {
                scanAndShowPhotos()
            }
        }
    }

    private fun scanAndShowPhotos() {
        val photos: List<Uri>? = ptpPhotoManager.scanForNewPhotos()
        resultTextView?.text = if (!photos.isNullOrEmpty()) {
            "Fotos encontradas: ${photos.size}"
        } else {
            "No se encontraron fotos nuevas"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanAndShowPhotos()
            } else {
                resultTextView?.text = "Permiso de almacenamiento denegado"
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MANAGE_ALL_FILES) {
            if (android.os.Environment.isExternalStorageManager()) {
                scanAndShowPhotos()
            } else {
                resultTextView?.text = "Permiso de acceso a todos los archivos denegado"
            }
        } else if (requestCode == 2001) { // Resultado del escaneo de código de barras
            if (data != null && data.hasExtra("scan_success")) {
                val scanSuccess = data.getBooleanExtra("scan_success", false)
                val imagePath = data.getStringExtra("image_path")
                if (scanSuccess) {
                    resultTextView?.text = "Escaneo exitoso"
                    val csvIntent = Intent(this@DroneScanActivity, CsvExporter::class.java)
                    csvIntent.putExtra("image_path", imagePath)
                    startActivity(csvIntent)
                } else {
                    resultTextView?.text = "Escaneo fallido, no se generará CSV"
                }
            } else {
                resultTextView?.text = "Escaneo fallido, no se generará CSV"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
        if (mediaFileListStateListener != null) {
            mediaManager?.removeMediaFileListStateListener(mediaFileListStateListener!!)
        }
        mediaManager?.disable(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {}
            override fun onFailure(error: IDJIError) {}
        })
    }
}
