package com.airbnb.ui.traveler.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.databinding.DialogFirstSaveBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * A premium custom Bottom Sheet dialog shown when a user saves a listing
 * but doesn't have any collections created yet. Gives them a zero-friction single-tap path
 * to save to "Favorites" or a quick path to create a custom wishlist collection.
 */
class FirstSaveDialog : BottomSheetDialogFragment() {

    private var _binding: DialogFirstSaveBinding? = null
    private val binding get() = _binding!!

    private var onSaveToFavoritesSelected: (() -> Unit)? = null
    private var onCreateCollectionSelected: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFirstSaveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnSaveToFavorites.setOnClickListener {
            onSaveToFavoritesSelected?.invoke()
            dismiss()
        }

        binding.btnCreateCollection.setOnClickListener {
            onCreateCollectionSelected?.invoke()
            dismiss()
        }
    }

    fun setOnSaveToFavoritesListener(listener: () -> Unit): FirstSaveDialog {
        this.onSaveToFavoritesSelected = listener
        return this
    }

    fun setOnCreateCollectionListener(listener: () -> Unit): FirstSaveDialog {
        this.onCreateCollectionSelected = listener
        return this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): FirstSaveDialog {
            return FirstSaveDialog()
        }
    }
}
