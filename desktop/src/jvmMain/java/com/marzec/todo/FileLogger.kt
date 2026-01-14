package com.marzec.todo

import com.marzec.logger.Logger
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FileLogger(private val logDir: File) : Logger {

    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val logTimestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    override fun log(tag: String, message: String) {
        logToFile("[$tag] $message")
    }

    override fun log(tag: String, message: String, t: Throwable) {
        logToFile("[$tag] $message", t)
    }

    private fun logToFile(message: String, throwable: Throwable? = null) {
        val logFile = getLogFile()
        PrintWriter(FileWriter(logFile, true)).use {
            val timestamp = logTimestampFormat.format(Date())
            it.println("[$timestamp] $message")
            throwable?.printStackTrace(it)
        }
        cleanupOldLogs()
    }

    private fun getLogFile(): File {
        val today = fileDateFormat.format(Date())
        return File(logDir, "log-$today.txt")
    }

    private fun cleanupOldLogs() {
        val twoWeeksAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -14)
        }.time
        logDir.listFiles { file ->
            if (file.name.startsWith("log-") && file.name.endsWith(".txt")) {
                try {
                    val datePart = file.name.substringAfter("log-").substringBefore(".txt")
                    val fileDate = fileDateFormat.parse(datePart)
                    fileDate?.before(twoWeeksAgo) ?: false
                } catch (e: ParseException) {
                    false
                }
            } else {
                false
            }
        }?.forEach { it.delete() }
    }
}
