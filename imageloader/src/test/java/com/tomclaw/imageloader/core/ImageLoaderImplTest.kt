package com.tomclaw.imageloader.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ImageLoaderImplTest {

    private lateinit var repository: ImageRepository
    private lateinit var imageLoader: ImageLoaderImpl

    // Executor that runs immediately on same thread for predictable testing
    private val immediateExecutor = java.util.concurrent.Executor { it.run() }
    private val backgroundExecutor = Executors.newFixedThreadPool(4)

    @Before
    fun setUp() {
        repository = mock()
        whenever(repository.generateKey(any(), any(), any())).thenAnswer { invocation ->
            "${invocation.arguments[0]}_${invocation.arguments[1]}_${invocation.arguments[2]}"
        }
        imageLoader = ImageLoaderImpl(repository, immediateExecutor, backgroundExecutor)
    }

    @Test
    fun `load uses cached result when available`() {
        val cachedResult = mock<Result>()
        whenever(repository.getCached("http://example.com/image.jpg", 100, 100))
            .thenReturn(cachedResult)

        val viewHolder = createMockViewHolder(100, 100)
        val handlers = Handlers<Any>()
        var successCalled = false
        handlers.successHandler { _, result ->
            successCalled = true
            assertEquals(cachedResult, result)
        }

        imageLoader.load(viewHolder, "http://example.com/image.jpg", handlers)

        assert(successCalled)
        verify(repository, never()).load(any(), any(), any())
    }

    @Test
    fun `load calls repository when not cached`() {
        val result = mock<Result>()
        whenever(repository.getCached(any(), any(), any())).thenReturn(null)
        whenever(repository.load("http://example.com/image.jpg", 100, 100)).thenReturn(result)

        val viewHolder = createMockViewHolder(100, 100)
        val handlers = Handlers<Any>()
        val latch = CountDownLatch(1)
        var receivedResult: Result? = null

        handlers.successHandler { _, r ->
            receivedResult = r
            latch.countDown()
        }

        imageLoader.load(viewHolder, "http://example.com/image.jpg", handlers)

        latch.await(2, TimeUnit.SECONDS)
        assertEquals(result, receivedResult)
    }

    @Test
    fun `load calls error handler on failure`() {
        whenever(repository.getCached(any(), any(), any())).thenReturn(null)
        whenever(repository.load(any(), any(), any())).thenReturn(null)

        val viewHolder = createMockViewHolder(100, 100)
        val handlers = Handlers<Any>()
        val latch = CountDownLatch(1)
        var errorCalled = false

        handlers.errorHandler {
            errorCalled = true
            latch.countDown()
        }

        imageLoader.load(viewHolder, "http://example.com/image.jpg", handlers)

        latch.await(2, TimeUnit.SECONDS)
        assert(errorCalled)
    }

    @Test
    fun `same URL same view skips duplicate request`() {
        whenever(repository.getCached(any(), any(), any())).thenReturn(null)
        whenever(repository.load(any(), any(), any())).thenAnswer {
            Thread.sleep(100)
            mock<Result>()
        }

        val viewHolder = createMockViewHolder(100, 100)
        val handlers = Handlers<Any>()

        // First request
        imageLoader.load(viewHolder, "http://example.com/image.jpg", handlers)
        // Second request for same URL on same view — should skip
        imageLoader.load(viewHolder, "http://example.com/image.jpg", handlers)

        Thread.sleep(300)

        // Repository.load should only be called once
        verify(repository, times(1)).load(any(), any(), any())
    }

    @Test
    fun `different URL cancels previous request`() {
        whenever(repository.getCached(any(), any(), any())).thenReturn(null)
        whenever(repository.load(any(), any(), any())).thenAnswer {
            Thread.sleep(500)
            mock<Result>()
        }

        val viewHolder = createMockViewHolder(100, 100)
        val handlers = Handlers<Any>()
        var successCount = 0

        handlers.successHandler { _, _ ->
            successCount++
        }

        // First request
        imageLoader.load(viewHolder, "http://example.com/image1.jpg", handlers)
        Thread.sleep(50)
        // Second request with different URL — should cancel first
        imageLoader.load(viewHolder, "http://example.com/image2.jpg", handlers)

        Thread.sleep(1000)

        // Only second request should succeed (first was cancelled)
        assertEquals(1, successCount)
    }

    @Test
    fun `multiple views loading same URL get independent requests`() {
        val result = mock<Result>()
        whenever(repository.getCached(any(), any(), any())).thenReturn(null)
        whenever(repository.load(any(), any(), any())).thenReturn(result)

        val viewHolder1 = createMockViewHolder(100, 100)
        val viewHolder2 = createMockViewHolder(100, 100)
        val handlers = Handlers<Any>()
        val latch = CountDownLatch(2)
        var successCount = 0

        handlers.successHandler { _, _ ->
            synchronized(this) { successCount++ }
            latch.countDown()
        }

        imageLoader.load(viewHolder1, "http://example.com/image.jpg", handlers)
        imageLoader.load(viewHolder2, "http://example.com/image.jpg", handlers)

        latch.await(2, TimeUnit.SECONDS)

        // Both views should receive success callback
        assertEquals(2, successCount)
    }

    @Test
    fun `placeholder is called before loading`() {
        whenever(repository.getCached(any(), any(), any())).thenReturn(null)
        whenever(repository.load(any(), any(), any())).thenAnswer {
            Thread.sleep(100)
            mock<Result>()
        }

        val viewHolder = createMockViewHolder(100, 100)
        val handlers = Handlers<Any>()
        var placeholderCalled = false

        handlers.placeholderHandler {
            placeholderCalled = true
        }

        imageLoader.load(viewHolder, "http://example.com/image.jpg", handlers)

        // Placeholder should be called immediately
        assert(placeholderCalled)
    }

    private fun createMockViewHolder(width: Int, height: Int): ViewHolder<Any> {
        return object : ViewHolder<Any> {
            override var tag: Any? = null
            private val size = ViewSize(width, height)

            override fun optSize() = size
            override fun getSize() = size
            override fun get() = Any()
        }
    }
}

