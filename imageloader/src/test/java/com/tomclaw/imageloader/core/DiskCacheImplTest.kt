package com.tomclaw.imageloader.core

import com.tomclaw.cache.DiskLruCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class DiskCacheImplTest {

    @Test
    fun `get returns file from disk lru cache`() {
        val diskLruCache = mock<DiskLruCache>()
        val expectedFile = mock<File>()
        whenever(diskLruCache["test_key"]).thenReturn(expectedFile)

        val diskCache = DiskCacheImpl(diskLruCache)

        val result = diskCache.get("test_key")

        assertEquals(expectedFile, result)
    }

    @Test
    fun `get returns null when key not found`() {
        val diskLruCache = mock<DiskLruCache>()
        whenever(diskLruCache["missing_key"]).thenReturn(null)

        val diskCache = DiskCacheImpl(diskLruCache)

        val result = diskCache.get("missing_key")

        assertNull(result)
    }

    @Test
    fun `put delegates to disk lru cache`() {
        val diskLruCache = mock<DiskLruCache>()
        val file = mock<File>()
        val cachedFile = mock<File>()
        whenever(diskLruCache.put("test_key", file)).thenReturn(cachedFile)

        val diskCache = DiskCacheImpl(diskLruCache)

        val result = diskCache.put("test_key", file)

        assertEquals(cachedFile, result)
        verify(diskLruCache).put("test_key", file)
    }

    @Test
    fun `remove delegates to disk lru cache delete`() {
        val diskLruCache = mock<DiskLruCache>()

        val diskCache = DiskCacheImpl(diskLruCache)
        diskCache.remove("test_key")

        verify(diskLruCache).delete("test_key")
    }
}

