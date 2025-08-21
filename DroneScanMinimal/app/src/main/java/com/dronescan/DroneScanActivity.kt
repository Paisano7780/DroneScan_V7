package com.dronescan

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
import android.widget.LinearLayout
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dronescan.barcode.BarcodeProcessor
import com.dronescan.csv.CsvExporter
import com.dronescan.usb.UsbDroneManager
import java.io.File

class DroneScanActivity : AppCompatActivity() {
    private val REQUEST_ALL_PERMISSIONS = 1003
    private val REQUEST_MANAGE_ALL_FILES = 1002
    
    private var titleTextView: TextView? = null
    private var statusTextView: TextView? = null
    private var resultTextView: TextView? = null
    private var scanButton: Button? = null
    private var exportButton: Button? = null
    private var connectButton: Button? = null
    
    private lateinit var usbDroneManager: UsbDroneManager
    private lateinit var barcodeProcessor: BarcodeProcessor
    private lateinit var csvExporter: CsvExporter
    
    private var isUsbConnected = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    updateStatus("Dispositivo USB conectado. Verificando...")
                    checkUsbConnection()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    updateStatus("Dispositivo USB desconectado.")
                    isUsbConnected = false
                    updateConnectionStatus()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupUI()
        initializeComponents()
        setupButtons()
        requestAllPermissions()
        registerUsbReceiver()
        checkUsbConnection()
        
