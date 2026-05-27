package com.airbnb.ui.wishlist

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
import com.airbnb.databinding.FragmentWishlistBinding
import com.airbnb.ui.wishlist.adapter.WishlistAdapter
import com.airbnb.core.ui.GuestPromptHelper
import com.airbnb.core.ui.isUserAuthenticated
import kotlinx.coroutines.launch

class WishlistFragment : Fragment() {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WishlistViewModel by viewModels()
    private lateinit var adapter: WishlistAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check authentication status
        if (!isUserAuthenticated()) {
            showGuestState()
            return
        }

        setupRecyclerView()
        observeViewModel()
    }

    private fun showGuestState() {
        GuestPromptHelper.setupGuestPrompt(
            promptLayout = binding.layoutGuestPrompt.root,
            fragment = this,
            title = getString(R.string.guest_prompt_title_wishlist),
            message = getString(R.string.guest_prompt_message_wishlist),
            iconRes = R.drawable.ic_heart
        )
        GuestPromptHelper.showGuestPrompt(
            promptLayout = binding.layoutGuestPrompt.root,
            contentLayout = binding.recyclerView
        )
        binding.emptyStateLayout.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = WishlistAdapter(
            onItemClick = { listing ->
                // Navigate to listing detail
                val bundle = Bundle().apply {
                    putString("listingId", listing.id)
                }
                findNavController().navigate(
                    R.id.action_wishlistFragment_to_listingDetailFragment,
                    bundle
                )
            },
            onRemoveClick = { listing ->
                viewModel.removeFromWishlist(listing.id)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WishlistFragment.adapter
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
