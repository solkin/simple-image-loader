package com.tomclaw.imageloader.core

import android.net.Uri
import java.io.File
import java.io.IOException

interface FileProvider {

    fun getFile(url: String): File? = getFile(Uri.parse(url))

    fun getFile(uri: Uri): File?

}

class FileProviderImpl(
    private val cacheDir: File,
    private val diskCache: DiskCache,
    vararg fileLoaders: FileLoader
) : FileProvider {

    private val loaders = HashMap<String, FileLoader>()

    init {
        fileLoaders.forEach { loader ->
            loader.schemes.forEach { loaders[it] = loader }
        }
    }

    override fun getFile(uri: Uri): File? {
        return diskCache.get(uri.toString()) ?: loadIntoCache(uri)
    }

    private fun loadIntoCache(uri: Uri): File? {
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("file", ".tmp", cacheDir)

            val loader = loaders[uri.scheme] ?: return null

            val uriString = uri.toString()

            return loader.load(uriString, tempFile)
                .takeIf { true }
                ?.let { diskCache.put(uriString, tempFile) }
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            tempFile?.delete()
        }
        return null
    }

}