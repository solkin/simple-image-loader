package com.tomclaw.imageloader

import android.content.Context
import com.tomclaw.imageloader.core.Decoder
import com.tomclaw.imageloader.core.DiskCache
import com.tomclaw.imageloader.core.FileProvider
import com.tomclaw.imageloader.core.FileProviderImpl
import com.tomclaw.imageloader.core.ImageLoader
import com.tomclaw.imageloader.core.ImageLoaderImpl
import com.tomclaw.imageloader.core.ImageRepository
import com.tomclaw.imageloader.core.ImageRepositoryImpl
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

    @Volatile
    private var imageLoader: ImageLoader? = null

    private const val DEFAULT_DISK_CACHE_SIZE = 15728640L

    /**
     * Returns the singleton ImageLoader instance.
     * Initializes with default configuration if not already initialized.
     * Called from both the main thread (view binding) and background threads (size
     * waiting), so initialization is guarded to run once.
     */
    fun Context.imageLoader(): ImageLoader {
        return imageLoader ?: synchronized(this@SimpleImageLoader) {
            imageLoader ?: initImageLoader()
        }
    }

    /**
     * Returns the ImageRepository for direct access to loading/caching.
     * Useful for Compose or custom UI frameworks.
     */
    fun Context.imageRepository(): ImageRepository {
        return imageLoader().repository
    }

    /**
     * Initializes the ImageLoader with custom configuration.
     * Call this before first use if you need custom settings.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun Context.initImageLoader(
        decoders: List<Decoder> = listOf(BitmapDecoder()),
        fileProvider: FileProvider = FileProviderImpl(
            cacheDir,
            DiskCache.create(cacheDir, DEFAULT_DISK_CACHE_SIZE),
            UrlLoader(),
            FileLoader(assets),
            ContentLoader(contentResolver)
        ),
        memoryCache: MemoryCache = MemoryCacheImpl(),
        mainExecutor: Executor = MainExecutorImpl(),
        backgroundExecutor: ExecutorService = Executors.newFixedThreadPool(10)
    ): ImageLoader {
        val repository = ImageRepositoryImpl(
            fileProvider,
            decoders,
            memoryCache,
            backgroundExecutor
        )
        val loader = ImageLoaderImpl(
            repository,
            mainExecutor,
            backgroundExecutor
        )
        imageLoader = loader
        return loader
    }

}