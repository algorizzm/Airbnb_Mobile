package com.airbnb.ui.traveler.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.airbnb.data.model.WishlistCollection
import com.airbnb.databinding.DialogCollectionOptionsBinding

/**
 * Dialog for wishlist collection actions.
 */
class OptionCollectionDialog : DialogFragment() {

    private var _binding: DialogCollectionOptionsBinding? = null
    private val binding get() = _binding!!

    private var collection: WishlistCollection? = null

    private var onRenameClick: ((WishlistCollection) -> Unit)? = null
    private var onDeleteClick: ((WishlistCollection) -> Unit)? = null

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

        _binding = DialogCollectionOptionsBinding.inflate(
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupButtons()
    }

    private fun setupUI() {

        val currentCollection = collection ?: return

        binding.tvCollectionName.text = currentCollection.name

        binding.cardDelete.visibility =
            if (currentCollection.isDefault) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun setupButtons() {

        binding.cardRename.setOnClickListener {

            collection?.let {
                onRenameClick?.invoke(it)
            }

            dismiss()
        }

        binding.cardDelete.setOnClickListener {

            collection?.let {
                onDeleteClick?.invoke(it)
            }

            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setCollection(
        collection: WishlistCollection
    ): OptionCollectionDialog {

        this.collection = collection
        return this
    }

    fun setOnRenameClick(
        callback: (WishlistCollection) -> Unit
    ): OptionCollectionDialog {

        this.onRenameClick = callback
        return this
    }

    fun setOnDeleteClick(
        callback: (WishlistCollection) -> Unit
    ): OptionCollectionDialog {

        this.onDeleteClick = callback
        return this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}