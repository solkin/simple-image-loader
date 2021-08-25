package com.tomclaw.imageloader.core

import android.net.Uri
import java.io.File
import java.io.IOException

interface FileProvider {

    fun getFile(uri: Uri): File? {
        return when (uri.scheme) {
            "http", "https" -> getFile(uri.path.orEmpty())
            else -> null
        }
    }

    fun getFile(url: String): File?

}

class FileProviderImpl(
    private val cacheDir: File,
    private val diskCache: DiskCache,
    private val fileDownloader: FileDownloader
) : FileProvider {

    override fun getFile(url: String): File? {
        return diskCache.get(url) ?: downloadIntoCache(url)
    }

    private fun downloadIntoCache(url: String): File? {
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("file", ".tmp", cacheDir)
            return fileDownloader.download(url, tempFile)
                .takeIf { true }
                ?.let { diskCache.put(url, tempFile) }
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            tempFile?.delete()
        }
        return null
    }

}