package com.tomclaw.imageloader.util.loader

import com.tomclaw.imageloader.core.Loader
import com.tomclaw.imageloader.util.safeClose
import com.tomclaw.imageloader.util.safeCopyTo
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class UrlLoader : Loader {

    override val schemes: List<String>
        get() = listOf("http", "https")

    override fun load(uriString: String, file: File): Boolean {
        val connection = openConnection(uriString)
        var input: InputStream? = null
        var output: OutputStream? = null
        try {
            input = connection.inputStream
            output = FileOutputStream(file)
            return input
                ?.takeIf { connection.responseCode in 200..299 }
                ?.safeCopyTo(output)
                ?.takeIf { true } ?: false
        } finally {
            input?.safeClose()
            output?.safeClose()
            connection.disconnect()
        }
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
