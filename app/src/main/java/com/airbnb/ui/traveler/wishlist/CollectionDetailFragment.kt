package com.airbnb.ui.traveler.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.R
import com.airbnb.databinding.FragmentCollectionDetailBinding
import com.airbnb.ui.traveler.wishlist.adapter.WishlistAdapter
import kotlinx.coroutines.launch

/**
 * Fragment displaying listings within a specific wishlist collection.
 */
class CollectionDetailFragment : Fragment() {

    private var _binding: FragmentCollectionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CollectionDetailViewModel by viewModels()
    private lateinit var adapter: WishlistAdapter

    private var collectionId: String? = null
    private var collectionName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionId = arguments?.getString("collectionId")
        collectionName = arguments?.getString("collectionName")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        collectionId?.let { id ->
            viewModel.loadCollection(id)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = collectionName ?: "Collection"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = WishlistAdapter(
            onItemClick = { listing ->
                // Navigate to listing detail
                val bundle = Bundle().apply {
                    putString("listingId", listing.id)
                }
                findNavController().navigate(
                    R.id.action_collectionDetailFragment_to_listingDetailFragment,
                    bundle
                )
            },
            onRemoveClick = { listing ->
                collectionId?.let { id ->
                    viewModel.removeFromCollection(id, listing.id)
                }
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CollectionDetailFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe listings
            launch {
                viewModel.listings.collect { listings ->
                    adapter.submitList(listings)
                    updateEmptyState(listings.isEmpty())
                }
            }

            // Observe loading state
            launch {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }

            // Observe toast messages
            launch {
                viewModel.toast.collect { message ->
                    message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.consumeToast()
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
