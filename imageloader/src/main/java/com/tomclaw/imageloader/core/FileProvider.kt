package com.tomclaw.imageloader.core

import android.net.Uri
import java.io.File

private const val TAG = "FileProvider"
private val log: Logger get() = SimpleImageLoaderLog.logger

interface FileProvider {

    fun getFile(url: String): File? = getFile(Uri.parse(url))

    fun getFile(uri: Uri): File?

}

class FileProviderImpl(
    private val cacheDir: File,
    private val diskCache: DiskCache,
    vararg loaders: Loader
) : FileProvider {

    private val loaders = HashMap<String, Loader>()

    init {
        loaders.forEach { loader ->
            loader.schemes.forEach { this.loaders[it] = loader }
        }
    }

    override fun getFile(uri: Uri): File? {
        val cached = diskCache.get(uri.toString())
        if (cached != null) {
            log.d(TAG, "Disk cache hit: $uri")
            return cached
        }
        log.d(TAG, "Disk cache miss: $uri")
        return loadIntoCache(uri)
    }

    private fun loadIntoCache(uri: Uri): File? {
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("file", ".tmp", cacheDir)

            val loader = loaders[uri.scheme]
            if (loader == null) {
                log.e(TAG, "No loader for scheme: ${uri.scheme}")
                return null
            }

            val uriString = uri.toString()
            log.d(TAG, "Loading: $uriString")

            val success = loader.load(uriString, tempFile)
            if (!success) {
                log.e(TAG, "Loader failed: $uriString")
                return null
            }

            log.d(TAG, "Loaded ${tempFile.length()} bytes: $uriString")
            val cachedFile = diskCache.put(uriString, tempFile)
            log.d(TAG, "Cached to disk: ${cachedFile.absolutePath}")
            return cachedFile
        } catch (ex: Throwable) {
            log.e(TAG, "Exception loading $uri: ${ex.message}", ex)
        } finally {
            tempFile?.delete()
        }
        return null
    }

}