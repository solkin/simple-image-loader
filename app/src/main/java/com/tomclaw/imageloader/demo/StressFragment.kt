package com.tomclaw.imageloader.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tomclaw.imageloader.demo.databinding.FragmentStressBinding
import com.tomclaw.imageloader.util.fetch
import java.util.concurrent.atomic.AtomicInteger

class StressFragment : Fragment() {

    private var _binding: FragmentStressBinding? = null
    private val binding get() = _binding!!
    
    private val imageCount = 500
    private val loadedCount = AtomicInteger(0)
    private val errorCount = AtomicInteger(0)
    
    private lateinit var adapter: StressAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupInsets()
        setupRecyclerView()
        setupControls()
        updateStats()
    }
    
    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { v, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = bottomInset + 56.dpToPx())
            insets
        }
    }
    
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupRecyclerView() {
        adapter = StressAdapter(
            imageCount = imageCount,
            onImageLoaded = {
                loadedCount.incrementAndGet()
                activity?.runOnUiThread { updateStats() }
            },
            onImageError = {
                errorCount.incrementAndGet()
                activity?.runOnUiThread { updateStats() }
            }
        )
        
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator = null
    }

    private fun setupControls() {
        binding.btnReload.setOnClickListener {
            resetStats()
            adapter.notifyDataSetChanged()
        }
        
        binding.btnClearCache.setOnClickListener {
            // For demo purposes, just reload with fresh URLs
            resetStats()
            adapter.refreshUrls()
            adapter.notifyDataSetChanged()
        }
    }

    private fun resetStats() {
        loadedCount.set(0)
        errorCount.set(0)
        updateStats()
    }

    private fun updateStats() {
        val loaded = loadedCount.get()
        val errors = errorCount.get()
        val total = loaded + errors
        
        binding.statTotal.text = imageCount.toString()
        binding.statLoaded.text = loaded.toString()
        binding.statErrors.text = errors.toString()
        
        binding.progressBar.max = imageCount
        binding.progressBar.progress = total
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class StressAdapter(
    private val imageCount: Int,
    private val onImageLoaded: () -> Unit,
    private val onImageError: () -> Unit
) : RecyclerView.Adapter<StressAdapter.ViewHolder>() {

    private var urlSuffix = System.currentTimeMillis()
    
    fun refreshUrls() {
        urlSuffix = System.currentTimeMillis()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stress, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Using picsum.photos for variety of images
        val imageUrl = "https://picsum.photos/200/200?random=$position&t=$urlSuffix"
        
        holder.imageView.fetch(imageUrl) {
            centerCrop()
            placeholder(R.drawable.ic_image)
            error(R.drawable.ic_image_remove)
            
            onSuccess { _, _ ->
                onImageLoaded()
            }
            onError { _, _ ->
                onImageError()
            }
        }
    }

    override fun getItemCount() = imageCount

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_view)
    }
}

