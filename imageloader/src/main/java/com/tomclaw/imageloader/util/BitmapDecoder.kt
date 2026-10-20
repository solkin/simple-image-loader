package com.tomclaw.imageloader.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.ORIENTATION_FLIP_VERTICAL
import androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
import androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSPOSE
import androidx.exifinterface.media.ExifInterface.ORIENTATION_TRANSVERSE
import androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED
import androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION
import com.tomclaw.imageloader.core.Decoder
import com.tomclaw.imageloader.core.Result
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.max

class BitmapDecoder : Decoder {

    override fun probe(file: File): Boolean {
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(file)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            return true
        } catch (ignored: Throwable) {
            ignored.printStackTrace()
        } finally {
            inputStream.safeClose()
        }
        return false
    }

    override fun decode(
        file: File,
        width: Int,
        height: Int
    ): Result? {
        var bitmap: Bitmap?
        try {
            bitmap = decodeSampledBitmap(file, width, height)
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
        }
        return bitmap?.let { BitmapResult(it) }
    }

    /**
     * Reads the picture's size, then decodes it as small as [reqWidth] by [reqHeight] allows, each
     * from the file afresh: how far reading the size goes depends on the file's headers alone —
     * past EXIF, XMP and maker notes, through a whole HEIF — so no stream is rewound to it.
     */
    private fun decodeSampledBitmap(
        file: File,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        file.decodeWith(options)
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            return null
        }

        val widthSample = (options.outWidth / reqWidth).toFloat()
        val heightSample = (options.outHeight / reqHeight).toFloat()
        var scaleFactor = max(widthSample, heightSample)
        if (scaleFactor < 1) {
            scaleFactor = 1f
        }
        options.inJustDecodeBounds = false
        options.inSampleSize = scaleFactor.toInt()
        options.inPreferredConfig = Bitmap.Config.RGB_565
        return file.decodeWith(options)
    }

    private fun File.decodeWith(options: BitmapFactory.Options): Bitmap? =
        BufferedInputStream(FileInputStream(this), STREAM_BUFFER_SIZE).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

    private fun getRotation(file: File): Int {
        return when (obtainFileOrientation(file.absolutePath)) {
            ORIENTATION_ROTATE_90, ORIENTATION_TRANSPOSE -> 90
            ORIENTATION_ROTATE_180, ORIENTATION_FLIP_VERTICAL -> 180
            ORIENTATION_ROTATE_270, ORIENTATION_TRANSVERSE -> 270
            else -> 0
        }
    }

    private fun obtainFileOrientation(fileName: String): Int {
        return try {
            val exifInterface = ExifInterface(fileName)
            exifInterface.getAttributeInt(
                TAG_ORIENTATION,
                ORIENTATION_NORMAL
            )
        } catch (ex: IOException) {
            ORIENTATION_UNDEFINED
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

private const val STREAM_BUFFER_SIZE = 64 * 1024
