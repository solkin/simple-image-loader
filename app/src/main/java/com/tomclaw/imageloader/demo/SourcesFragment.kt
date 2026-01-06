package com.tomclaw.imageloader.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.tomclaw.imageloader.demo.databinding.FragmentSourcesBinding
import com.tomclaw.imageloader.util.fetch

class SourcesFragment : Fragment() {

    private var _binding: FragmentSourcesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSourcesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupInsets()
        loadImages()
    }
    
    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { v, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = bottomInset + 80.dpToPx())
            insets
        }
    }
    
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun loadImages() {
        // HTTP/HTTPS source
        val httpUrl = "https://picsum.photos/800/400?random=${System.currentTimeMillis()}"
        binding.urlHttp.text = httpUrl
        binding.imageHttp.fetch(httpUrl) {
            centerCrop()
            crossfade()
            placeholder(R.drawable.ic_image)
            error(R.drawable.ic_image_remove)
        }
        
        // File (Assets) source
        val assetUrl = "file:///android_asset/sample_image.jpg"
        binding.urlFile.text = assetUrl
        binding.imageFile.fetch(assetUrl) {
            centerCrop()
            crossfade()
            placeholder(R.drawable.ic_image)
            error(R.drawable.ic_image_remove)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

