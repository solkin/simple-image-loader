package com.tomclaw.imageloader.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.tomclaw.imageloader.demo.databinding.FragmentGalleryBinding
import com.tomclaw.imageloader.util.fetch
import org.json.JSONArray
import java.net.URL
import kotlin.concurrent.thread
import kotlin.random.Random

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    
    private val items = mutableListOf<GalleryItem>()
    private lateinit var adapter: GalleryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupInsets()
        
        adapter = GalleryAdapter(items)
        
        binding.recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator = null
        
        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }
        
        if (items.isEmpty()) {
            loadData()
        }
    }
    
    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { v, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = bottomInset + 80.dpToPx())
            insets
        }
    }
    
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun loadData() {
        binding.progress.isVisible = items.isEmpty()
        
        thread {
            try {
                val since = Random(System.currentTimeMillis()).nextInt(100, 5000)
                val json = URL("https://api.github.com/users?since=$since&per_page=50").readText()
                val array = JSONArray(json)
                
                val newItems = mutableListOf<GalleryItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val avatarUrl = obj.optString("avatar_url") ?: continue
                    val login = obj.optString("login")
                    newItems.add(GalleryItem(avatarUrl, login))
                }
                
                activity?.runOnUiThread {
                    items.clear()
                    items.addAll(newItems)
                    adapter.notifyDataSetChanged()
                    binding.progress.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                activity?.runOnUiThread {
                    binding.progress.isVisible = false
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class GalleryItem(
    val imageUrl: String,
    val title: String
)

class GalleryAdapter(
    private val items: List<GalleryItem>
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        
        holder.titleView.text = item.title
        
        holder.imageView.fetch(item.imageUrl) {
            centerCrop()
            crossfade()
            placeholder(R.drawable.ic_image)
            error(R.drawable.ic_image_remove)
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_view)
        val titleView: TextView = view.findViewById(R.id.title_view)
    }
}

