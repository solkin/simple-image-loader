package com.tomclaw.imageloader.demo

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tomclaw.imageloader.SimpleImageLoader
import com.tomclaw.imageloader.centerCrop
import com.tomclaw.imageloader.centerInside
import com.tomclaw.imageloader.errorResWithTint
import com.tomclaw.imageloader.fitCenter
import com.tomclaw.imageloader.placeholderResWithTint

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
            .placeholderResWithTint(R.drawable.ic_image, ContextCompat.getColor(this, R.color.teal_700))
            .errorResWithTint(R.drawable.ic_image_remove, R.color.purple_500)
            .centerCrop()
            .into(image1)
        SimpleImageLoader.load()
            .from("https://zibuhoker.ru/ifm/images/image2.jpg")
            .placeholderResWithTint(R.drawable.ic_image, R.color.teal_700)
            .errorResWithTint(R.drawable.ic_image_remove, R.color.purple_500)
            .fitCenter()
            .into(image2)
        SimpleImageLoader.load()
            .from("https://zibuhoker.ru/ifm/images/image3.jpg")
            .placeholderResWithTint(R.drawable.ic_image, R.color.teal_700)
            .errorResWithTint(R.drawable.ic_image_remove, R.color.purple_500)
            .centerInside()
            .into(image3)
    }

}