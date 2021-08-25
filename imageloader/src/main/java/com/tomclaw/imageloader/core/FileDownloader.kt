package com.tomclaw.imageloader.core

import com.tomclaw.imageloader.safeCopyTo
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

interface FileDownloader {

    fun download(url: String, file: File): Boolean

}

class FileDownloaderImpl() : FileDownloader {

    override fun download(url: String, file: File): Boolean {
        val connection = openConnection(url)
        val fileStream = FileOutputStream(file)
        return connection.inputStream
            ?.takeIf { connection.responseCode in 200..299 }
            ?.safeCopyTo(fileStream)
            ?.takeIf { true } ?: false
    }

    private fun openConnection(url: String): HttpURLConnection {
        val u = URL(url)
        return (u.openConnection() as HttpURLConnection).apply {
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
