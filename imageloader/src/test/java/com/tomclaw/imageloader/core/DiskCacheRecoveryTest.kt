package com.tomclaw.imageloader.core

import com.tomclaw.cache.DiskLruCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.DataOutputStream
import java.io.File

class DiskCacheRecoveryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val cacheSize = 15L * 1024 * 1024
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        cacheDir = folder.newFolder("cache")
    }

    private fun corruptJournal() {
        DataOutputStream(File(cacheDir, "journal.bin").outputStream()).use { stream ->
            stream.writeInt(999) // version DiskLruCache does not recognize
            stream.writeInt(0)
        }
    }

    @Test
    fun `create opens a healthy cache`() {
        val cache = DiskCache.create(cacheDir, cacheSize)
        assertTrue(cache is DiskCacheImpl)
    }

    @Test
    fun `create recovers from a corrupt journal instead of throwing`() {
        corruptJournal()
        // Raw create would throw IllegalArgumentException here
        val cache = DiskCache.create(cacheDir, cacheSize)
        assertTrue("should recover into a working DiskLruCache", cache is DiskCacheImpl)
    }

    @Test
    fun `recovery keeps nested caches intact`() {
        corruptJournal()
        val nested = File(cacheDir, "picked_media").apply { mkdirs() }
        val keep = File(nested, "keep").apply { writeText("data") }

        DiskCache.create(cacheDir, cacheSize)

        assertTrue("nested caches must survive the wipe", keep.exists())
    }

    @Test
    fun `recovered cache actually stores and returns files`() {
        corruptJournal()
        val cache = DiskCache.create(cacheDir, cacheSize)

        val source = File(folder.newFolder("src"), "img").apply { writeText("payload") }
        val stored = cache.put("http://example.com/a.png", source)
        assertNotNull(stored)

        val hit = cache.get("http://example.com/a.png")
        assertNotNull("stored entry must be retrievable", hit)
        assertEquals("payload", hit!!.readText())
    }

    @Test
    fun `fallback cache stores and retrieves`() {
        val cache = FallbackDiskCache(cacheDir, maxSize = cacheSize)

        val src = File(folder.newFolder("src"), "a").apply { writeText("hello") }
        val stored = cache.put("key-a", src)
        assertEquals("hello", stored.readText())
        assertEquals("hello", cache.get("key-a")?.readText())
        assertNull(cache.get("absent"))
    }

    @Test
    fun `fallback cache clears once over budget`() {
        // maxSize 4 bytes: after the first 5-byte entry the dir is already over budget,
        // so the next put wipes the dir before storing.
        val cache = FallbackDiskCache(cacheDir, maxSize = 4)

        val src = File(folder.newFolder("src"), "a").apply { writeText("hello") }
        cache.put("key-a", src)
        assertEquals("hello", cache.get("key-a")?.readText())

        val src2 = File(folder.newFolder("src2"), "b").apply { writeText("world") }
        cache.put("key-b", src2)
        assertNull("old entry evicted once over budget", cache.get("key-a"))
        assertEquals("world", cache.get("key-b")?.readText())
    }
}
