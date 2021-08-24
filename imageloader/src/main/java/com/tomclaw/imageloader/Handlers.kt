package com.tomclaw.imageloader

import android.graphics.PorterDuff
import android.widget.ImageView

fun LoaderChain.fitCenter(): LoaderChain {
    successHandler { imageView, bitmap ->
        with(imageView) {
            scaleType = ImageView.ScaleType.FIT_CENTER
            colorFilter = null
            setImageBitmap(bitmap)
        }
    }
    return this
}

fun LoaderChain.centerCrop(): LoaderChain {
    successHandler { imageView, bitmap ->
        with(imageView) {
            scaleType = ImageView.ScaleType.CENTER_CROP
            colorFilter = null
            setImageBitmap(bitmap)
        }
    }
    return this
}

fun LoaderChain.centerInside(): LoaderChain {
    successHandler { imageView, bitmap ->
        with(imageView) {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            colorFilter = null
            setImageBitmap(bitmap)
        }
    }
    return this
}

fun LoaderChain.placeholderResWithTint(drawableRes: Int, tintColor: Int): LoaderChain {
    placeholderHandler(centerResWithTint(drawableRes, tintColor))
    return this
}

fun LoaderChain.errorResWithTint(drawableRes: Int, tintColor: Int): LoaderChain {
    errorHandler(centerResWithTint(drawableRes, tintColor))
    return this
}

private fun centerResWithTint(drawableRes: Int, tintColor: Int): (ImageView) -> Unit {
    return {
        with(it) {
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(drawableRes)
            setColorFilter(
                tintColor,
                PorterDuff.Mode.MULTIPLY
            )
        }
    }
}