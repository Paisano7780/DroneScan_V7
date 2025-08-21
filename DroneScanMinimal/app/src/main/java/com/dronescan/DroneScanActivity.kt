package com.dronescan

import android.Manifest
import android.app.Activity
import android.content.*
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dronescan.barcode.BarcodeProcessor
import com.dronescan.csv.CsvExporter
import com.dronescan.ptp.PtpPhotoManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Crear layout programáticamente
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        val titleTextView = TextView(this).apply {
            text = "DroneScan - Escáner de Códigos QR/Barras"
            textSize = 20f
            setPadding(0, 0, 0, 24)
        }
        
        resultTextView = TextView(this).apply {
            text = "Conecta tu drone DJI Mini 3 vía USB para comenzar el escaneo automático de fotos."
            textSize = 14f
        }
        
        layout.addView(titleTextView)
        layout.addView(resultTextView)
        setContentView(layout)
        
        ptpPhotoManager = PtpPhotoManager(this)
        
        checkPermissions()
        registerUsbReceiver()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_STORAGE_PERMISSION)
        } else {
            checkManageAllFilesPermission()
        }
    }

    private fun checkManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES)
            }
        }
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(usbReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    checkManageAllFilesPermission()
                } else {
                    resultTextView?.text = "Permisos requeridos no concedidos"
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MANAGE_ALL_FILES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (android.os.Environment.isExternalStorageManager()) {
                    resultTextView?.text = "Permisos concedidos. Esperando conexión USB..."
                } else {
                    resultTextView?.text = "Permiso de gestión de archivos requerido"
                }
            }
        }
    }

    private fun scanAndShowPhotos() {
        try {
            val photos = ptpPhotoManager.getPhotosFromDevice()
            if (photos.isNotEmpty()) {
                processPhotos(photos)
            } else {
                resultTextView?.text = "No se encontraron fotos en el dispositivo"
            }
        } catch (e: Exception) {
            resultTextView?.text = "Error al acceder a las fotos: ${e.message}"
        }
    }

    private fun processPhotos(photos: List<File>) {
        val barcodeProcessor = BarcodeProcessor(this)
        val allBarcodes = mutableListOf<String>()
        var processedCount = 0

        for (photo in photos) {
            try {
                val barcodes = barcodeProcessor.processImage(photo.absolutePath)
                allBarcodes.addAll(barcodes)
                processedCount++
                
                resultTextView?.text = "Procesadas $processedCount de ${photos.size} fotos. " +
                    "Códigos encontrados: ${allBarcodes.size}"
            } catch (e: Exception) {
                resultTextView?.text = "Error procesando ${photo.name}: ${e.message}"
            }
        }

        if (allBarcodes.isNotEmpty()) {
            exportToCSV(allBarcodes, photos)
        } else {
            resultTextView?.text = "No se encontraron códigos QR/barras en las fotos"
        }
    }

    private fun exportToCSV(barcodes: List<String>, photos: List<File>) {
        try {
            val csvExporter = CsvExporter(this)
            val csvFile = csvExporter.exportBarcodes(barcodes, photos)
            resultTextView?.text = "Exportación completa: ${csvFile.absolutePath}\n" +
                "Total códigos: ${barcodes.size}\n" +
                "Total fotos: ${photos.size}"
        } catch (e: Exception) {
            resultTextView?.text = "Error exportando CSV: ${e.message}"
        }
    }
}
