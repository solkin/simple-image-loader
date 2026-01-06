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
import com.tomclaw.imageloader.demo.databinding.FragmentTransformBinding
import com.tomclaw.imageloader.util.fetch

class TransformFragment : Fragment() {

    private var _binding: FragmentTransformBinding? = null
    private val binding get() = _binding!!

    private val sampleImages = listOf(
        "https://picsum.photos/800/600?random=1",
        "https://picsum.photos/800/600?random=2",
        "https://picsum.photos/800/600?random=3",
        "https://picsum.photos/800/600?random=4",
        "https://picsum.photos/800/600?random=5"
    )
    
    private var currentImageIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupInsets()
        setupListeners()
        loadImage()
    }
    
    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { v, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = bottomInset + 80.dpToPx())
            insets
        }
    }
    
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupListeners() {
        // Scale type changes
        binding.scaleTypeGroup.setOnCheckedStateChangeListener { _, _ ->
            loadImage()
        }
        
        // Transform changes
        listOf(
            binding.chipCircle,
            binding.chipRounded,
            binding.chipGrayscale,
            binding.chipBlur
        ).forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ -> loadImage() }
        }
        
        // Option changes
        listOf(
            binding.chipCrossfade,
            binding.chipPlaceholder
        ).forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ -> loadImage() }
        }
        
        // Reload button
        binding.btnReload.setOnClickListener {
            loadImage()
        }
        
        // New image button
        binding.btnNewImage.setOnClickListener {
            currentImageIndex = (currentImageIndex + 1) % sampleImages.size
            loadImage()
        }
    }

    private fun loadImage() {
        val imageUrl = sampleImages[currentImageIndex] + "&t=${System.currentTimeMillis()}"
        
        val useCrossfade = binding.chipCrossfade.isChecked
        val usePlaceholder = binding.chipPlaceholder.isChecked
        val useCircle = binding.chipCircle.isChecked
        val useRounded = binding.chipRounded.isChecked
        val useGrayscale = binding.chipGrayscale.isChecked
        val useBlur = binding.chipBlur.isChecked
        
        val scaleType = when (binding.scaleTypeGroup.checkedChipId) {
            R.id.chip_center_crop -> ImageView.ScaleType.CENTER_CROP
            R.id.chip_fit_center -> ImageView.ScaleType.FIT_CENTER
            R.id.chip_center_inside -> ImageView.ScaleType.CENTER_INSIDE
            else -> ImageView.ScaleType.CENTER_CROP
        }
        
        binding.previewImage.fetch(imageUrl) {
            when (scaleType) {
                ImageView.ScaleType.CENTER_CROP -> centerCrop()
                ImageView.ScaleType.FIT_CENTER -> fitCenter()
                ImageView.ScaleType.CENTER_INSIDE -> centerInside()
                else -> centerCrop()
            }
            
            if (useCrossfade) crossfade(500)
            if (usePlaceholder) placeholder(R.drawable.ic_image)
            
            if (useCircle) circleCrop()
            if (useRounded) roundedCorners(48f)
            if (useGrayscale) grayscale()
            if (useBlur) blur(25f)
            
            error(R.drawable.ic_image_remove)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

