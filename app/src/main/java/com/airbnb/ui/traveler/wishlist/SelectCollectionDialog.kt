package com.airbnb.ui.traveler.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.core.auth.AuthManager
import com.airbnb.data.repository.WishlistCollectionRepository
import com.airbnb.databinding.DialogSelectCollectionBinding
import com.airbnb.ui.traveler.wishlist.adapter.CollectionSelectionAdapter
import kotlinx.coroutines.launch

/**
 * Dialog for selecting a wishlist collection when saving a listing.
 */
class SelectCollectionDialog : DialogFragment() {

    private var _binding: DialogSelectCollectionBinding? = null
    private val binding get() = _binding!!

    private val collectionRepository = WishlistCollectionRepository()

    private lateinit var adapter: CollectionSelectionAdapter

    private var listingId: String? = null

    private var onCollectionSelected: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(
            STYLE_NO_FRAME,
            android.R.style.Theme_Translucent_NoTitleBar
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DialogSelectCollectionBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        listingId = arguments?.getString(ARG_LISTING_ID)

        setupRecyclerView()
        setupCreateNewButton()

        loadCollections()
    }

    private fun setupRecyclerView() {

        adapter = CollectionSelectionAdapter { collection ->

            onCollectionSelected?.invoke(collection.id)

            dismiss()
        }

        binding.recyclerViewCollections.apply {

            layoutManager = LinearLayoutManager(requireContext())

            adapter = this@SelectCollectionDialog.adapter
        }
    }

    private fun setupCreateNewButton() {

        binding.btnCreateNew.setOnClickListener {

            showCreateCollectionDialog()
        }
    }

    private fun loadCollections() {

        val userId = AuthManager.currentUserId() ?: return

        showLoading(true)

        lifecycleScope.launch {

            collectionRepository.getCollections(userId)

                .onSuccess { collections ->

                    showLoading(false)

                    if (collections.isEmpty()) {

                        createDefaultCollection()

                    } else {

                        adapter.submitList(collections)
                    }
                }

                .onFailure { error ->

                    showLoading(false)

                    Toast.makeText(
                        requireContext(),
                        "Failed to load collections: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    dismiss()
                }
        }
    }

    private fun createDefaultCollection() {

        val userId = AuthManager.currentUserId() ?: return

        lifecycleScope.launch {

            collectionRepository.getOrCreateDefaultCollection(userId)

                .onSuccess { collection ->

                    onCollectionSelected?.invoke(collection.id)

                    dismiss()
                }

                .onFailure { error ->

                    Toast.makeText(
                        requireContext(),
                        "Failed to create default collection: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    dismiss()
                }
        }
    }

    private fun showCreateCollectionDialog() {

        val dialog = CreateCollectionDialog()

        dialog.setOnCollectionCreated {

            loadCollections()
        }

        dialog.show(
            parentFragmentManager,
            "CreateCollectionDialog"
        )
    }

    private fun showLoading(isLoading: Boolean) {

        binding.progressBar.visibility =
            if (isLoading) View.VISIBLE else View.GONE

        binding.recyclerViewCollections.visibility =
            if (isLoading) View.GONE else View.VISIBLE

        binding.btnCreateNew.isEnabled = !isLoading
    }

    fun setListingId(id: String): SelectCollectionDialog {

        arguments = Bundle().apply {

            putString(ARG_LISTING_ID, id)
        }

        return this
    }

    fun setOnCollectionSelected(
        callback: (String) -> Unit
    ): SelectCollectionDialog {

        this.onCollectionSelected = callback

        return this
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    companion object {

        private const val ARG_LISTING_ID = "listing_id"

        fun newInstance(
            listingId: String
        ): SelectCollectionDialog {

            return SelectCollectionDialog().apply {

                arguments = Bundle().apply {

                    putString(
                        ARG_LISTING_ID,
                        listingId
                    )
                }
            }
        }
    }
}