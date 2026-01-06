package com.tomclaw.imageloader.core

import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

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

    private val futures: MutableMap<String, Future<*>> = HashMap()

    override fun <T> load(
        view: ViewHolder<T>,
        uriString: String,
        handlers: Handlers<T>
    ) {
        val size = view.optSize() ?: run { waitSizeAsync(view, uriString, handlers); return }
        val key = repository.generateKey(uriString, size.width, size.height)
        val prevTag = view.tag
        view.tag = key
        val isLoading = prevTag
            ?.takeIf { it is String }
            ?.let { prevKey ->
                val future = futures[prevKey]
                if (prevKey == key && future?.isDone == false) {
                    // This is the same URL and task is not yet completed.
                    true
                } else {
                    future?.cancel(true)
                    false
                }
            }
        if (isLoading == true) return

        // Check cache first
        repository.getCached(uriString, size.width, size.height)
            ?.run { handlers.success.invoke(view, this) }
            ?: loadAsync(view, size, uriString, key, handlers)
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
        key: String,
        handlers: Handlers<T>
    ) {
        val weakView = WeakReference(view)
        handlers.placeholder.invoke(view)
        backgroundExecutor.submit {
            repository.load(uriString, size.width, size.height)
                ?.let { result ->
                    mainExecutor.execute {
                        weakView.get()?.apply {
                            if (tag == key) {
                                handlers.success.invoke(this, result)
                            }
                        }
                        futures.remove(key)
                    }
                } ?: mainExecutor.execute {
                    weakView.get()?.let { handlers.error.invoke(it) }
                    futures.remove(key)
                }
        }.let { future ->
            futures[key] = future
        }
    }

}
