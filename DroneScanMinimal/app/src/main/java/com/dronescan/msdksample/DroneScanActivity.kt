package com.dronescan.msdksample

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.Result
import com.dronescan.msdksample.barcode.BarcodeProcessor
import com.dronescan.msdksample.csv.CsvExporter
import com.dronescan.msdksample.debug.DebugLogger
import com.dronescan.msdksample.ptp.PtpPhotoManager
import com.dronescan.msdksample.usb.UsbDroneManager
import java.io.File

class DroneScanActivity : AppCompatActivity() {
    private val REQUEST_ALL_PERMISSIONS = 1003
    private val REQUEST_MANAGE_ALL_FILES = 1002
    
    private var titleTextView: TextView? = null
    private var statusTextView: TextView? = null
    private var resultTextView: TextView? = null
    private var scanButton: Button? = null
    private var exportButton: Button? = null
    private var debugLogsButton: Button? = null
    
    private lateinit var usbDroneManager: UsbDroneManager
    private lateinit var barcodeProcessor: BarcodeProcessor
    private lateinit var csvExporter: CsvExporter
    
    private var isUsbConnected = false
    private var photoMonitoringActive = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    updateStatus("Dispositivo USB conectado. Verificando...")
                    appendResult("🔌 Dispositivo USB detectado\n")
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    updateStatus("Dispositivo USB desconectado")
                    appendResult("❌ Dispositivo USB desconectado\n")
                    isUsbConnected = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drone_scan)
        
        DebugLogger.d("DroneScanActivity", "🚀 Iniciando DroneScan v2.6.1 - UI RESTAURADA")
        
        setupUI()
        initializeComponents()
        setupPermissions()
        setupButtonListeners()
        registerUsbReceiver()
        
        // Mostrar versión claramente en la UI
        updateStatus("🚀 DroneScan v2.6.1 - UI Completa Restaurada")
        appendResult("=== DroneScan v2.6.1 ===\n")
        appendResult("✅ UI completa con todos los botones funcionales\n")
        appendResult("✅ USB detection activo\n")
        appendResult("✅ MediaManager integration disponible\n")
        appendResult("📱 Conecta tu drone para comenzar...\n\n")
    }

    private fun setupUI() {
        titleTextView = findViewById(R.id.title_text_view)
        statusTextView = findViewById(R.id.status_text_view)
        resultTextView = findViewById(R.id.result_text_view)
        scanButton = findViewById(R.id.scan_button)
        exportButton = findViewById(R.id.export_button)
        debugLogsButton = findViewById(R.id.debug_logs_button)
    }

    private fun initializeComponents() {
        // Inicializar componentes
        barcodeProcessor = BarcodeProcessor(this)
        csvExporter = CsvExporter(this)
        
        // Configurar UsbDroneManager
        usbDroneManager = UsbDroneManager(this)
        
                // Configurar callback de conexión
        usbDroneManager.onConnectionStatusChanged = { connected: Boolean, message: String ->
            runOnUiThread {
                isUsbConnected = connected
                updateStatus(message)
                appendResult("$message\n")
                
                if (connected) {
                    appendResult("✅ Drone conectado y listo para escaneo\n")
                } else {
                    appendResult("❌ Drone desconectado\n")
                }
            }
        }
        
        // Configurar callback de fotos detectadas
        usbDroneManager.onPhotoDetected = { photoPath: String ->
            runOnUiThread {
                appendResult("📸 Nueva foto detectada: $photoPath\n")
                // Procesar la foto automáticamente
                processPhotoFile(photoPath)
            }
        }
    }

    private fun setupButtonListeners() {
        scanButton?.setOnClickListener {
            if (isUsbConnected) {
                startManualScan()
            } else {
                Toast.makeText(this, "Conecta un drone DJI primero", Toast.LENGTH_SHORT).show()
            }
        }
        
        exportButton?.setOnClickListener {
            exportResults()
        }
        
        debugLogsButton?.setOnClickListener {
            showDebugLogs()
        }
    }

    private fun startManualScan() {
        appendResult("🔍 Iniciando escaneo manual de fotos...\n")
        updateStatus("Escaneando fotos del drone...")
        
        // Usar UsbDroneManager para escanear fotos vía MediaManager
        usbDroneManager.scanForPhotos()
    }

    private fun processPhotoFile(photoPath: String) {
        val photoFileName = File(photoPath).name
        
        DebugLogger.d("DroneScanActivity", "📷 Procesando foto: $photoFileName")
        
        val photoFile = File(photoPath)
        if (!photoFile.exists()) {
            appendResult("❌ Archivo no encontrado: $photoFileName\n")
            return
        }
        
        val barcodes = barcodeProcessor.processImage(photoFile.absolutePath)
        
        if (barcodes.isNotEmpty()) {
            val result = "📷 $photoFileName: ${barcodes.size} código(s) encontrado(s)\n"
            appendResult(result)
            
            barcodes.forEach { barcode ->
                appendResult("  🔍 $barcode\n")
            }
            
            // Exportar automáticamente
            csvExporter.addScanResult(photoFileName, barcodes)
        } else {
            appendResult("📷 $photoFileName: Sin códigos detectados\n")
        }
    }

    private fun exportResults() {
        try {
            val csvFile = csvExporter.exportToCsv()
            updateStatus("✅ Resultados exportados a: ${csvFile.absolutePath}")
            
            // Mostrar diálogo de éxito
            AlertDialog.Builder(this)
                .setTitle("Exportación Exitosa")
                .setMessage("Los resultados se guardaron en:\n${csvFile.absolutePath}")
                .setPositiveButton("OK", null)
                .show()
                
        } catch (e: Exception) {
            val errorMsg = "❌ Error al exportar: ${e.message}"
            updateStatus(errorMsg)
            DebugLogger.e("DroneScanActivity", "Error en exportación", e)
            
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }
    }

    private fun showDebugLogs() {
        val logs = DebugLogger.getAllLogs()
        val logLines = logs.split("\n").takeLast(20)
        
        AlertDialog.Builder(this)
            .setTitle("Debug Logs")
            .setMessage(logLines.joinToString("\n"))
            .setPositiveButton("OK", null)
            .setNegativeButton("Limpiar") { _, _ ->
                DebugLogger.clearLogs()
                Toast.makeText(this, "Logs limpiados", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun checkUsbConnection() {
        updateStatus("Verificando dispositivos USB conectados...")
        runOnUiThread {
            appendResult("🔍 Escaneando dispositivos USB...\n")
        }
    }

    private fun updateStatus(status: String) {
        runOnUiThread {
            statusTextView?.text = "Estado: $status"
        }
    }

    private fun appendResult(result: String) {
        runOnUiThread {
            val currentText = resultTextView?.text?.toString() ?: ""
            resultTextView?.text = "$currentText$result"
        }
    }

    private fun setupPermissions() {
        val permissions = mutableListOf<String>()
        
        // Permisos básicos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        // Android 13+ permisos específicos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_ALL_PERMISSIONS)
        } else {
            checkManageAllFilesPermission()
        }
    }

    private fun checkManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:$packageName")
            startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES)
        }
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbReceiver, filter)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            REQUEST_ALL_PERMISSIONS -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    checkManageAllFilesPermission()
                } else {
                    Toast.makeText(this, "Permisos requeridos para el funcionamiento", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_MANAGE_ALL_FILES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                updateStatus("✅ Permisos completos otorgados")
            } else {
                updateStatus("⚠️ Permisos limitados - funcionalidad reducida")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            DebugLogger.e("DroneScanActivity", "Error unregistering USB receiver", e)
        }
    }
}
