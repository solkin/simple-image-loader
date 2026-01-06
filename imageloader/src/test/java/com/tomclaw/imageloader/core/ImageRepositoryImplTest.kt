package com.tomclaw.imageloader.core

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
import java.util.concurrent.Executors

class ImageRepositoryImplTest {

    private lateinit var fileProvider: FileProvider
    private lateinit var decoder: Decoder
    private lateinit var memoryCache: MemoryCache
    private lateinit var repository: ImageRepositoryImpl

    @Before
    fun setUp() {
        fileProvider = mock()
        decoder = mock()
        memoryCache = mock()
        repository = ImageRepositoryImpl(
            fileProvider,
            listOf(decoder),
            memoryCache,
            Executors.newSingleThreadExecutor()
        )
    }

    @Test
    fun `load returns cached result when available`() {
        val cachedResult = mock<Result>()
        whenever(cachedResult.isRecycled()).thenReturn(false)
        whenever(memoryCache.get(any())).thenReturn(cachedResult)

        val result = repository.load("http://example.com/image.jpg", 100, 100)

        assertEquals(cachedResult, result)
        verify(fileProvider, never()).getFile(any())
    }

    @Test
    fun `load skips recycled cached result`() {
        val recycledResult = mock<Result>()
        whenever(recycledResult.isRecycled()).thenReturn(true)
        whenever(memoryCache.get(any())).thenReturn(recycledResult)

        val file = mock<File>()
        val freshResult = mock<Result>()
        whenever(fileProvider.getFile(any())).thenReturn(file)
        whenever(decoder.probe(file)).thenReturn(true)
        whenever(decoder.decode(file, 100, 100)).thenReturn(freshResult)

        val result = repository.load("http://example.com/image.jpg", 100, 100)

        assertEquals(freshResult, result)
        verify(memoryCache).put(any(), any())
    }

    @Test
    fun `load returns null when file provider fails`() {
        whenever(memoryCache.get(any())).thenReturn(null)
        whenever(fileProvider.getFile(any())).thenReturn(null)

        val result = repository.load("http://example.com/image.jpg", 100, 100)

        assertNull(result)
    }

    @Test
    fun `load returns null when no decoder matches`() {
        val file = mock<File>()
        whenever(memoryCache.get(any())).thenReturn(null)
        whenever(fileProvider.getFile(any())).thenReturn(file)
        whenever(decoder.probe(file)).thenReturn(false)

        val result = repository.load("http://example.com/image.jpg", 100, 100)

        assertNull(result)
    }

    @Test
    fun `load caches successful result`() {
        val file = mock<File>()
        val decodedResult = mock<Result>()
        whenever(memoryCache.get(any())).thenReturn(null)
        whenever(fileProvider.getFile(any())).thenReturn(file)
        whenever(decoder.probe(file)).thenReturn(true)
        whenever(decoder.decode(file, 100, 100)).thenReturn(decodedResult)

        val result = repository.load("http://example.com/image.jpg", 100, 100)

        assertNotNull(result)
        verify(memoryCache).put(any(), any())
    }

    @Test
    fun `getCached returns cached result`() {
        val cachedResult = mock<Result>()
        whenever(cachedResult.isRecycled()).thenReturn(false)
        whenever(memoryCache.get(any())).thenReturn(cachedResult)

        val result = repository.getCached("http://example.com/image.jpg", 100, 100)

        assertEquals(cachedResult, result)
    }

    @Test
    fun `getCached returns null for recycled result`() {
        val recycledResult = mock<Result>()
        whenever(recycledResult.isRecycled()).thenReturn(true)
        whenever(memoryCache.get(any())).thenReturn(recycledResult)

        val result = repository.getCached("http://example.com/image.jpg", 100, 100)

        assertNull(result)
    }

    @Test
    fun `generateKey creates consistent keys`() {
        val key1 = repository.generateKey("http://example.com/image.jpg", 100, 100)
        val key2 = repository.generateKey("http://example.com/image.jpg", 100, 100)

        assertEquals(key1, key2)
    }

    @Test
    fun `generateKey creates different keys for different sizes`() {
        val key1 = repository.generateKey("http://example.com/image.jpg", 100, 100)
        val key2 = repository.generateKey("http://example.com/image.jpg", 200, 200)

        assert(key1 != key2)
    }

    @Test
    fun `loadAsync returns future with result`() {
        val file = mock<File>()
        val decodedResult = mock<Result>()
        whenever(memoryCache.get(any())).thenReturn(null)
        whenever(fileProvider.getFile(any())).thenReturn(file)
        whenever(decoder.probe(file)).thenReturn(true)
        whenever(decoder.decode(file, 100, 100)).thenReturn(decodedResult)

        val future = repository.loadAsync("http://example.com/image.jpg", 100, 100)
        val result = future.get()

        assertEquals(decodedResult, result)
    }
}

