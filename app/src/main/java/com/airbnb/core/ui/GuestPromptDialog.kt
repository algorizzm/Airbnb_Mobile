package com.airbnb.core.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.databinding.DialogGuestPromptBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class GuestPromptDialog : BottomSheetDialogFragment() {

    private var _binding: DialogGuestPromptBinding? = null
    private val binding get() = _binding!!

    private var navigated = false

    override fun getTheme(): Int = R.style.RoundedBottomSheetDialog

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {

            val bottomSheet =
                dialog.findViewById<View>(
                    com.google.android.material.R.id.design_bottom_sheet
                )

            bottomSheet?.let { sheet ->

                val behavior = BottomSheetBehavior.from(sheet)

                // Get screen height
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.heightPixels

                // 80% height
                val desiredHeight = (screenHeight * 0.80).toInt()

                // Apply height
                sheet.layoutParams.height = desiredHeight

                // Expand fully
                behavior.state = BottomSheetBehavior.STATE_EXPANDED

                // Allow drag dismiss
                behavior.isDraggable = true

                // Prevent tiny collapsed state
                behavior.skipCollapsed = true

                // Smooth interaction
                behavior.peekHeight = desiredHeight
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DialogGuestPromptBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGuestSignUp.setOnClickListener {
            navigateToAuth()
        }

        binding.btnGuestLogIn.setOnClickListener {
            navigateToAuth()
        }

        binding.btnApple.setOnClickListener {
            navigateToAuth()
        }

        binding.btnGuestDismiss.setOnClickListener {
            dismiss()
        }
    }

    private fun navigateToAuth() {

        if (navigated) return

        navigated = true

        dismiss()

        try {

            val navController =
                requireParentFragment().findNavController()

            val destination =
                navController.graph.findNode(R.id.auth_graph)

            if (destination != null) {
                navController.navigate(R.id.auth_graph)
            }

        } catch (_: Exception) {
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        const val TAG = "GuestPromptDialog"

        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager
        ) {

            if (
                fragmentManager.findFragmentByTag(TAG) == null &&
                !fragmentManager.isStateSaved
            ) {

                GuestPromptDialog().show(
                    fragmentManager,
                    TAG
                )
            }
        }
    }
}