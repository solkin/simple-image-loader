package com.tomclaw.imageloader.core

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong

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
        val size = view.optSize() ?: run { waitSizeAsync(view, uriString, handlers); return }
        val cacheKey = repository.generateKey(uriString, size.width, size.height)

        // Cancel previous request for this view
        val prevTag = view.tag
        if (prevTag is RequestTag) {
            if (prevTag.cacheKey == cacheKey && futures[prevTag.requestId]?.isDone == false) {
                // Same URL and still loading — skip
                return
            }
            // Different URL or completed — cancel previous
            futures.remove(prevTag.requestId)?.cancel(true)
        }

        // Generate unique request ID for this view
        val requestId = requestIdGenerator.incrementAndGet()
        val requestTag = RequestTag(requestId, cacheKey)
        view.tag = requestTag

        // Check cache first
        repository.getCached(uriString, size.width, size.height)
            ?.run { handlers.success.invoke(view, this) }
            ?: loadAsync(view, size, uriString, requestTag, handlers)
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
        val weakView = WeakReference(view)
        handlers.placeholder.invoke(view)

        val future = backgroundExecutor.submit {
            val result = repository.load(uriString, size.width, size.height)

            mainExecutor.execute {
                // Always clean up the future
                futures.remove(requestTag.requestId)

                // Only update view if request is still valid
                val currentView = weakView.get() ?: return@execute
                val currentTag = currentView.tag
                if (currentTag is RequestTag && currentTag.requestId == requestTag.requestId) {
                    if (result != null) {
                        handlers.success.invoke(currentView, result)
                    } else {
                        handlers.error.invoke(currentView)
                    }
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
