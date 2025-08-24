package com.dronescan.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object DebugLogger {
    private const val TAG = "DroneScan_Debug"
    private val logs = mutableListOf<String>()
    private val maxLogs = 100
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    
    fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $level/$tag: $message"
        
        // Agregar al log interno
        synchronized(logs) {
            logs.add(logEntry)
            if (logs.size > maxLogs) {
                logs.removeAt(0)
            }
        }
        
        // Log normal de Android
        when (level) {
            "D" -> Log.d(tag, message, throwable)
            "I" -> Log.i(tag, message, throwable)
            "W" -> Log.w(tag, message, throwable)
            "E" -> Log.e(tag, message, throwable)
            "V" -> Log.v(tag, message, throwable)
        }
        
        // Si hay excepción, agregarla también
        throwable?.let {
            val errorEntry = "[$timestamp] E/$tag: ${it.javaClass.simpleName}: ${it.message}"
            synchronized(logs) {
                logs.add(errorEntry)
                logs.add("[$timestamp] E/$tag: StackTrace: ${it.stackTraceToString()}")
            }
        }
    }
    
    fun d(tag: String, message: String) = log("D", tag, message)
    fun i(tag: String, message: String) = log("I", tag, message)
    fun w(tag: String, message: String) = log("W", tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log("E", tag, message, throwable)
    fun v(tag: String, message: String) = log("V", tag, message)
    
    fun getAllLogs(): String {
        synchronized(logs) {
            return if (logs.isEmpty()) {
                "No hay logs disponibles"
            } else {
                logs.joinToString("\n")
            }
        }
    }
    
    fun getErrorLogs(): String {
        synchronized(logs) {
            val errorLogs = logs.filter { it.contains(" E/") || it.contains("Exception") || it.contains("Error") }
            return if (errorLogs.isEmpty()) {
                "No hay errores registrados"
            } else {
                errorLogs.joinToString("\n")
            }
        }
    }
    
    fun copyLogsToClipboard(context: Context, onlyErrors: Boolean = false): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val logsText = if (onlyErrors) getErrorLogs() else getAllLogs()
            val clip = ClipData.newPlainText("DroneScan Logs", logsText)
            clipboard.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al copiar logs al portapapeles", e)
            false
        }
    }
    
    fun clearLogs() {
        synchronized(logs) {
            logs.clear()
        }
        i(TAG, "Logs limpiados")
    }
}
