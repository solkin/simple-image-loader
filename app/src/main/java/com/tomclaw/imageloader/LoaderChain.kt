package com.tomclaw.imageloader

import android.graphics.Bitmap
import android.widget.ImageView

class LoaderChain(
    private val imageLoader: ImageLoader
) {

    private var url: String? = null
    private var success: (ImageView, Bitmap) -> Unit = { imageView, bitmap ->
        imageView.setImageBitmap(bitmap)
    }
    private var placeholder: (ImageView) -> Unit = {}
    private var error: (ImageView) -> Unit = {}

    fun from(url: String): LoaderChain {
        this.url = url
        return this
    }

    fun placeholderHandler(placeholder: (ImageView) -> Unit): LoaderChain {
        this.placeholder = placeholder
        return this
    }

    fun errorHandler(error: (ImageView) -> Unit): LoaderChain {
        this.error = error
        return this
    }

    fun successHandler(success: (ImageView, Bitmap) -> Unit): LoaderChain {
        this.success = success
        return this
    }

    fun into(imageView: ImageView) {
        val url = url ?: throw IllegalArgumentException("URL must be specified")
        imageLoader.load(imageView, url, success, placeholder, error)
    }

}