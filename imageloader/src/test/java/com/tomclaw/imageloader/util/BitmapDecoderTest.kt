package com.tomclaw.imageloader.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BitmapDecoderTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `decodes a small JPEG down to the size asked`() {
        val file = jpeg(width = 400, height = 300, headerBytes = 0)

        val result = BitmapDecoder().decode(file, 100, 100)

        assertNotNull(result)
        val bitmap = (result!!.getDrawable() as BitmapDrawable).bitmap
        assertEquals(100, bitmap.width)
        assertEquals(75, bitmap.height)
    }

    @Test
    fun `decodes a JPEG whose headers run far past the first bytes`() {
        // Cameras put EXIF, XMP, maker notes and gain-map headers before the picture: past 128 KiB here.
        val file = jpeg(width = 400, height = 300, headerBytes = 400 * 1024)

        val result = BitmapDecoder().decode(file, 100, 100)

        assertNotNull(result)
        assertEquals(100, (result!!.getDrawable() as BitmapDrawable).bitmap.width)
    }

    /** A JPEG with application segments of [headerBytes] in all between its start and the picture. */
    private fun jpeg(width: Int, height: Int, headerBytes: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(240, 128, 0))
        val encoded = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        val out = ByteArrayOutputStream()
        out.write(encoded, 0, 2) // SOI
        var left = headerBytes
        while (left > 0) {
            val payload = minOf(left, 65533)
            out.write(0xFF)
            out.write(0xE9) // APP9, which no decoder reads
            out.write((payload + 2) shr 8)
            out.write((payload + 2) and 0xFF)
            out.write(ByteArray(payload))
            left -= payload
        }
        out.write(encoded, 2, encoded.size - 2)
        return folder.newFile("photo.jpg").apply { writeBytes(out.toByteArray()) }
    }
}
