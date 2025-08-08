package dji.sampleV5.aircraft.util

import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ErrorLogger {
    private const val LOG_FILE_NAME = "DroneScanErrorLog.txt"

    fun log(message: String, tag: String = "General") {
        try {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (!documentsDir.exists()) documentsDir.mkdirs()
            val logFile = File(documentsDir, LOG_FILE_NAME)
            val writer = FileWriter(logFile, true)
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            writer.append("[$timestamp][$tag] $message\n")
            writer.close()
        } catch (_: Exception) {
            // No hacer nada si falla el log
        }
    }

    fun log(e: Throwable, tag: String = "Exception") {
        log(e.toString() + "\n" + e.stackTraceToString(), tag)
    }
}
