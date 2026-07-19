package com.tomclaw.imageloader.core

import com.tomclaw.cache.DiskLruCache
import java.io.File
import java.security.MessageDigest

private const val TAG = "DiskCache"
private val log: Logger get() = SimpleImageLoaderLog.logger

interface DiskCache {

    fun get(key: String): File?

    fun put(key: String, file: File): File

    fun remove(key: String)

    companion object {

        /**
         * Opens a DiskLruCache without ever throwing. A journal killed mid-write leaves
         * an unreadable file that DiskLruCache.create rejects for good, and the caller
         * used to crash on every image bind. Here a failed open wipes the cache dir and
         * retries once; if it still fails the cache degrades to [FallbackDiskCache] so
         * image loading keeps working instead of taking the process down.
         */
        fun create(cacheDir: File, maxSize: Long): DiskCache {
            try {
                return DiskCacheImpl(DiskLruCache.create(cacheDir, maxSize))
            } catch (ex: Throwable) {
                log.e(TAG, "Failed to open disk cache, resetting: ${cacheDir.absolutePath}", ex)
            }
            wipeCacheFiles(cacheDir)
            try {
                return DiskCacheImpl(DiskLruCache.create(cacheDir, maxSize))
            } catch (ex: Throwable) {
                log.e(TAG, "Disk cache still unavailable, falling back", ex)
            }
            return FallbackDiskCache(cacheDir, maxSize)
        }

        /** Only root-level files (journal + entries) belong to DiskLruCache; leave nested caches alone. */
        private fun wipeCacheFiles(cacheDir: File) {
            try {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) file.delete()
                }
            } catch (ex: Throwable) {
                log.e(TAG, "Failed to reset disk cache", ex)
            }
        }
    }

}

class DiskCacheImpl(private val diskLruCache: DiskLruCache) : DiskCache {

    override fun get(key: String): File? {
        return diskLruCache[key]
    }

    override fun put(key: String, file: File): File {
        return diskLruCache.put(key, file)
    }

    override fun remove(key: String) {
        diskLruCache.delete(key)
    }

}

/**
 * Journal-free cache used only when DiskLruCache can't be opened. Files live in a
 * dedicated subdirectory named by key hash. There is no per-entry eviction, so the
 * whole directory is cleared once it grows past [maxSize] — crude, but it keeps the
 * footprint bounded without a journal to corrupt.
 */
class FallbackDiskCache(cacheDir: File, private val maxSize: Long) : DiskCache {

    private val dir = File(cacheDir, FALLBACK_DIR).apply { mkdirs() }

    override fun get(key: String): File? {
        return try {
            fileFor(key).takeIf { it.exists() && it.length() > 0 }
        } catch (ex: Throwable) {
            log.e(TAG, "Fallback get failed", ex)
            null
        }
    }

    override fun put(key: String, file: File): File {
        return try {
            if (dirSize() > maxSize) clear()
            val target = fileFor(key)
            file.copyTo(target, overwrite = true)
            target
        } catch (ex: Throwable) {
            log.e(TAG, "Fallback put failed", ex)
            file
        }
    }

    override fun remove(key: String) {
        try {
            fileFor(key).delete()
        } catch (ex: Throwable) {
            log.e(TAG, "Fallback remove failed", ex)
        }
    }

    private fun fileFor(key: String) = File(dir, key.hashName())

    private fun dirSize(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    private fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun String.hashName(): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val FALLBACK_DIR = "fallback"
    }

}
