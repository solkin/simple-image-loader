package com.tomclaw.imageloader.util

import com.tomclaw.imageloader.core.FileLoader
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class FileDownloader : FileLoader {

    override val schemes: List<String>
        get() = listOf("http", "https")

    override fun load(uri: String, file: File): Boolean {
        val connection = openConnection(uri)
        val fileStream = FileOutputStream(file)
        return connection.inputStream
            ?.takeIf { connection.responseCode in 200..299 }
            ?.safeCopyTo(fileStream)
            ?.takeIf { true } ?: false
    }

    private fun openConnection(uri: String): HttpURLConnection {
        val url = URL(uri)
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = METHOD_GET
            doInput = true
            doOutput = false
            connectTimeout = TIMEOUT_CONNECTION
            readTimeout = TIMEOUT_SOCKET
        }
    }

}

private const val METHOD_GET = "GET"
private const val TIMEOUT_SOCKET = 70 * 1000
private const val TIMEOUT_CONNECTION = 60 * 1000
