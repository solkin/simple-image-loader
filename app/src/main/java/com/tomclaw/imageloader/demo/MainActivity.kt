package com.tomclaw.imageloader.demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tomclaw.imageloader.SimpleImageLoader

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SimpleImageLoader.init(cacheDir, resources.displayMetrics)

        val recyclerview = findViewById<RecyclerView>(R.id.recycler)
        recyclerview.layoutManager = LinearLayoutManager(this)
        val data = ArrayList<ItemsViewModel>()
        for (i in 1..30) {
            val imageIndex = i % 3 + 1
            val url = "https://zibuhoker.ru/ifm/images/image$imageIndex.jpg"
            data.add(ItemsViewModel(url, "Image $i", url))
        }
        val adapter = CustomAdapter(data)
        recyclerview.adapter = adapter
    }

}