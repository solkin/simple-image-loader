package com.tomclaw.imageloader.util

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class ImageViewHolderTest {

    private fun imageView() = ImageView(RuntimeEnvironment.getApplication())

    private fun ImageView.layOut(width: Int, height: Int) {
        measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        layout(0, 0, width, height)
    }

    @Test
    fun `sized view with no layout coming gives its size at once`() {
        val view = imageView()
        view.layOut(100, 50)
        val sizes = mutableListOf<Pair<Int, Int>>()

        ImageViewHolder(view).awaitSize { sizes += it.width to it.height }

        assertEquals(listOf(100 to 50), sizes)
    }

    @Test
    fun `view never laid out waits without blocking, then gives its size once laid out`() {
        val view = imageView()
        val sizes = mutableListOf<Pair<Int, Int>>()

        ImageViewHolder(view).awaitSize { sizes += it.width to it.height }
        ShadowLooper.idleMainLooper()
        assertTrue("no size yet", sizes.isEmpty())

        view.layOut(100, 50)
        assertTrue("not within the layout pass", sizes.isEmpty())
        ShadowLooper.idleMainLooper()

        assertEquals(listOf(100 to 50), sizes)
    }

    @Test
    fun `layout to no size keeps waiting`() {
        val view = imageView()
        val sizes = mutableListOf<Pair<Int, Int>>()

        ImageViewHolder(view).awaitSize { sizes += it.width to it.height }
        view.layOut(0, 0)
        ShadowLooper.idleMainLooper()
        assertTrue(sizes.isEmpty())

        view.layOut(80, 80)
        ShadowLooper.idleMainLooper()
        assertEquals(listOf(80 to 80), sizes)
    }

    @Test
    fun `size is given once however often the view is laid out`() {
        val view = imageView()
        val sizes = mutableListOf<Pair<Int, Int>>()

        ImageViewHolder(view).awaitSize { sizes += it.width to it.height }
        view.layOut(100, 100)
        ShadowLooper.idleMainLooper()
        view.layOut(120, 120)
        view.layOut(140, 140)
        ShadowLooper.idleMainLooper()

        assertEquals(listOf(100 to 100), sizes)
    }

    @Test
    fun `cancelled wait gives nothing`() {
        val view = imageView()
        var called = false

        ImageViewHolder(view).awaitSize { called = true }.cancel()
        view.layOut(100, 100)
        ShadowLooper.idleMainLooper()

        assertFalse(called)
    }

    @Test
    fun `wait cancelled within the layout pass gives nothing`() {
        val view = imageView()
        var called = false

        val wait = ImageViewHolder(view).awaitSize { called = true }
        view.layOut(100, 100)
        wait.cancel()
        ShadowLooper.idleMainLooper()

        assertFalse(called)
    }

    @Test
    fun `row taken off the window and given back, sized and unchanged, gives its size at once`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val parent = FrameLayout(activity)
        activity.setContentView(parent)
        val view = imageView()
        parent.addView(view, FrameLayout.LayoutParams(100, 100))
        ShadowLooper.idleMainLooper()
        assertTrue(view.isLaidOut)

        // As a list's pool does: off the window and back, with no layout since
        parent.removeView(view)
        assertFalse("detached, it forgets it was laid out", view.isLaidOut)
        view.layOut(100, 100)
        val sizes = mutableListOf<Pair<Int, Int>>()

        ImageViewHolder(view).awaitSize { sizes += it.width to it.height }

        assertEquals(listOf(100 to 100), sizes)
    }
}
