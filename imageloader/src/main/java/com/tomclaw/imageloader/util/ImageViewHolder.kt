package com.tomclaw.imageloader.util

import android.widget.ImageView
import com.tomclaw.imageloader.core.ViewHolder
import com.tomclaw.imageloader.core.ViewSize

class ImageViewHolder(private val imageView: ImageView) : ViewHolder<ImageView> {

    override fun getSize(): ViewSize {
        val w = imageView.measuredWidth.takeIf { it > 0 }
            ?: imageView.resources.displayMetrics.widthPixels
        val h = imageView.measuredHeight.takeIf { it > 0 }
            ?: imageView.resources.displayMetrics.heightPixels
        return ViewSize(w, h)
    }

    override var tag: Any?
        get() = imageView.tag
        set(value) {
            imageView.tag = value
        }

    override fun get(): ImageView {
        return imageView
    }

}
