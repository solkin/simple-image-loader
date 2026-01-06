package com.tomclaw.imageloader.core

import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "ImageRepository"
private val log: Logger get() = SimpleImageLoaderLog.logger

/**
 * Core image loading and caching logic.
 * This class is UI-agnostic and can be used with any UI framework.
 */
interface ImageRepository {

    /**
     * Loads an image synchronously. Call from background thread.
     * Duplicate requests for the same URL are coalesced.
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

    // Locks for coalescing duplicate requests to the same URL
    private val loadingLocks = ConcurrentHashMap<String, ReentrantLock>()

    override fun load(url: String, width: Int, height: Int): Result? {
        val key = generateKey(url, width, height)

        // Check memory cache first (lock-free fast path)
        memoryCache.get(key)
            ?.takeUnless { it.isRecycled() }
            ?.let {
                log.d(TAG, "Memory cache hit: $url")
                return it
            }

        // Get or create lock for this key to coalesce duplicate requests
        val lock = loadingLocks.computeIfAbsent(key) { ReentrantLock() }
        val waitingForLock = lock.isLocked
        if (waitingForLock) {
            log.d(TAG, "Waiting for coalesced request: $url")
        }

        return lock.withLock {
            try {
                // Double-check cache after acquiring lock
                // (another thread might have loaded it while we waited)
                memoryCache.get(key)
                    ?.takeUnless { it.isRecycled() }
                    ?.let {
                        log.d(TAG, "Memory cache hit after wait: $url")
                        return it
                    }

                // Load from disk/network
                log.d(TAG, "FileProvider.getFile: $url")
                val file = fileProvider.getFile(url)
                if (file == null) {
                    log.e(TAG, "FileProvider returned null: $url")
                    return null
                }
                log.d(TAG, "File obtained: ${file.absolutePath} (${file.length()} bytes)")

                // Find suitable decoder and decode
                val decoder = decoders.find { decoder -> decoder.probe(file) }
                if (decoder == null) {
                    log.e(TAG, "No decoder found for: $url")
                    return null
                }

                log.d(TAG, "Decoding: $url (${width}x${height})")
                val result = decoder.decode(file, width, height)
                if (result == null) {
                    log.e(TAG, "Decoder returned null: $url")
                    return null
                }

                // Cache the result
                memoryCache.put(key, result)
                log.d(TAG, "Cached result: $url (${result.getByteCount()} bytes)")

                result
            } finally {
                // Clean up lock if no one else is waiting
                loadingLocks.remove(key, lock)
            }
        }
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

