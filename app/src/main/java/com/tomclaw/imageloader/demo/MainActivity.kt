package com.tomclaw.imageloader.demo

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tomclaw.imageloader.SimpleImageLoader.imageLoader
import java.net.URL
import kotlin.concurrent.thread
import kotlin.random.Random
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageLoader()

        val recyclerview = findViewById<RecyclerView>(R.id.recycler)
        recyclerview.layoutManager = LinearLayoutManager(this)


        val data = ArrayList<ItemsViewModel>()
        val adapter = CustomAdapter(data)

        thread {
            // Yeah, this demo is quite a codeshit :)
            try {
                val since = Random(System.currentTimeMillis()).nextInt(100, 5000)
                val json = URL("https://api.github.com/users?since=$since").readText()
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val avatarUrl = obj.optString("avatar_url") ?: continue
                    val login = obj.optString("login")
                    val url = obj.optString("url")
                    data.add(ItemsViewModel(avatarUrl, login, url))
                }
            } catch (ignored: Throwable) {
            }
            runOnUiThread { adapter.notifyDataSetChanged() }
        }

        recyclerview.adapter = adapter
    }

}