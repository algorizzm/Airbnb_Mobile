package com.verdant.ui.hikes.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.verdant.R
import com.verdant.databinding.FragmentCreateHikeMediaBinding
import com.verdant.databinding.ItemCreateHikeMediaSlotBinding
import kotlinx.coroutines.launch

class CreateHikeMediaFragment :
    Fragment(R.layout.fragment_create_hike_media) {

    private var _binding: FragmentCreateHikeMediaBinding? = null
    private val binding get() = _binding!!

    private val flowVm get() = createHikeFlowViewModel()

    private var pendingGalleryIndex: Int? = null

    private val coverPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        flowVm.uploadCover(uri)
    }

    private val galleryPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->

        val idx =
            pendingGalleryIndex
                ?: return@registerForActivityResult

        pendingGalleryIndex = null

        uri ?: return@registerForActivityResult

        flowVm.uploadGallerySlot(idx, uri)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding =
            FragmentCreateHikeMediaBinding.bind(view)

        binding.btnPickCover.setOnClickListener {
            coverPicker.launch("image/*")
        }

        binding.recyclerGallery.layoutManager =
            GridLayoutManager(requireContext(), 2)

        val adapter =
            GallerySlotAdapter(
                slotCount = CREATE_HIKE_MAX_GALLERY,
                onPick = { index ->
                    pendingGalleryIndex = index
                    galleryPicker.launch("image/*")
                }
            )

        binding.recyclerGallery.adapter = adapter

        observeUi(adapter)
    }

    private fun observeUi(
        adapter: GallerySlotAdapter
    ) {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                flowVm.ui.collect { s ->

                    if (s.coverImageUrl.isNotBlank()) {

                        Glide.with(this@CreateHikeMediaFragment)
                            .load(s.coverImageUrl)
                            .centerCrop()
                            .into(binding.imgCover)
                    }

                    adapter.submit(s.galleryImageUrls)

                    binding.layoutFloatingCard.tvFloatingHikeTitle.text =
                        s.title.ifBlank {
                            "Untitled Hike"
                        }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class GallerySlotAdapter(
        private val slotCount: Int,
        private val onPick: (Int) -> Unit
    ) : RecyclerView.Adapter<GallerySlotAdapter.VH>() {

        private var urls: List<String> =
            List(slotCount) { "" }

        fun submit(list: List<String>) {

            val m = list.toMutableList()

            while (m.size < slotCount) {
                m.add("")
            }

            urls = m.take(slotCount)

            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = slotCount

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): VH {

            val inf =
                LayoutInflater.from(parent.context)

            val b =
                ItemCreateHikeMediaSlotBinding.inflate(
                    inf,
                    parent,
                    false
                )

            return VH(b, onPick)
        }

        override fun onBindViewHolder(
            holder: VH,
            position: Int
        ) {

            holder.bind(
                position,
                urls.getOrElse(position) { "" }
            )
        }

        class VH(
            private val binding: ItemCreateHikeMediaSlotBinding,
            private val onPick: (Int) -> Unit
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(
                index: Int,
                url: String
            ) {

                binding.tvSlotLabel.text =
                    "Gallery item ${index + 1}"

                binding.btnPick.setOnClickListener {
                    onPick(index)
                }

                if (url.isNotBlank()) {

                    Glide.with(binding.imgPreview)
                        .load(url)
                        .centerCrop()
                        .into(binding.imgPreview)

                } else {

                    Glide.with(binding.imgPreview)
                        .clear(binding.imgPreview)
                }
            }
        }
    }
}