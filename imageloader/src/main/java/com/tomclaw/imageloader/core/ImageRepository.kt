package com.tomclaw.imageloader.core

import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

/**
 * Core image loading and caching logic.
 * This class is UI-agnostic and can be used with any UI framework.
 */
interface ImageRepository {

    /**
     * Loads an image synchronously. Call from background thread.
     * @param url The image URL (supports http, https, file, content schemes)
     * @param width Target width for downsampling
     * @param height Target height for downsampling
     * @return Decoded image result or null if loading failed
     */
    fun load(url: String, width: Int, height: Int): Result?

    /**
     * Loads an image asynchronously.
     * @param url The image URL
     * @param width Target width for downsampling
     * @param height Target height for downsampling
     * @return Future that resolves to the decoded image result
     */
    fun loadAsync(url: String, width: Int, height: Int): Future<Result?>

    /**
     * Gets a cached result if available.
     * @param url The image URL
     * @param width Target width
     * @param height Target height
     * @return Cached result or null
     */
    fun getCached(url: String, width: Int, height: Int): Result?

    /**
     * Generates a cache key for the given parameters.
     */
    fun generateKey(url: String, width: Int, height: Int): String

}

class ImageRepositoryImpl(
    private val fileProvider: FileProvider,
    private val decoders: List<Decoder>,
    private val memoryCache: MemoryCache,
    private val backgroundExecutor: ExecutorService
) : ImageRepository {

    override fun load(url: String, width: Int, height: Int): Result? {
        val key = generateKey(url, width, height)

        // Check memory cache first
        memoryCache.get(key)
            ?.takeUnless { it.isRecycled() }
            ?.let { return it }

        // Load from disk/network
        val file = fileProvider.getFile(url) ?: return null

        // Find suitable decoder and decode
        val result = decoders
            .find { decoder -> decoder.probe(file) }
            ?.decode(file, width, height)
            ?: return null

        // Cache the result
        memoryCache.put(key, result)

        return result
    }

    override fun loadAsync(url: String, width: Int, height: Int): Future<Result?> {
        return backgroundExecutor.submit(Callable {
            load(url, width, height)
        })
    }

    override fun getCached(url: String, width: Int, height: Int): Result? {
        val key = generateKey(url, width, height)
        return memoryCache.get(key)?.takeUnless { it.isRecycled() }
    }

    override fun generateKey(url: String, width: Int, height: Int): String {
        return url.toSHA1() + "_" + width + "_" + height
    }

    private fun String.toSHA1(): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

}

