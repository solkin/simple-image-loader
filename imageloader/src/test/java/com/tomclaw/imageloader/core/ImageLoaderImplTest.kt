package com.tomclaw.imageloader.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
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
            override fun awaitSize(onSize: (ViewSize) -> Unit): Cancellable {
                onSize(size)
                return Cancellable { }
            }
            override fun get() = Any()
        }
    }

    @Test
    fun `views waiting for their size hold no background thread`() {
        val singleThread = Executors.newSingleThreadExecutor()
        val loader = ImageLoaderImpl(repository, immediateExecutor, singleThread)
        whenever(repository.getCached(any(), any(), any())).thenReturn(null)
        whenever(repository.load(any(), any(), any())).thenReturn(mock())

        // More views never laid out than the pool has threads
        repeat(20) { loader.load(UnsizedViewHolder(), "http://example.com/$it.jpg", Handlers()) }

        val latch = CountDownLatch(1)
        val handlers = Handlers<Any>().successHandler { _, _ -> latch.countDown() }
        loader.load(createMockViewHolder(100, 100), "http://example.com/image.jpg", handlers)

        assertTrue("a sized view still loads", latch.await(2, TimeUnit.SECONDS))
        singleThread.shutdownNow()
    }

    @Test
    fun `view waiting for its size loads once it has one`() {
        val result = mock<Result>()
        whenever(repository.getCached(any(), any(), any())).thenReturn(null)
        whenever(repository.load("http://example.com/image.jpg", 100, 50)).thenReturn(result)

        val view = UnsizedViewHolder()
        val latch = CountDownLatch(1)
        var placeholderCalled = false
        var receivedResult: Result? = null
        val handlers = Handlers<Any>()
            .placeholderHandler { placeholderCalled = true }
            .successHandler { _, r ->
                receivedResult = r
                latch.countDown()
            }

        imageLoader.load(view, "http://example.com/image.jpg", handlers)
        assertTrue("placeholder shows while waiting", placeholderCalled)
        verify(repository, never()).load(any(), any(), any())

        view.layOut(ViewSize(100, 50))

        latch.await(2, TimeUnit.SECONDS)
        assertEquals(result, receivedResult)
    }

    @Test
    fun `newer request for the view cancels its wait for size`() {
        whenever(repository.getCached(any(), any(), any())).thenReturn(null)
        whenever(repository.load(any(), any(), any())).thenReturn(mock())

        val view = UnsizedViewHolder()
        val loaded = mutableListOf<String>()
        val latch = CountDownLatch(1)
        fun handlers(url: String) = Handlers<Any>().successHandler { _, _ ->
            synchronized(loaded) { loaded += url }
            latch.countDown()
        }

        imageLoader.load(view, "http://example.com/old.jpg", handlers("old"))
        val oldWait = view.waits.single()
        imageLoader.load(view, "http://example.com/new.jpg", handlers("new"))

        assertTrue("the older wait is cancelled", oldWait.cancelled)
        assertEquals("only the newer one waits", 1, view.waits.count { !it.cancelled })

        view.layOut(ViewSize(100, 100))
        // Even a wait that fires late lands nowhere
        oldWait.onSize(ViewSize(100, 100))

        latch.await(2, TimeUnit.SECONDS)
        Thread.sleep(100)
        assertEquals(listOf("new"), loaded)
        verify(repository, never()).load(eq("http://example.com/old.jpg"), any(), any())
    }

    @Test
    fun `request waiting for size cancels the load before it`() {
        whenever(repository.getCached(any(), any(), any())).thenReturn(null)
        whenever(repository.load(any(), any(), any())).thenAnswer {
            Thread.sleep(300)
            mock<Result>()
        }

        val view = UnsizedViewHolder(ViewSize(100, 100))
        var successCount = 0
        val handlers = Handlers<Any>().successHandler { _, _ -> successCount++ }

        imageLoader.load(view, "http://example.com/old.jpg", handlers)
        view.size = null
        imageLoader.load(view, "http://example.com/new.jpg", handlers)

        Thread.sleep(600)
        assertEquals("the older load lands nowhere", 0, successCount)
    }

    @Test
    fun `wait for size of a view that dropped its tag lands nowhere`() {
        whenever(repository.getCached(any(), any(), any())).thenReturn(mock())

        val view = UnsizedViewHolder()
        var successCalled = false
        val handlers = Handlers<Any>().successHandler { _, _ -> successCalled = true }

        imageLoader.load(view, "http://example.com/image.jpg", handlers)
        view.tag = null
        view.layOut(ViewSize(100, 100))

        assertFalse(successCalled)
    }

    @Test
    fun `cancel stops the view's wait for size`() {
        whenever(repository.getCached(any(), any(), any())).thenReturn(mock())

        val view = UnsizedViewHolder()
        var successCalled = false
        val handlers = Handlers<Any>().successHandler { _, _ -> successCalled = true }

        imageLoader.load(view, "http://example.com/image.jpg", handlers)
        val wait = view.waits.single()
        imageLoader.cancel(view)
        view.layOut(ViewSize(100, 100))

        assertTrue(wait.cancelled)
        assertFalse(successCalled)
    }

    @Test
    fun `cancel stops the view's load`() {
        whenever(repository.getCached(any(), any(), any())).thenReturn(null)
        whenever(repository.load(any(), any(), any())).thenAnswer {
            Thread.sleep(200)
            mock<Result>()
        }

        val view = createMockViewHolder(100, 100)
        var called = false
        val handlers = Handlers<Any>()
            .successHandler { _, _ -> called = true }
            .errorHandler { called = true }

        imageLoader.load(view, "http://example.com/image.jpg", handlers)
        imageLoader.cancel(view)

        Thread.sleep(400)
        assertFalse(called)
    }

    /** A view not laid out yet: it waits for [layOut] to have a size. */
    private class UnsizedViewHolder(var size: ViewSize? = null) : ViewHolder<Any> {

        class Wait(val onSize: (ViewSize) -> Unit) : Cancellable {
            var cancelled = false
            override fun cancel() {
                cancelled = true
            }
        }

        val waits = mutableListOf<Wait>()
        override var tag: Any? = null

        override fun optSize() = size

        override fun awaitSize(onSize: (ViewSize) -> Unit): Cancellable {
            size?.let {
                onSize(it)
                return Cancellable { }
            }
            return Wait(onSize).also { waits += it }
        }

        fun layOut(size: ViewSize) {
            this.size = size
            waits.filter { !it.cancelled }.forEach {
                waits -= it
                it.onSize(size)
            }
        }

        override fun get() = Any()
    }
}

