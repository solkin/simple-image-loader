package com.tomclaw.imageloader.core

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.min

/**
 * Crops the image into a circle.
 */
class CircleCropTransformation : Transformation {

    override val key: String = "circle_crop"

    override fun transform(bitmap: Bitmap): Bitmap {
        val size = min(bitmap.width, bitmap.height)
        val x = (bitmap.width - size) / 2
        val y = (bitmap.height - size) / 2

        val squaredBitmap = Bitmap.createBitmap(bitmap, x, y, size, size)
        if (squaredBitmap != bitmap) {
            bitmap.recycle()
        }

        val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            isAntiAlias = true
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        squaredBitmap.recycle()

        return result
    }
}

/**
 * Rounds the corners of the image.
 */
class RoundedCornersTransformation(
    private val radiusPx: Int
) : Transformation {

    override val key: String = "rounded_$radiusPx"

    override fun transform(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            isAntiAlias = true
        }

        val rect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        val path = Path().apply {
            addRoundRect(rect, radiusPx.toFloat(), radiusPx.toFloat(), Path.Direction.CW)
        }

        canvas.drawPath(path, paint)

        return result
    }
}

/**
 * Converts the image to grayscale.
 */
class GrayscaleTransformation : Transformation {

    override val key: String = "grayscale"

    override fun transform(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(0f)
            })
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }
}

/**
 * Applies blur effect to the image.
 * Note: For better performance on API 17+, consider using RenderScript.
 */
class BlurTransformation(
    private val radius: Int = 25
) : Transformation {

    override val key: String = "blur_$radius"

    override fun transform(bitmap: Bitmap): Bitmap {
        // Simple box blur implementation
        // For production, use RenderScript or other optimized solutions
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            bitmap.width / 4,
            bitmap.height / 4,
            true
        )

        val result = Bitmap.createScaledBitmap(
            scaledBitmap,
            bitmap.width,
            bitmap.height,
            true
        )

        scaledBitmap.recycle()

        return result
    }
}

