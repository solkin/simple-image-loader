package com.tomclaw.imageloader

import com.tomclaw.cache.DiskLruCache
import java.io.File

interface DiskCache {

    fun get(key: String): File?

    fun put(key: String, file: File): File

    fun remove(key: String)

}

class DiskCacheImpl(private val diskLruCache: DiskLruCache): DiskCache {

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