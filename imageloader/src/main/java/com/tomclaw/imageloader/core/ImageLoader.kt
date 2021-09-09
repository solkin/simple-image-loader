package com.tomclaw.imageloader.core

import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

interface ImageLoader {

    fun <T> load(
        view: ViewHolder<T>,
        uriString: String,
        handlers: Handlers<T>
    )

}

class ImageLoaderImpl(
    private val fileProvider: FileProvider,
    private val decoder: Decoder,
    private val memoryCache: MemoryCache,
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
        val key = generateKey(uriString, size.width, size.height)
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

        memoryCache.get(key)
            ?.takeUnless { it.isRecycled() }
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
        val weakImageView = WeakReference(view)
        handlers.placeholder.invoke(view)
        backgroundExecutor.submit {
            fileProvider.getFile(uriString)
                .takeIf { it != null }
                ?.let { file ->
                    decoder.decode(file, size.width, size.height)
                }
                ?.let { result ->
                    memoryCache.put(key, result)
                    mainExecutor.execute {
                        weakImageView.get()?.apply {
                            if (tag == key) {
                                handlers.success.invoke(view, result)
                            }
                        }
                        futures.remove(key)
                    }
                } ?: handlers.error.invoke(view)
        }.let { future ->
            futures[uriString] = future
        }
    }

    private fun generateKey(url: String, width: Int, height: Int): String {
        return url.toSHA1() + "_" + width + "_" + height
    }

    private fun String.toSHA1(): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(this.toByteArray())
        return bytes.toHex()
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

}
