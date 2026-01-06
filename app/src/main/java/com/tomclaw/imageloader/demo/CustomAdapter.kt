package com.tomclaw.imageloader.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tomclaw.imageloader.util.fetch

class CustomAdapter(private val list: List<ItemsViewModel>) :
    RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemViewModel = list[position]
        holder.imageView.fetch(itemViewModel.uriString) {
            centerCrop()
            crossfade()
            placeholder(R.drawable.ic_image)
            error(R.drawable.ic_image_remove)
        }

        holder.titleView.text = itemViewModel.title
        holder.subtitleView.text = itemViewModel.subtitle
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
        val titleView: TextView = itemView.findViewById(R.id.title_view)
        val subtitleView: TextView = itemView.findViewById(R.id.subtitle_view)
    }
}
