package com.tomclaw.imageloader.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tomclaw.imageloader.SimpleImageLoader.imageLoader

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageLoader()

        val recyclerview = findViewById<RecyclerView>(R.id.recycler)
        recyclerview.layoutManager = LinearLayoutManager(this)
        val data = ArrayList<ItemsViewModel>()
        val uris = listOf(
            "https://zibuhoker.ru/ifm/images/image1.jpg",
            "https://zibuhoker.ru/ifm/images/image2.jpg",
            "https://zibuhoker.ru/ifm/images/image3.jpg",
            "https://zibuhoker.ru/ifm/images/image4.jpg",
            "file:///android_asset/image1.jpg",
        )
        for (i in 1..30) {
            val imageIndex = i % uris.size
            val uri = uris[imageIndex]
            data.add(ItemsViewModel(uri, "Image $i", uri))
        }
        val adapter = CustomAdapter(data)
        recyclerview.adapter = adapter
    }

}