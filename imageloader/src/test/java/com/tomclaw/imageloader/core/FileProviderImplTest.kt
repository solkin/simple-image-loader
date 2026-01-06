package com.tomclaw.imageloader.core

import android.net.Uri
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class FileProviderImplTest {

    private lateinit var cacheDir: File
    private lateinit var diskCache: DiskCache

    @Before
    fun setUp() {
        cacheDir = createTempDir("test_cache")
        diskCache = mock()
    }

    @After
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    @Test
    fun `getFile returns cached file when available`() {
        val cachedFile = File(cacheDir, "cached.jpg").apply { createNewFile() }
        whenever(diskCache.get("http://example.com/image.jpg")).thenReturn(cachedFile)

        val fileProvider = FileProviderImpl(cacheDir, diskCache, createSuccessLoader())

        val result = fileProvider.getFile(Uri.parse("http://example.com/image.jpg"))

        assertEquals(cachedFile, result)
    }

    @Test
    fun `getFile loads and caches file when not in cache`() {
        whenever(diskCache.get(any())).thenReturn(null)
        whenever(diskCache.put(any(), any())).thenAnswer { invocation ->
            invocation.arguments[1] as File
        }

        val fileProvider = FileProviderImpl(cacheDir, diskCache, createSuccessLoader())

        val result = fileProvider.getFile(Uri.parse("http://example.com/image.jpg"))

        assertNotNull(result)
        verify(diskCache).put(any(), any())
    }

    @Test
    fun `getFile does not cache when loader fails`() {
        whenever(diskCache.get(any())).thenReturn(null)

        val failingLoader = createFailingLoader()
        val fileProvider = FileProviderImpl(cacheDir, diskCache, failingLoader)

        val result = fileProvider.getFile(Uri.parse("http://example.com/image.jpg"))

        assertNull(result)
        verify(diskCache, never()).put(any(), any())
    }

    @Test
    fun `getFile returns null for unsupported scheme`() {
        whenever(diskCache.get(any())).thenReturn(null)

        val httpLoader = createSuccessLoader("http")
        val fileProvider = FileProviderImpl(cacheDir, diskCache, httpLoader)

        val result = fileProvider.getFile(Uri.parse("ftp://example.com/image.jpg"))

        assertNull(result)
        verify(diskCache, never()).put(any(), any())
    }

    @Test
    fun `getFile routes to correct loader by scheme`() {
        whenever(diskCache.get(any())).thenReturn(null)
        whenever(diskCache.put(any(), any())).thenAnswer { it.arguments[1] as File }

        var httpLoaderCalled = false
        var fileLoaderCalled = false

        val httpLoader = object : Loader {
            override val schemes = listOf("http", "https")
            override fun load(uriString: String, file: File): Boolean {
                httpLoaderCalled = true
                return true
            }
        }

        val fileLoader = object : Loader {
            override val schemes = listOf("file")
            override fun load(uriString: String, file: File): Boolean {
                fileLoaderCalled = true
                return true
            }
        }

        val fileProvider = FileProviderImpl(cacheDir, diskCache, httpLoader, fileLoader)

        fileProvider.getFile(Uri.parse("http://example.com/image.jpg"))
        assertEquals(true, httpLoaderCalled)
        assertEquals(false, fileLoaderCalled)

        httpLoaderCalled = false
        fileProvider.getFile(Uri.parse("file:///path/to/image.jpg"))
        assertEquals(false, httpLoaderCalled)
        assertEquals(true, fileLoaderCalled)
    }

    @Test
    fun `getFile with string url delegates to uri version`() {
        val cachedFile = File(cacheDir, "cached.jpg").apply { createNewFile() }
        whenever(diskCache.get("http://example.com/image.jpg")).thenReturn(cachedFile)

        val fileProvider = FileProviderImpl(cacheDir, diskCache, createSuccessLoader())

        val result = fileProvider.getFile("http://example.com/image.jpg")

        assertEquals(cachedFile, result)
    }

    private fun createSuccessLoader(vararg schemes: String = arrayOf("http", "https")): Loader {
        return object : Loader {
            override val schemes = schemes.toList()
            override fun load(uriString: String, file: File): Boolean {
                file.writeText("fake image data")
                return true
            }
        }
    }

    private fun createFailingLoader(vararg schemes: String = arrayOf("http", "https")): Loader {
        return object : Loader {
            override val schemes = schemes.toList()
            override fun load(uriString: String, file: File) = false
        }
    }
}

