package com.tomclaw.imageloader.util

import android.content.res.AssetManager
import com.tomclaw.imageloader.core.FileLoader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI

class LocalFileLoader(private val assets: AssetManager?) : FileLoader {

    override val schemes: List<String>
        get() = listOf("file")

    override fun load(uri: String, file: File): Boolean {
        val u = URI(uri)
        val source = if (u.path.startsWith(ASSET_PREFIX)) {
            assets?.open(u.path.replace(ASSET_PREFIX, ""))
        } else {
            val sourceFile = File(u)
            FileInputStream(sourceFile)
        }
        val destination = FileOutputStream(file)
        return source?.safeCopyTo(destination) ?: false
    }

}

private const val ASSET_PREFIX = "/android_asset/"
