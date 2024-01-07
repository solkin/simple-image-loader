package com.tomclaw.imageloader.util

import android.net.Uri
import android.widget.ImageView
import com.tomclaw.imageloader.SimpleImageLoader.imageLoader
import com.tomclaw.imageloader.core.Handlers

fun ImageView.fetch(uri: Uri, params: Handlers<ImageView>.() -> Unit = {}) {
    fetch(uri.toString(), params)
}

fun ImageView.fetch(url: String, params: Handlers<ImageView>.() -> Unit = {}) {
    val handlers = Handlers<ImageView>()
        .apply {
            successHandler { viewHolder, result ->
                viewHolder.get().setImageDrawable(result.getDrawable())
            }
        }
        .apply(params)
    val viewHolder = ImageViewHolder(this)
    context.imageLoader().load(viewHolder, url, handlers)
}
