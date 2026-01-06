package com.tomclaw.imageloader.core

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes

/**
 * DSL marker to prevent scope leakage in nested lambdas.
 */
@DslMarker
annotation class ImageLoaderDsl

/**
 * Cache policy for memory and disk caching.
 */
enum class CachePolicy {
    ENABLED,
    DISABLED,
    READ_ONLY,
    WRITE_ONLY
}

/**
 * Transformation to apply to loaded images.
 */
interface Transformation {
    fun transform(bitmap: Bitmap): Bitmap
    val key: String
}

/**
 * Builder for image loading requests.
 * Supports fluent DSL syntax with composable handlers.
 */
@ImageLoaderDsl
class ImageRequest<T> {

    // Handlers
    internal var onSuccess: (T, Drawable) -> Unit = { _, _ -> }
        private set
    internal var onError: (T, Throwable?) -> Unit = { _, _ -> }
        private set
    internal var onLoading: (T) -> Unit = {}
        private set

    // Configuration
    internal var placeholderRes: Int? = null
        private set
    internal var placeholderDrawable: Drawable? = null
        private set
    internal var errorRes: Int? = null
        private set
    internal var errorDrawable: Drawable? = null
        private set
    internal var transformations: MutableList<Transformation> = mutableListOf()
        private set
    internal var crossfadeDuration: Int = 0
        private set
    internal var scaleType: ScaleType? = null
        private set
    internal var targetSize: Size? = null
        private set
    internal var memoryCachePolicy: CachePolicy = CachePolicy.ENABLED
        private set
    internal var diskCachePolicy: CachePolicy = CachePolicy.ENABLED
        private set

    /**
     * Scale type for the loaded image.
     */
    enum class ScaleType {
        CENTER_CROP,
        FIT_CENTER,
        CENTER_INSIDE
    }

    /**
     * Target size for image loading.
     */
    data class Size(val width: Int, val height: Int) {
        companion object {
            val ORIGINAL = Size(-1, -1)
        }
    }

    // ==================== Scale Type ====================

    fun scaleType(type: ScaleType) = apply {
        this.scaleType = type
    }

    fun centerCrop() = scaleType(ScaleType.CENTER_CROP)

    fun fitCenter() = scaleType(ScaleType.FIT_CENTER)

    fun centerInside() = scaleType(ScaleType.CENTER_INSIDE)

    // ==================== Placeholder ====================

    fun placeholder(@DrawableRes drawableRes: Int) = apply {
        this.placeholderRes = drawableRes
        this.placeholderDrawable = null
    }

    fun placeholder(drawable: Drawable) = apply {
        this.placeholderDrawable = drawable
        this.placeholderRes = null
    }

    // ==================== Error ====================

    fun error(@DrawableRes drawableRes: Int) = apply {
        this.errorRes = drawableRes
        this.errorDrawable = null
    }

    fun error(drawable: Drawable) = apply {
        this.errorDrawable = drawable
        this.errorRes = null
    }

    // ==================== Animation ====================

    fun crossfade(durationMs: Int = 300) = apply {
        this.crossfadeDuration = durationMs
    }

    // ==================== Size ====================

    fun size(width: Int, height: Int) = apply {
        this.targetSize = Size(width, height)
    }

    fun size(size: Size) = apply {
        this.targetSize = size
    }

    // ==================== Transformations ====================

    fun transform(transformation: Transformation) = apply {
        this.transformations.add(transformation)
    }

    fun transform(block: TransformationBuilder.() -> Unit) = apply {
        TransformationBuilder(this.transformations).apply(block)
    }

    // ==================== Transformation Shortcuts ====================

    /**
     * Crops the image to a circle.
     */
    fun circleCrop() = apply {
        transformations.add(CircleCropTransformation())
    }

    /**
     * Rounds the corners of the image.
     */
    fun roundedCorners(radiusPx: Float) = apply {
        transformations.add(RoundedCornersTransformation(radiusPx.toInt()))
    }

    /**
     * Converts the image to grayscale.
     */
    fun grayscale() = apply {
        transformations.add(GrayscaleTransformation())
    }

    /**
     * Applies blur to the image.
     */
    fun blur(radius: Float = 25f) = apply {
        transformations.add(BlurTransformation(radius.toInt()))
    }

    // ==================== Cache Policy ====================

    fun memoryCache(policy: CachePolicy) = apply {
        this.memoryCachePolicy = policy
    }

    fun memoryCache(enabled: Boolean) = apply {
        this.memoryCachePolicy = if (enabled) CachePolicy.ENABLED else CachePolicy.DISABLED
    }

    fun diskCache(policy: CachePolicy) = apply {
        this.diskCachePolicy = policy
    }

    fun diskCache(enabled: Boolean) = apply {
        this.diskCachePolicy = if (enabled) CachePolicy.ENABLED else CachePolicy.DISABLED
    }

    // ==================== Callbacks ====================

    fun onSuccess(handler: (T, Drawable) -> Unit) = apply {
        val previous = this.onSuccess
        this.onSuccess = { view, drawable ->
            previous(view, drawable)
            handler(view, drawable)
        }
    }

    fun onError(handler: (T, Throwable?) -> Unit) = apply {
        val previous = this.onError
        this.onError = { view, error ->
            previous(view, error)
            handler(view, error)
        }
    }

    fun onLoading(handler: (T) -> Unit) = apply {
        val previous = this.onLoading
        this.onLoading = { view ->
            previous(view)
            handler(view)
        }
    }

    // ==================== Builder for reuse ====================

    fun copy(): ImageRequest<T> {
        return ImageRequest<T>().also {
            it.onSuccess = this.onSuccess
            it.onError = this.onError
            it.onLoading = this.onLoading
            it.placeholderRes = this.placeholderRes
            it.placeholderDrawable = this.placeholderDrawable
            it.errorRes = this.errorRes
            it.errorDrawable = this.errorDrawable
            it.transformations = this.transformations.toMutableList()
            it.crossfadeDuration = this.crossfadeDuration
            it.scaleType = this.scaleType
            it.targetSize = this.targetSize
            it.memoryCachePolicy = this.memoryCachePolicy
            it.diskCachePolicy = this.diskCachePolicy
        }
    }
}

/**
 * Builder for chaining transformations.
 */
@ImageLoaderDsl
class TransformationBuilder(private val transformations: MutableList<Transformation>) {

    fun circleCrop() {
        transformations.add(CircleCropTransformation())
    }

    fun rounded(radiusPx: Int) {
        transformations.add(RoundedCornersTransformation(radiusPx))
    }

    fun grayscale() {
        transformations.add(GrayscaleTransformation())
    }

    fun blur(radius: Int = 25) {
        transformations.add(BlurTransformation(radius))
    }

    fun custom(transformation: Transformation) {
        transformations.add(transformation)
    }
}

/**
 * Creates an ImageRequest for reuse.
 */
fun <T> imageRequest(block: ImageRequest<T>.() -> Unit): ImageRequest<T> {
    return ImageRequest<T>().apply(block)
}

