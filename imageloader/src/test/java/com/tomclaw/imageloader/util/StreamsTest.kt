package com.tomclaw.imageloader.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class StreamsTest {

    @Test
    fun `safeCopyTo copies data successfully`() {
        val data = "Hello, World!".toByteArray()
        val input = ByteArrayInputStream(data)
        val output = ByteArrayOutputStream()

        val result = input.safeCopyTo(output)

        assertTrue(result)
        assertEquals("Hello, World!", output.toString())
    }

    @Test
    fun `safeCopyTo returns false on exception`() {
        val input = ByteArrayInputStream("data".toByteArray())
        val failingOutput = object : OutputStream() {
            override fun write(b: Int) {
                throw IOException("Write failed")
            }
        }

        val result = input.safeCopyTo(failingOutput)

        assertFalse(result)
    }

    @Test
    fun `safeCopyTo handles large data`() {
        val largeData = ByteArray(100_000) { it.toByte() }
        val input = ByteArrayInputStream(largeData)
        val output = ByteArrayOutputStream()

        val result = input.safeCopyTo(output)

        assertTrue(result)
        assertEquals(largeData.size, output.size())
    }

    @Test
    fun `copyTo copies all bytes`() {
        val data = ByteArray(50_000) { (it % 256).toByte() }
        val input = ByteArrayInputStream(data)
        val output = ByteArrayOutputStream()

        input.copyTo(output)

        assertTrue(data.contentEquals(output.toByteArray()))
    }

    @Test
    fun `safeClose does not throw on null`() {
        val closeable: Closeable? = null
        closeable.safeClose() // Should not throw
    }

    @Test
    fun `safeClose closes the stream`() {
        var closed = false
        val closeable = Closeable { closed = true }

        closeable.safeClose()

        assertTrue(closed)
    }

    @Test
    fun `safeClose ignores IOException`() {
        val failingCloseable = Closeable {
            throw IOException("Close failed")
        }

        failingCloseable.safeClose() // Should not throw
    }

    @Test
    fun `copyTo respects thread interruption`() {
        val infiniteInput = object : InputStream() {
            private var count = 0
            override fun read(): Int {
                if (count++ > 1000) {
                    Thread.currentThread().interrupt()
                }
                return 42
            }
        }
        val output = ByteArrayOutputStream()

        var interrupted = false
        try {
            infiniteInput.copyTo(output)
        } catch (e: Exception) {
            interrupted = true
        }

        assertTrue(interrupted)
    }
}

