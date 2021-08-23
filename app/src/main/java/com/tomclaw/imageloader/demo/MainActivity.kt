package com.tomclaw.imageloader.demo

import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tomclaw.imageloader.SimpleImageLoader

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val image1 = findViewById<ImageView>(R.id.image1)
        val image2 = findViewById<ImageView>(R.id.image2)
        val image3 = findViewById<ImageView>(R.id.image3)

        SimpleImageLoader.init(cacheDir, resources.displayMetrics)

        SimpleImageLoader.load()
            .from("https://zibuhoker.ru/ifm/images/image1.jpg")
            .placeholderHandler(centerResWithTint(R.drawable.ic_image, R.color.teal_700))
            .errorHandler(centerResWithTint(R.drawable.ic_image_remove, R.color.purple_500))
            .successHandler(centerCrop)
            .into(image1)
        SimpleImageLoader.load()
            .from("https://zibuhoker.ru/ifm/images/image2.jpg")
            .placeholderHandler(centerResWithTint(R.drawable.ic_image, R.color.teal_700))
            .errorHandler(centerResWithTint(R.drawable.ic_image_remove, R.color.purple_500))
            .successHandler(fitCenter)
            .into(image2)
        SimpleImageLoader.load()
            .from("https://zibuhoker.ru/ifm/images/image3.jpg")
            .placeholderHandler(centerResWithTint(R.drawable.ic_image, R.color.teal_700))
            .errorHandler(centerResWithTint(R.drawable.ic_image_remove, R.color.purple_500))
            .successHandler(centerInside)
            .into(image3)
    }

    private val fitCenter: (ImageView, Bitmap) -> Unit = { imageView, bitmap ->
        with(imageView) {
            scaleType = ImageView.ScaleType.FIT_CENTER
            colorFilter = null
            setImageBitmap(bitmap)
        }
    }

    private val centerCrop: (ImageView, Bitmap) -> Unit = { imageView, bitmap ->
        with(imageView) {
            scaleType = ImageView.ScaleType.CENTER_CROP
            colorFilter = null
            setImageBitmap(bitmap)
        }
    }

    private val centerInside: (ImageView, Bitmap) -> Unit = { imageView, bitmap ->
        with(imageView) {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            colorFilter = null
            setImageBitmap(bitmap)
        }
    }

    private val centerResWithTint = fun(drawableRes: Int, tintColor: Int): (ImageView) -> Unit {
        return {
            with(it) {
                scaleType = ImageView.ScaleType.CENTER
                setImageResource(drawableRes)
                setColorFilter(
                    ContextCompat.getColor(context, tintColor),
                    PorterDuff.Mode.MULTIPLY
                )
            }
        }
    }
}