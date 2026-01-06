package com.tomclaw.imageloader.core

import android.graphics.drawable.Drawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class ImageRequestTest {

    @Test
    fun `default request has no configuration`() {
        val request = ImageRequest<Any>()

        assertNull(request.scaleType)
        assertNull(request.placeholderRes)
        assertNull(request.errorRes)
        assertEquals(0, request.crossfadeDuration)
        assertTrue(request.transformations.isEmpty())
    }

    @Test
    fun `centerCrop sets scale type`() {
        val request = ImageRequest<Any>().centerCrop()

        assertEquals(ImageRequest.ScaleType.CENTER_CROP, request.scaleType)
    }

    @Test
    fun `fitCenter sets scale type`() {
        val request = ImageRequest<Any>().fitCenter()

        assertEquals(ImageRequest.ScaleType.FIT_CENTER, request.scaleType)
    }

    @Test
    fun `centerInside sets scale type`() {
        val request = ImageRequest<Any>().centerInside()

        assertEquals(ImageRequest.ScaleType.CENTER_INSIDE, request.scaleType)
    }

    @Test
    fun `crossfade sets duration`() {
        val request = ImageRequest<Any>().crossfade(500)

        assertEquals(500, request.crossfadeDuration)
    }

    @Test
    fun `crossfade uses default duration`() {
        val request = ImageRequest<Any>().crossfade()

        assertEquals(300, request.crossfadeDuration)
    }

    @Test
    fun `placeholder sets resource id`() {
        val request = ImageRequest<Any>().placeholder(123)

        assertEquals(123, request.placeholderRes)
        assertNull(request.placeholderDrawable)
    }

    @Test
    fun `placeholder with drawable clears resource id`() {
        val drawable = mock<Drawable>()
        val request = ImageRequest<Any>()
            .placeholder(123)
            .placeholder(drawable)

        assertNull(request.placeholderRes)
        assertEquals(drawable, request.placeholderDrawable)
    }

    @Test
    fun `error sets resource id`() {
        val request = ImageRequest<Any>().error(456)

        assertEquals(456, request.errorRes)
    }

    @Test
    fun `size sets target size`() {
        val request = ImageRequest<Any>().size(200, 300)

        assertEquals(200, request.targetSize?.width)
        assertEquals(300, request.targetSize?.height)
    }

    @Test
    fun `memoryCache sets policy`() {
        val request = ImageRequest<Any>().memoryCache(CachePolicy.DISABLED)

        assertEquals(CachePolicy.DISABLED, request.memoryCachePolicy)
    }

    @Test
    fun `memoryCache with boolean`() {
        val request = ImageRequest<Any>().memoryCache(enabled = false)

        assertEquals(CachePolicy.DISABLED, request.memoryCachePolicy)
    }

    @Test
    fun `diskCache sets policy`() {
        val request = ImageRequest<Any>().diskCache(CachePolicy.READ_ONLY)

        assertEquals(CachePolicy.READ_ONLY, request.diskCachePolicy)
    }

    @Test
    fun `transform adds transformation`() {
        val transformation = mock<Transformation>()
        val request = ImageRequest<Any>().transform(transformation)

        assertEquals(1, request.transformations.size)
        assertEquals(transformation, request.transformations[0])
    }

    @Test
    fun `transform block adds multiple transformations`() {
        val request = ImageRequest<Any>().apply {
            transform {
                circleCrop()
                grayscale()
            }
        }

        assertEquals(2, request.transformations.size)
    }

    @Test
    fun `onSuccess composes handlers`() {
        var count = 0
        val request = ImageRequest<Any>()
            .onSuccess { _, _ -> count++ }
            .onSuccess { _, _ -> count++ }

        request.onSuccess(mock(), mock())

        assertEquals(2, count)
    }

    @Test
    fun `onError composes handlers`() {
        var count = 0
        val request = ImageRequest<Any>()
            .onError { _, _ -> count++ }
            .onError { _, _ -> count++ }

        request.onError(mock(), null)

        assertEquals(2, count)
    }

    @Test
    fun `onLoading composes handlers`() {
        var count = 0
        val request = ImageRequest<Any>()
            .onLoading { count++ }
            .onLoading { count++ }

        request.onLoading(mock())

        assertEquals(2, count)
    }

    @Test
    fun `copy creates independent copy`() {
        val original = ImageRequest<Any>()
            .centerCrop()
            .crossfade(500)
            .placeholder(123)

        val copy = original.copy()
        copy.fitCenter()

        assertEquals(ImageRequest.ScaleType.CENTER_CROP, original.scaleType)
        assertEquals(ImageRequest.ScaleType.FIT_CENTER, copy.scaleType)
    }

    @Test
    fun `imageRequest builder creates configured request`() {
        val request = imageRequest<Any> {
            centerCrop()
            crossfade()
            placeholder(123)
        }

        assertEquals(ImageRequest.ScaleType.CENTER_CROP, request.scaleType)
        assertEquals(300, request.crossfadeDuration)
        assertEquals(123, request.placeholderRes)
    }

    @Test
    fun `chained configuration works correctly`() {
        val request = ImageRequest<Any>()
            .centerCrop()
            .crossfade(400)
            .placeholder(111)
            .error(222)
            .memoryCache(CachePolicy.DISABLED)
            .diskCache(enabled = false)
            .size(100, 200)

        assertEquals(ImageRequest.ScaleType.CENTER_CROP, request.scaleType)
        assertEquals(400, request.crossfadeDuration)
        assertEquals(111, request.placeholderRes)
        assertEquals(222, request.errorRes)
        assertEquals(CachePolicy.DISABLED, request.memoryCachePolicy)
        assertEquals(CachePolicy.DISABLED, request.diskCachePolicy)
        assertEquals(100, request.targetSize?.width)
        assertEquals(200, request.targetSize?.height)
    }
}

