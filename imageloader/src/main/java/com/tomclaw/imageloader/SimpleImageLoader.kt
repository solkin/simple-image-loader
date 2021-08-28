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
import com.tomclaw.imageloader.util.FileDownloader
import com.tomclaw.imageloader.util.LocalFileLoader
import java.util.concurrent.Executors

object SimpleImageLoader {

    private var imageLoader: ImageLoader? = null

    fun Context.imageLoader(cacheSize: Long = 5242880L): ImageLoader {
        return imageLoader ?: run {
            val decoder: Decoder = BitmapDecoder()
            val diskCache: DiskCache = DiskCacheImpl(DiskLruCache.create(cacheDir, cacheSize))
            val fileProvider: FileProvider = FileProviderImpl(
                cacheDir,
                diskCache,
                FileDownloader(),
                LocalFileLoader(assets)
            )
            val memoryCache: MemoryCache = MemoryCacheImpl()
            val mainExecutor = MainExecutorImpl()
            val backgroundExecutor = Executors.newFixedThreadPool(5)
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