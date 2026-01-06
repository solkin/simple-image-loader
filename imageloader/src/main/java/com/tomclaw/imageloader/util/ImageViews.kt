package com.tomclaw.imageloader.util

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.tomclaw.imageloader.SimpleImageLoader.imageLoader
import com.tomclaw.imageloader.core.Handlers
import com.tomclaw.imageloader.core.ImageRequest

/**
 * Loads an image into the ImageView using the new DSL.
 */
fun ImageView.fetch(uri: Uri, config: ImageRequest<ImageView>.() -> Unit = {}) {
    fetch(uri.toString(), config)
}

/**
 * Loads an image into the ImageView using the new DSL.
 *
 * Example:
 * ```
 * imageView.fetch(url) {
 *     centerCrop()
 *     crossfade()
 *     placeholder(R.drawable.loading)
 *     error(R.drawable.error)
 *     onSuccess { view, drawable -> }
 *     onError { view, throwable -> }
 * }
 * ```
 */
fun ImageView.fetch(url: String, config: ImageRequest<ImageView>.() -> Unit = {}) {
    val request = ImageRequest<ImageView>().apply(config)
    val handlers = request.toHandlers(this)
    val viewHolder = ImageViewHolder(this)
    context.imageLoader().load(viewHolder, url, handlers)
}

/**
 * Loads an image using a pre-configured ImageRequest.
 *
 * Example:
 * ```
 * val avatarConfig = imageRequest<ImageView> {
 *     circleCrop()
 *     crossfade()
 *     placeholder(R.drawable.avatar_placeholder)
 * }
 *
 * imageView.fetch(url, avatarConfig)
 * ```
 */
fun ImageView.fetch(url: String, request: ImageRequest<ImageView>) {
    val handlers = request.toHandlers(this)
    val viewHolder = ImageViewHolder(this)
    context.imageLoader().load(viewHolder, url, handlers)
}

/**
 * Converts ImageRequest to Handlers for backward compatibility.
 */
private fun ImageRequest<ImageView>.toHandlers(imageView: ImageView): Handlers<ImageView> {
    val request = this
    return Handlers<ImageView>().apply {
        // Success handler
        successHandler { viewHolder, result ->
            val view = viewHolder.get()
            var drawable = result.getDrawable()

            // Apply transformations
            if (request.transformations.isNotEmpty() && drawable is BitmapDrawable) {
                var bitmap = drawable.bitmap
                for (transformation in request.transformations) {
                    bitmap = transformation.transform(bitmap)
                }
                drawable = BitmapDrawable(view.resources, bitmap)
            }

            // Apply scale type
            request.scaleType?.let { scaleType ->
                view.scaleType = when (scaleType) {
                    ImageRequest.ScaleType.CENTER_CROP -> ImageView.ScaleType.CENTER_CROP
                    ImageRequest.ScaleType.FIT_CENTER -> ImageView.ScaleType.FIT_CENTER
                    ImageRequest.ScaleType.CENTER_INSIDE -> ImageView.ScaleType.CENTER_INSIDE
                }
            }

            // Clear color filter and set image
            view.colorFilter = null
            view.setImageDrawable(drawable)

            // Apply crossfade animation
            if (request.crossfadeDuration > 0) {
                view.alpha = 0f
                view.animate()
                    .alpha(1f)
                    .setDuration(request.crossfadeDuration.toLong())
                    .start()
            }

            // Call user callback
            request.onSuccess(view, drawable)
        }

        // Placeholder handler
        placeholderHandler { viewHolder ->
            val view = viewHolder.get()
            request.placeholderRes?.let { res ->
                view.scaleType = ImageView.ScaleType.CENTER
                view.setImageResource(res)
            }
            request.placeholderDrawable?.let { drawable ->
                view.scaleType = ImageView.ScaleType.CENTER
                view.setImageDrawable(drawable)
            }
            view.colorFilter = null
            request.onLoading(view)
        }

        // Error handler
        errorHandler { viewHolder ->
            val view = viewHolder.get()
            request.errorRes?.let { res ->
                view.scaleType = ImageView.ScaleType.CENTER
                view.setImageResource(res)
            }
            request.errorDrawable?.let { drawable ->
                view.scaleType = ImageView.ScaleType.CENTER
                view.setImageDrawable(drawable)
            }
            view.colorFilter = null
            request.onError(view, null)
        }
    }
}

