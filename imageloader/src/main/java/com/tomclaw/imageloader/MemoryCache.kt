package com.tomclaw.imageloader

import android.graphics.Bitmap
import android.util.LruCache

interface MemoryCache {

    fun get(key: String): Bitmap?

    fun put(key: String, bitmap: Bitmap): Bitmap?

    fun remove(key: String): Bitmap?

}

class MemoryCacheImpl : MemoryCache {

    private val bitmapLruCache: LruCache<String, Bitmap>

    init {
        val maxMemory = Runtime.getRuntime().maxMemory().toInt()
        // Use 1/12th of the available memory for this memory cache.
        val cacheSize = maxMemory / 12
        bitmapLruCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String?, value: Bitmap): Int {
                return value.byteCount
            }
        }
    }

    override fun get(key: String): Bitmap? {
        return bitmapLruCache[key]
    }

    override fun put(key: String, bitmap: Bitmap): Bitmap? {
        return bitmapLruCache.put(key, bitmap)
    }

    override fun remove(key: String): Bitmap? {
        return bitmapLruCache.remove(key)
    }

}