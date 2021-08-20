package com.tomclaw.imageloader

import android.graphics.Bitmap
import android.util.DisplayMetrics
import android.util.Size
import android.widget.ImageView
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future


interface ImageLoader {

    fun load(
        imageView: ImageView,
        url: String,
        success: (ImageView, Bitmap) -> Unit,
        placeholder: (ImageView) -> Unit,
        error: (ImageView) -> Unit
    )

}

class ImageLoaderImpl(
    private val fileProvider: FileProvider,
    private val bitmapDecoder: BitmapDecoder,
    private val memoryCache: MemoryCache,
    private val mainExecutor: Executor,
    private val backgroundExecutor: ExecutorService,
    private val displayMetrics: DisplayMetrics
) : ImageLoader {

    private val futures: MutableMap<String, Future<*>> = HashMap()

    override fun load(
        imageView: ImageView,
        url: String,
        success: (ImageView, Bitmap) -> Unit,
        placeholder: (ImageView) -> Unit,
        error: (ImageView) -> Unit
    ) {
        val size = imageView.getSize()
        val key = generateKey(url, size.width, size.height)
        val prevTag = imageView.tag
        imageView.tag = key
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
            ?.takeUnless { it.isRecycled }
            ?.run { success.invoke(imageView, this) }
            ?: loadAsync(imageView, size, url, key, success, placeholder, error)
    }

    private fun loadAsync(
        imageView: ImageView,
        size: Size,
        url: String,
        key: String,
        success: (ImageView, Bitmap) -> Unit,
        placeholder: (ImageView) -> Unit,
        error: (ImageView) -> Unit
    ) {
        val weakImageView = WeakReference(imageView)
        placeholder.invoke(imageView)
        backgroundExecutor.submit {
            fileProvider.getFile(url)
                .takeIf { it != null }
                ?.let { file ->
                    bitmapDecoder.getBitmap(file, size.width, size.height)
                }
                ?.let { bitmap ->
                    memoryCache.put(key, bitmap)
                    mainExecutor.execute {
                        weakImageView.get()?.apply {
                            if (tag == key) {
                                success.invoke(imageView, bitmap)
                            }
                        }
                        futures.remove(key)
                    }
                } ?: error.invoke(imageView)
        }.let { future ->
            futures[url] = future
        }
    }

    private fun ImageView.getSize(): Size {
        val w = measuredWidth.takeIf { it > 0 } ?: displayMetrics.widthPixels
        val h = measuredHeight.takeIf { it > 0 } ?: displayMetrics.heightPixels
        return Size(w, h)
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