package com.tomclaw.imageloader.util

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import com.tomclaw.imageloader.core.Cancellable
import com.tomclaw.imageloader.core.ViewHolder
import com.tomclaw.imageloader.core.ViewSize

class ImageViewHolder(private val imageView: ImageView) : ViewHolder<ImageView> {

    /**
     * Waits for the view's size on the main thread, holding no thread meanwhile: a view that is
     * never laid out keeps only its own listener, which goes with the view.
     */
    override fun awaitSize(onSize: (ViewSize) -> Unit): Cancellable {
        if (hasSize()) {
            optSize()?.let(onSize)
            return Cancellable { }
        }
        return SizeWait(onSize).also { imageView.addOnLayoutChangeListener(it) }
    }

    /**
     * Whether the view has the size it is shown at: a positive one, with no layout coming. Being
     * laid out ([View.isLaidOut]) is no sign: a view taken off the window forgets it was, and a
     * list's row given back to it, sized and unchanged, is laid out no more.
     */
    private fun hasSize(): Boolean =
        !imageView.isLayoutRequested && imageView.width > 0 && imageView.height > 0

    override fun optSize(): ViewSize? {
        val width = (
                imageView.layoutParams?.width?.takeIf { it > 0 }
                    ?: imageView.width.takeIf { it > 0 }
                    ?: MATCH_PARENT
                )
            .takeIf { it != WRAP_CONTENT }
            ?: imageView.context.resources.displayMetrics.widthPixels
        val height = (
                imageView.layoutParams?.height?.takeIf { it > 0 }
                    ?: imageView.height.takeIf { it > 0 }
                    ?: MATCH_PARENT
                )
            .takeIf { it != WRAP_CONTENT }
            ?: imageView.context.resources.displayMetrics.heightPixels
        return ViewSize(width, height).takeIf { width > 0 && height > 0 }
    }

    /**
     * Waits for a layout that gives the view a size, then gives the size once the layout pass is
     * over, since what it is for may ask for another; a view that has none by then waits again.
     */
    private inner class SizeWait(
        private val onSize: (ViewSize) -> Unit
    ) : View.OnLayoutChangeListener, Runnable, Cancellable {

        override fun onLayoutChange(
            view: View,
            left: Int, top: Int, right: Int, bottom: Int,
            oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
        ) {
            if (right - left <= 0 || bottom - top <= 0) return
            view.removeOnLayoutChangeListener(this)
            mainHandler.post(this)
        }

        override fun run() {
            val size = optSize()
            if (hasSize() && size != null) {
                onSize(size)
            } else {
                imageView.addOnLayoutChangeListener(this)
            }
        }

        override fun cancel() {
            imageView.removeOnLayoutChangeListener(this)
            mainHandler.removeCallbacks(this)
        }

    }

    override var tag: Any?
        get() = imageView.tag
        set(value) {
            imageView.tag = value
        }

    override fun get(): ImageView {
        return imageView
    }

    private companion object {
        val mainHandler = Handler(Looper.getMainLooper())
    }

}