        Log.d("DroneScan", "DroneScan Pro USB iniciado correctamente")
    }

    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        titleTextView = TextView(this).apply {
            text = "DroneScan Pro - Escáner DJI"
            textSize = 20f
            setPadding(0, 0, 0, 24)
            setTextColor(resources.getColor(android.R.color.black))
        }
        
        statusTextView = TextView(this).apply {
            text = "Estado: Inicializando..."
            textSize = 16f
            setPadding(0, 0, 0, 16)
            setTextColor(resources.getColor(android.R.color.darker_gray))
        }
        
        resultTextView = TextView(this).apply {
            text = "DroneScan Pro - Acceso USB Nativo\\n\\n🔌 Características:\\n• Acceso directo vía USB Host\\n• Sin dependencias SDK complejas\\n• Detección automática de dispositivos\\n• Compatible con DJI RC y otros drones\\n• Procesamiento inteligente de imágenes\\n\\nConecta tu drone o control remoto vía USB para comenzar."
            textSize = 14f
            setPadding(0, 0, 0, 24)
            setTextColor(resources.getColor(android.R.color.black))
        }
        
        connectButton = Button(this).apply {
            text = "🔍 Verificar Conexión USB"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
        }
        
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        scanButton = Button(this).apply {
            text = "📸 Escanear Fotos"
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { setMargins(0, 0, 8, 0) }
        }
        
        exportButton = Button(this).apply {
            text = "📁 Ver Exportaciones"
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply { setMargins(8, 0, 0, 0) }
        }
        
        buttonLayout.addView(scanButton)
        buttonLayout.addView(exportButton)
        
        layout.addView(titleTextView)
        layout.addView(statusTextView)
        layout.addView(resultTextView)
        layout.addView(connectButton)
        layout.addView(buttonLayout)
        
        setContentView(layout)
    }
    
    private fun initializeComponents() {
        usbDroneManager = UsbDroneManager(this)
        barcodeProcessor = BarcodeProcessor(this)
        csvExporter = CsvExporter(this)
        
        // Inicializar el USB manager
        usbDroneManager.initialize()
    }
    
    private fun setupButtons() {
        connectButton?.setOnClickListener {
            checkUsbConnection()
        }
        
        scanButton?.setOnClickListener {
            if (isUsbConnected) {
                updateStatus("Iniciando escaneo USB...")
                scanUsbPhotos()
            } else {
                updateStatus("Iniciando escaneo manual...")
                scanDronePhotos()
            }
        }
        
        exportButton?.setOnClickListener {
            openExportFolder()
        }
    }
    
    private fun updateStatus(message: String) {
        statusTextView?.text = "Estado: $message"
        Log.d("DroneScan", message)
    }
    
    private fun updateResult(message: String) {
        resultTextView?.text = message
        Log.d("DroneScan", message)
    }
    
    private fun updateConnectionStatus() {
        val status = if (isUsbConnected) "✅ USB Conectado" else "❌ USB Desconectado"
        connectButton?.text = "🔍 Verificar Conexión USB - $status"
        
        scanButton?.text = if (isUsbConnected) "📸 Escanear USB" else "📸 Escanear Manual"
    }

    private fun checkUsbConnection() {
        try {
            updateStatus("Verificando dispositivos USB...")
            
            isUsbConnected = usbDroneManager.hasConnectedDrone()
            val deviceName = usbDroneManager.getConnectedDeviceName()
            
            if (isUsbConnected && deviceName != null) {
                updateStatus("✅ Dispositivo USB conectado")
                updateResult("🎉 ¡Dispositivo USB detectado!\\n\\n📱 Dispositivo: $deviceName\\n🔗 Conexión: Establecida\\n📡 Acceso: Directo vía USB Host\\n\\n🚀 Listo para escanear fotos directamente del dispositivo.\\n\\nUsa '📸 Escanear USB' para acceso directo a las fotos.")
            } else {
                updateStatus("⚠️ No hay dispositivos USB conectados")
                updateResult("⚠️ No se detectaron dispositivos USB compatibles\\n\\n🔌 Para conectar tu drone o control remoto:\\n1. Conecta el cable USB al móvil\\n2. Acepta permisos USB cuando aparezcan\\n3. Presiona 'Verificar Conexión USB'\\n\\n📱 Mientras tanto, puedes usar el modo manual para escanear fotos locales.")
            }
            
            updateConnectionStatus()
            
        } catch (e: Exception) {
            updateStatus("❌ Error verificando USB")
            updateResult("❌ Error verificando conexión USB: ${e.message}\\n\\n🔄 Soluciones:\\n1. Reconectar el dispositivo USB\\n2. Verificar que el cable funcione\\n3. Reiniciar la aplicación\\n4. Usar modo manual si el problema persiste")
            Log.e("DroneScan", "Error verificando USB", e)
            isUsbConnected = false
            updateConnectionStatus()
        }
    }
    
    private fun scanUsbPhotos() {
        try {
            updateStatus("🔄 Accediendo a fotos vía USB...")
            
            val photos = usbDroneManager.scanForPhotos()
            
            if (photos.isNotEmpty()) {
                updateStatus("📁 Encontradas ${photos.size} fotos")
                updateResult("🔍 Analizando fotos del dispositivo USB...\\n\\n📊 Total de fotos: ${photos.size}\\n🎯 Preparando procesamiento...\\n⏳ Iniciando escaneo de códigos...")
                processPhotos(photos)
            } else {
                updateStatus("📭 No hay fotos en el dispositivo")
                updateResult("📭 No se encontraron fotos en el dispositivo USB\\n\\n🔍 Verifica que:\\n• El dispositivo tenga fotos\\n• Las fotos estén en formato compatible (JPG, PNG, DNG)\\n• El dispositivo esté correctamente conectado\\n\\n💡 Puedes probar con 'Escanear Manual' para usar fotos locales")
            }
        } catch (e: Exception) {
            updateStatus("❌ Error accediendo a USB")
            updateResult("❌ No se pudo acceder a las fotos USB\\n\\n🔍 Error: ${e.message}\\n\\n🔧 Soluciones:\\n• Verificar conexión USB\\n• Verificar permisos de la app\\n• Reconectar el dispositivo\\n• Usar 'Escanear Manual' como alternativa")
            Log.e("DroneScan", "Error obteniendo fotos USB: ${e.message}", e)
        }
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (permissions.isNotEmpty()) {
            updateStatus("🔐 Solicitando permisos...")
            AlertDialog.Builder(this)
                .setTitle("🔐 Permisos Requeridos")
                .setMessage("DroneScan Pro necesita permisos para:\\n\\n📁 Almacenamiento: Leer fotos y guardar resultados\\n📷 Cámara: Escaneo de códigos QR/barras\\n🔗 Conexión: Comunicación con drone DJI\\n\\n✅ Todos los permisos son necesarios para el funcionamiento completo")
                .setPositiveButton("✅ Conceder") { _, _ ->
                    ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_ALL_PERMISSIONS)
                }
                .setNegativeButton("❌ Cancelar") { _, _ ->
                    updateResult("❌ Permisos denegados\\n\\nLa aplicación necesita permisos para funcionar correctamente.\\n\\n🔧 Ve a: Configuración → Aplicaciones → DroneScan → Permisos")
                }
                .show()
        } else {
            checkManageAllFilesPermission()
        }
    }

    private fun checkManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                updateStatus("📁 Configurando acceso total a archivos...")
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivityForResult(intent, REQUEST_MANAGE_ALL_FILES)
                } catch (e: Exception) {
                    updateStatus("❌ Error configurando permisos")
                    Toast.makeText(this, "Configura permisos manualmente en Configuración", Toast.LENGTH_LONG).show()
                }
            } else {
                permissionsGranted()
            }
        } else {
            permissionsGranted()
        }
    }
    
    private fun permissionsGranted() {
        updateStatus("✅ Todos los permisos concedidos")
        updateResult("🎉 ¡Configuración completada!\\n\\n✅ Todos los permisos están configurados\\n🔧 DroneScan Pro está listo para usar\\n\\n🚀 Próximos pasos:\\n1. Conecta tu drone DJI Mini 3 vía USB\\n2. Usa '🔍 Verificar Conexión DJI'\\n3. Inicia el escaneo con '📸 Escanear Fotos'\\n\\n💡 También puedes usar el modo manual sin drone")
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        registerReceiver(usbReceiver, filter)
        Log.d("DroneScan", "USB receiver registrado")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(usbReceiver)
            usbDroneManager.cleanup()
        } catch (e: Exception) {
            Log.e("DroneScan", "Error en cleanup: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ALL_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    updateStatus("✅ Permisos básicos concedidos")
                    checkManageAllFilesPermission()
                } else {
                    updateStatus("⚠️ Algunos permisos denegados")
                    updateResult("⚠️ Algunos permisos fueron denegados\\n\\n🔧 Para funcionalidad completa:\\n1. Ve a Configuración → Aplicaciones → DroneScan\\n2. Habilita todos los permisos\\n3. Reinicia la aplicación\\n\\n💡 La app puede funcionar con limitaciones")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MANAGE_ALL_FILES) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                permissionsGranted()
            } else {
                updateStatus("⚠️ Permiso de archivos pendiente")
                updateResult("⚠️ Permiso de gestión de archivos no concedido\\n\\n🔧 Para funcionalidad completa:\\n1. Ve a Configuración → Aplicaciones → DroneScan\\n2. Activa 'Permitir gestión de todos los archivos'\\n3. Vuelve a la aplicación")
            }
        }
    }

    private fun scanDronePhotos() {
        try {
            // Limpiar el resultado anterior
            updateResult("")
            
            updateStatus("� Iniciando escaneo...")
            
            if (usbDroneManager.hasConnectedDrone()) {
                updateResult("🔗 Dispositivo USB detectado\\n\\n📱 Accediendo directamente vía USB...")
                scanUsbPhotos()
            } else {
                updateStatus("📂 Buscando dispositivos...")
                updateResult("🔍 Buscando dispositivos USB compatibles...")
                
                // Verificar si hay dispositivos USB disponibles
                val availableDevices = usbDroneManager.getAvailableDevices()
                
                if (availableDevices.isNotEmpty()) {
                    updateResult("� Dispositivos encontrados: ${availableDevices.size}\\n\\n🔄 Intentando conexión automática...")
                    scanUsbPhotos()
                } else {
                    updateStatus("� No hay dispositivos USB")
                    updateResult("� No se detectaron dispositivos USB\\n\\n� Verifica que:\\n• El dispositivo esté conectado por USB\\n• Los permisos USB estén habilitados\\n• El modo de transferencia de archivos esté activo\\n\\n💡 También puedes usar 'Escanear Manual' para fotos locales")
                }
            }
            
        } catch (e: Exception) {
            updateStatus("❌ Error en escaneo")
            updateResult("❌ Error durante el escaneo: ${e.message}\\n\\n� Puedes intentar:\\n• Reconectar el dispositivo USB\\n• Usar 'Escanear Manual'\\n• Verificar permisos de la app")
            Log.e("DroneScan", "Error en scanDronePhotos", e)
        }
    }

    private fun processPhotos(photos: List<File>) {
        val allBarcodes = mutableListOf<String>()
        var processedCount = 0

        updateStatus("🔍 Analizando códigos en ${photos.size} fotos...")

        for (photo in photos) {
            try {
                val barcodes = barcodeProcessor.processImage(photo.absolutePath)
                allBarcodes.addAll(barcodes)
                processedCount++
                
                updateStatus("📊 Procesadas $processedCount/${photos.size} • Códigos: ${allBarcodes.size}")
                
                if (processedCount % 5 == 0 || processedCount == photos.size) {
                    updateResult("🔄 Progreso del escaneo:\\n\\n📊 Fotos procesadas: $processedCount/${photos.size}\\n🎯 Códigos encontrados: ${allBarcodes.size}\\n📄 Último archivo: ${photo.name}\\n\\n⏳ ${if (processedCount < photos.size) "Procesando..." else "Finalizando..."}")
                }
            } catch (e: Exception) {
                Log.e("DroneScan", "Error procesando ${photo.name}: ${e.message}")
            }
        }

        updateStatus("✅ Procesamiento completado")

        if (allBarcodes.isNotEmpty()) {
            exportToCSV(allBarcodes, photos)
        } else {
            updateResult("✅ Análisis completado\\n\\n📊 Resultados:\\n• ${photos.size} fotos analizadas\\n• 0 códigos QR/barras encontrados\\n\\n💡 Posibles causas:\\n• Fotos sin códigos visibles\\n• Códigos muy pequeños o borrosos\\n• Ángulos no legibles\\n• Iluminación insuficiente\\n\\n🔍 Revisa que las fotos contengan códigos claros y legibles")
        }
    }

    private fun exportToCSV(barcodes: List<String>, photos: List<File>) {
        try {
            updateStatus("💾 Exportando a CSV...")
            
            val csvFile = csvExporter.exportBarcodes(barcodes, photos)
            
            updateStatus("🎉 Exportación completada")
            updateResult("🎉 ¡Exportación exitosa!\\n\\n📄 Archivo: ${csvFile.name}\\n📁 Ubicación: Documents/DroneScan/\\n\\n📊 Estadísticas detalladas:\\n• 🎯 Códigos totales: ${barcodes.size}\\n• 📸 Fotos procesadas: ${photos.size}\\n• 🔢 Códigos únicos: ${barcodes.distinct().size}\\n• 📋 Duplicados: ${barcodes.size - barcodes.distinct().size}\\n\\n💾 Encuentra tu archivo CSV en la carpeta de exportaciones")
            
            Toast.makeText(this, "🎉 CSV exportado: ${barcodes.size} códigos", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            updateStatus("❌ Error en exportación")
            updateResult("❌ Error exportando CSV: ${e.message}\\n\\n🔧 Verifica:\\n• Permisos de escritura\\n• Espacio disponible\\n• Acceso a Documents/\\n\\n🔄 Intenta reiniciar si persiste el problema")
            Log.e("DroneScan", "Error exportando CSV", e)
        }
    }
    
    private fun openExportFolder() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2FDroneScan")
                addCategory(Intent.CATEGORY_DEFAULT)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments")
                    type = "*/*"
                }
                if (fallbackIntent.resolveActivity(packageManager) != null) {
                    startActivity(fallbackIntent)
                } else {
                    Toast.makeText(this, "📁 Busca: Documents/DroneScan en tu explorador de archivos", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "📁 Ve a: Documents/DroneScan", Toast.LENGTH_LONG).show()
        }
    }
}
