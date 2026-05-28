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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.R
import com.airbnb.databinding.FragmentWishlistBinding
import com.airbnb.ui.traveler.wishlist.adapter.WishlistAdapter
import com.airbnb.ui.traveler.wishlist.adapter.WishlistCollectionAdapter
import com.airbnb.ui.auth.GuestPromptHelper
import com.airbnb.ui.auth.isUserAuthenticated
import kotlinx.coroutines.launch
import com.airbnb.ui.auth.GuestPromptDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.airbnb.core.ui.EditTextDialog

class WishlistFragment : Fragment() {

    private var _binding: FragmentWishlistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WishlistViewModel by viewModels()
    private lateinit var collectionAdapter: WishlistCollectionAdapter
    private var showCollections = true // Toggle between collection view and flat list view

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
        setupCreateCollectionButton()
        observeViewModel()
    }

    private fun showGuestState() {

        binding.layoutGuestPrompt.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.headerDivider.visibility = View.GONE

        binding.btnLogin.setOnClickListener {

            GuestPromptDialog().show(parentFragmentManager, "GuestPromptDialog")
        }
    }

    private fun setupRecyclerView() {
        collectionAdapter = WishlistCollectionAdapter(
            onCollectionClick = { collection ->
                // Navigate to collection details
                val bundle = Bundle().apply {
                    putString("collectionId", collection.id)
                    putString("collectionName", collection.name)
                }
                findNavController().navigate(
                    R.id.action_wishlistFragment_to_collectionDetailFragment,
                    bundle
                )
            },
            onCollectionLongClick = { collection ->
                showCollectionOptionsDialog(collection)
            }
        )

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = collectionAdapter
        }
    }

    private fun setupCreateCollectionButton() {
        binding.fabCreateCollection?.setOnClickListener {
            showCreateCollectionDialog()
        }
    }

    private fun showCreateCollectionDialog() {
        val dialog = CreateCollectionDialog()
        dialog.setOnCollectionCreated {
            // Collections will auto-refresh via Flow
            Toast.makeText(requireContext(), "Collection created", Toast.LENGTH_SHORT).show()
        }
        dialog.show(parentFragmentManager, "CreateCollectionDialog")
    }

    private fun showCollectionOptionsDialog(collection: com.airbnb.data.model.WishlistCollection) {
        val options = if (collection.isDefault) {
            arrayOf("Rename")
        } else {
            arrayOf("Rename", "Delete")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(collection.name)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Rename" -> showRenameDialog(collection)
                    "Delete" -> showDeleteConfirmation(collection)
                }
            }
            .show()
    }

    private fun showRenameDialog(collection: com.airbnb.data.model.WishlistCollection) {
        EditTextDialog.show(
            context = requireContext(),
            title = "Rename Collection",
            hint = "Collection name",
            initial = collection.name,
            onSave = { newName ->
                if (newName.isNotBlank()) {
                    viewModel.renameCollection(collection.id, newName)
                }
            }
        )
    }

    private fun showDeleteConfirmation(collection: com.airbnb.data.model.WishlistCollection) {
        val message = if (collection.isEmpty()) {
            "Delete \"${collection.name}\"?"
        } else {
            "Delete \"${collection.name}\"? All ${collection.size()} listings will be moved to Favorites."
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Collection")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCollection(collection.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe collections
            launch {
                viewModel.collections.collect { collections ->
                    collectionAdapter.submitList(collections)
                    updateEmptyState(collections.isEmpty())
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
