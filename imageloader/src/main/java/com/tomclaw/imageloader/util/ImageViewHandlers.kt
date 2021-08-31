package com.tomclaw.imageloader.util

import android.graphics.PorterDuff
import android.widget.ImageView
import com.tomclaw.imageloader.core.Handlers
import com.tomclaw.imageloader.core.ViewHolder

fun Handlers<ImageView>.fitCenter() = apply {
    successHandler { viewHolder, result ->
        with(viewHolder.get()) {
            setImageDrawable(null)
            scaleType = ImageView.ScaleType.FIT_CENTER
            colorFilter = null
            setImageDrawable(result.getDrawable())
        }
    }
    return this
}

fun Handlers<ImageView>.centerCrop() = apply {
    successHandler { viewHolder, result ->
        with(viewHolder.get()) {
            setImageDrawable(null)
            scaleType = ImageView.ScaleType.CENTER_CROP
            colorFilter = null
            setImageDrawable(result.getDrawable())
        }
    }
    return this
}

fun Handlers<ImageView>.centerInside() = apply {
    successHandler { viewHolder, result ->
        with(viewHolder.get()) {
            setImageDrawable(null)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            colorFilter = null
            setImageDrawable(result.getDrawable())
        }
    }
}

fun Handlers<ImageView>.withPlaceholder(drawableRes: Int) = apply {
    placeholderHandler {
        it.centerRes(drawableRes)
        it.get().colorFilter = null
    }
}

fun Handlers<ImageView>.withPlaceholder(drawableRes: Int, tintColor: Int) = apply {
    placeholderHandler {
        with(it) {
            centerRes(drawableRes)
            tint(tintColor)
        }
    }
}

fun Handlers<ImageView>.whenError(drawableRes: Int, tintColor: Int) = apply {
    errorHandler {
        with(it) {
            centerRes(drawableRes)
            tint(tintColor)
        }
    }
}

private fun ViewHolder<ImageView>.centerRes(drawableRes: Int) {
    with(get()) {
        scaleType = ImageView.ScaleType.CENTER
        setImageResource(drawableRes)
    }
}

private fun ViewHolder<ImageView>.tint(tintColor: Int) {
    with(get()) {
        setColorFilter(
            tintColor,
            PorterDuff.Mode.MULTIPLY
        )
    }
}