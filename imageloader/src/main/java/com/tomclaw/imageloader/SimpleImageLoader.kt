package com.tomclaw.imageloader

import android.util.DisplayMetrics
import com.tomclaw.cache.DiskLruCache
import java.io.File
import java.util.concurrent.Executors

object SimpleImageLoader {

    var imageLoader: ImageLoader? = null

    fun init(
        cacheDir: File,
        displayMetrics: DisplayMetrics
    ) {
        val fileDownloader: FileDownloader = FileDownloaderImpl()
        val bitmapDecoder: BitmapDecoder = BitmapDecoderImpl()
        val diskCache: DiskCache = DiskCacheImpl(DiskLruCache.create(cacheDir, 5242880L))
        val fileProvider: FileProvider = FileProviderImpl(cacheDir, diskCache, fileDownloader)
        val memoryCache: MemoryCache = MemoryCacheImpl()
        val mainExecutor = MainExecutorImpl()
        val backgroundExecutor = Executors.newFixedThreadPool(5)
        imageLoader = ImageLoaderImpl(
            fileProvider,
            bitmapDecoder,
            memoryCache,
            mainExecutor,
            backgroundExecutor,
            displayMetrics
        )
    }

    fun load(): LoaderChain {
        return imageLoader?.let { LoaderChain(it) } ?: throw IllegalStateException()
    }

}