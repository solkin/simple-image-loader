package com.tomclaw.imageloader.core

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong

private const val TAG = "ImageLoader"
private val log: Logger get() = SimpleImageLoaderLog.logger

/**
 * UI-aware image loader that binds loading results to ViewHolder.
 * Uses [ImageRepository] for actual loading and caching.
 */
interface ImageLoader {

    /**
     * The underlying repository for direct access to loading/caching.
     */
    val repository: ImageRepository

    /**
     * Loads an image into a ViewHolder with callback handlers.
     */
    fun <T> load(
        view: ViewHolder<T>,
        uriString: String,
        handlers: Handlers<T>
    )

}

class ImageLoaderImpl(
    override val repository: ImageRepository,
    private val mainExecutor: Executor,
    private val backgroundExecutor: ExecutorService
) : ImageLoader {

    // Thread-safe map using unique request ID as key (not URL-based key)
    private val futures: ConcurrentHashMap<Long, Future<*>> = ConcurrentHashMap()
    private val requestIdGenerator = AtomicLong(0)

    override fun <T> load(
        view: ViewHolder<T>,
        uriString: String,
        handlers: Handlers<T>
    ) {
        val size = view.optSize() ?: run {
            log.d(TAG, "Waiting for size: $uriString")
            waitSizeAsync(view, uriString, handlers)
            return
        }
        val cacheKey = repository.generateKey(uriString, size.width, size.height)

        // Cancel previous request for this view
        val prevTag = view.tag
        if (prevTag is RequestTag) {
            if (prevTag.cacheKey == cacheKey && futures[prevTag.requestId]?.isDone == false) {
                // Same URL and still loading — skip
                log.d(TAG, "Skip duplicate request #${prevTag.requestId}: $uriString")
                return
            }
            // Different URL or completed — cancel previous
            val cancelled = futures.remove(prevTag.requestId)?.cancel(true)
            if (cancelled == true) {
                log.d(TAG, "Cancelled request #${prevTag.requestId}")
            }
        }

        // Generate unique request ID for this view
        val requestId = requestIdGenerator.incrementAndGet()
        val requestTag = RequestTag(requestId, cacheKey)
        view.tag = requestTag

        log.d(TAG, "Request #$requestId: $uriString (${size.width}x${size.height})")

        // Check cache first
        val cached = repository.getCached(uriString, size.width, size.height)
        if (cached != null) {
            log.d(TAG, "Cache hit #$requestId: $uriString")
            handlers.success.invoke(view, cached)
        } else {
            log.d(TAG, "Cache miss #$requestId, loading: $uriString")
            loadAsync(view, size, uriString, requestTag, handlers)
        }
    }

    private fun <T> waitSizeAsync(
        view: ViewHolder<T>,
        uriString: String,
        handlers: Handlers<T>
    ) {
        backgroundExecutor.submit {
            view.getSize()
            mainExecutor.execute {
                load(view, uriString, handlers)
            }
        }
    }

    private fun <T> loadAsync(
        view: ViewHolder<T>,
        size: ViewSize,
        uriString: String,
        requestTag: RequestTag,
        handlers: Handlers<T>
    ) {
        // Keep WeakReference to the actual view (T), not the ViewHolder wrapper
        val actualView = view.get()
        val weakActualView = WeakReference(actualView)
        handlers.placeholder.invoke(view)

        val future = backgroundExecutor.submit {
            log.d(TAG, "Loading #${requestTag.requestId}: $uriString")
            val result = repository.load(uriString, size.width, size.height)

            mainExecutor.execute {
                // Always clean up the future
                futures.remove(requestTag.requestId)

                // Only update view if request is still valid
                val currentActualView = weakActualView.get()
                if (currentActualView == null) {
                    log.w(TAG, "View collected #${requestTag.requestId}: $uriString")
                    return@execute
                }

                // Check tag on the actual view (stored in ViewHolder which wraps it)
                val currentTag = view.tag
                if (currentTag is RequestTag && currentTag.requestId == requestTag.requestId) {
                    if (result != null) {
                        log.d(TAG, "Success #${requestTag.requestId}: $uriString")
                        handlers.success.invoke(view, result)
                    } else {
                        log.e(TAG, "Error #${requestTag.requestId}: $uriString")
                        handlers.error.invoke(view)
                    }
                } else {
                    log.w(TAG, "Stale #${requestTag.requestId}: $uriString (tag mismatch)")
                }
            }
        }

        futures[requestTag.requestId] = future
    }

    /**
     * Tag stored in ViewHolder to track request identity.
     */
    private data class RequestTag(
        val requestId: Long,
        val cacheKey: String
    )

}
