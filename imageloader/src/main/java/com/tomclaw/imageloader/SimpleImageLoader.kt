package com.tomclaw.imageloader

import android.content.Context
import com.tomclaw.cache.DiskLruCache
import com.tomclaw.imageloader.core.Decoder
import com.tomclaw.imageloader.core.DiskCache
import com.tomclaw.imageloader.core.DiskCacheImpl
import com.tomclaw.imageloader.core.FileProvider
import com.tomclaw.imageloader.core.FileProviderImpl
import com.tomclaw.imageloader.core.ImageLoader
import com.tomclaw.imageloader.core.ImageLoaderImpl
import com.tomclaw.imageloader.core.MainExecutorImpl
import com.tomclaw.imageloader.core.MemoryCache
import com.tomclaw.imageloader.core.MemoryCacheImpl
import com.tomclaw.imageloader.util.BitmapDecoder
import com.tomclaw.imageloader.util.loader.ContentLoader
import com.tomclaw.imageloader.util.loader.UrlLoader
import com.tomclaw.imageloader.util.loader.FileLoader
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object SimpleImageLoader {

    private var imageLoader: ImageLoader? = null

    fun Context.imageLoader(
        decoder: Decoder = BitmapDecoder(),
        fileProvider: FileProvider = FileProviderImpl(
            cacheDir,
            DiskCacheImpl(DiskLruCache.create(cacheDir, 15728640L)),
            UrlLoader(),
            FileLoader(assets),
            ContentLoader(contentResolver)
        ),
        memoryCache: MemoryCache = MemoryCacheImpl(),
        mainExecutor: Executor = MainExecutorImpl(),
        backgroundExecutor: ExecutorService = Executors.newFixedThreadPool(10)
    ): ImageLoader {
        return imageLoader ?: run {
            val loader = ImageLoaderImpl(
                fileProvider,
                decoder,
                memoryCache,
                mainExecutor,
                backgroundExecutor
            )
            imageLoader = loader
            return loader
        }
    }

}