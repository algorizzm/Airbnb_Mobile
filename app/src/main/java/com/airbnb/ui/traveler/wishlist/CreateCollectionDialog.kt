package com.airbnb.ui.traveler.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.airbnb.core.auth.AuthManager
import com.airbnb.data.model.WishlistCollection
import com.airbnb.data.repository.WishlistCollectionRepository
import com.airbnb.databinding.DialogCreateCollectionBinding
import kotlinx.coroutines.launch

/**
 * Dialog for creating a new wishlist collection.
 */
class CreateCollectionDialog : DialogFragment() {

    private var _binding: DialogCreateCollectionBinding? = null
    private val binding get() = _binding!!

    private val collectionRepository = WishlistCollectionRepository()
    private var onCollectionCreated: ((WishlistCollection) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
    }

    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreate.setOnClickListener {
            createCollection()
        }
    }

    private fun createCollection() {
        val name = binding.etCollectionName.text.toString().trim()

        if (name.isEmpty()) {
            binding.etCollectionName.error = "Collection name is required"
            return
        }

        val userId = AuthManager.currentUserId()
        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnCreate.isEnabled = false
        binding.btnCancel.isEnabled = false

        lifecycleScope.launch {
            collectionRepository.createCollection(userId, name)
                .onSuccess { collection ->
                    Toast.makeText(
                        requireContext(),
                        "Collection created",
                        Toast.LENGTH_SHORT
                    ).show()
                    onCollectionCreated?.invoke(collection)
                    dismiss()
                }
                .onFailure { error ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreate.isEnabled = true
                    binding.btnCancel.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Failed to create collection: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    fun setOnCollectionCreated(callback: (WishlistCollection) -> Unit): CreateCollectionDialog {
        this.onCollectionCreated = callback
        return this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
