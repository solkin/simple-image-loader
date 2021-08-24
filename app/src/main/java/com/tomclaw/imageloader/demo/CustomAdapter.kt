package com.tomclaw.imageloader.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tomclaw.imageloader.SimpleImageLoader
import com.tomclaw.imageloader.centerCrop
import com.tomclaw.imageloader.errorResWithTint
import com.tomclaw.imageloader.placeholderResWithTint

class CustomAdapter(private val list: List<ItemsViewModel>) :
    RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemViewModel = list[position]
        SimpleImageLoader.load()
            .from(itemViewModel.imageUrl)
            .placeholderResWithTint(
                R.drawable.ic_image,
                ContextCompat.getColor(holder.imageView.context, R.color.teal_700)
            )
            .errorResWithTint(
                R.drawable.ic_image_remove,
                ContextCompat.getColor(holder.imageView.context, R.color.purple_500)
            )
            .centerCrop()
            .into(holder.imageView)
        holder.titleView.text = itemViewModel.title
        holder.subtitleView.text = itemViewModel.subtitle
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
        val titleView: TextView = itemView.findViewById(R.id.title_view)
        val subtitleView: TextView = itemView.findViewById(R.id.subtitle_view)
    }
}