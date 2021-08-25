package com.tomclaw.imageloader.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import com.tomclaw.imageloader.core.Decoder
import com.tomclaw.imageloader.core.Result
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.max

class BitmapDecoder : Decoder {

    override fun decode(
        file: File,
        width: Int,
        height: Int
    ): Result? {
        var bitmap: Bitmap?
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(file)
            bitmap = decodeSampledBitmapFromStream(inputStream, width, height)
            val rotation = getRotation(file)
            if (bitmap != null && rotation != 0) {
                val m = Matrix()
                m.setRotate(
                    rotation.toFloat(),
                    (bitmap.width / 2).toFloat(),
                    (bitmap.height / 2).toFloat()
                )
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, false)
            }
        } catch (ignored: Throwable) {
            ignored.printStackTrace()
            bitmap = null
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (ignored: IOException) {
                }
            }
        }
        return bitmap?.let { BitmapResult(it) }
    }

    private fun decodeSampledBitmapFromStream(
        stream: InputStream,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        var bitmap: Bitmap?
        try {
            val inputStream: InputStream = BufferedInputStream(stream, THUMBNAIL_BUFFER_SIZE)
            inputStream.mark(THUMBNAIL_BUFFER_SIZE)

            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)

            // Calculate inSampleSize
            val widthSample = (options.outWidth / reqWidth).toFloat()
            val heightSample = (options.outHeight / reqHeight).toFloat()
            var scaleFactor = max(widthSample, heightSample)
            if (scaleFactor < 1) {
                scaleFactor = 1f
            }
            options.inJustDecodeBounds = false
            options.inSampleSize = scaleFactor.toInt()
            options.inPreferredConfig = Bitmap.Config.RGB_565

            // Decode bitmap with inSampleSize set
            inputStream.reset()
            bitmap = BitmapFactory.decodeStream(inputStream, null, options)
        } catch (ignored: Throwable) {
            ignored.printStackTrace()
            bitmap = null
        }
        return bitmap
    }

    private fun getRotation(file: File): Int {
        return when (obtainFileOrientation(file.absolutePath)) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90
            ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270
            else -> 0
        }
    }

    private fun obtainFileOrientation(fileName: String): Int {
        return try {
            val exifInterface = ExifInterface(fileName)
            exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (ex: IOException) {
            ExifInterface.ORIENTATION_UNDEFINED
        }
    }

}

class BitmapResult(private val bitmap: Bitmap) : Result {

    override fun getByteCount() = bitmap.byteCount

    override fun isRecycled() = bitmap.isRecycled

    override fun getDrawable(): Drawable {
        return BitmapDrawable(null, bitmap)
    }

}

/**
 * Buffer is large enough to rewind past any EXIF headers.
 */
private const val THUMBNAIL_BUFFER_SIZE = 128 * 1024