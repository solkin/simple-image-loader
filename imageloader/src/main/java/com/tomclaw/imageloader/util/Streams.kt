package com.tomclaw.imageloader.util

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.io.OutputStream

fun InputStream.safeCopyTo(output: OutputStream): Boolean {
    return try {
        copyTo(output); true
    } catch (ex: Throwable) {
        ex.printStackTrace(); false
    }
}

@Throws(IOException::class)
fun InputStream.copyTo(output: OutputStream) {
    val buffer = ByteArray(10240)
    var read: Int
    while (this.read(buffer).also { read = it } != -1) {
        output.write(buffer, 0, read)
        if (Thread.interrupted()) {
            throw InterruptedIOException()
        }
    }
}

fun Closeable?.safeClose() {
    try {
        this?.close()
    } catch (ignored: IOException) {
    }
}

