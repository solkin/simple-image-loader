package com.tomclaw.imageloader.core

import android.util.LruCache

interface MemoryCache {

    fun get(key: String): Result?

    fun put(key: String, result: Result): Result?

    fun remove(key: String): Result?

}

class MemoryCacheImpl : MemoryCache {

    private val bitmapLruCache: LruCache<String, Result>

    init {
        val maxMemory = Runtime.getRuntime().maxMemory().toInt()
        // Use 1/12th of the available memory for this memory cache.
        val cacheSize = maxMemory / 12
        bitmapLruCache = object : LruCache<String, Result>(cacheSize) {
            override fun sizeOf(key: String?, value: Result): Int {
                return value.getByteCount()
            }
        }
    }

    override fun get(key: String): Result? {
        return bitmapLruCache[key]
    }

    override fun put(key: String, result: Result): Result? {
        return bitmapLruCache.put(key, result)
    }

    override fun remove(key: String): Result? {
        return bitmapLruCache.remove(key)
    }

}