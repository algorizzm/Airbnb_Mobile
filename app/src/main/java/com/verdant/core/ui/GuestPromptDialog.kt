package com.verdant.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.verdant.R
import com.verdant.databinding.DialogGuestPromptBinding

class GuestPromptDialog : BottomSheetDialogFragment() {

    private var _binding: DialogGuestPromptBinding? = null
    private val binding get() = _binding!!

    // Debounce: prevent double-tap navigation
    private var navigated = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogGuestPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGuestSignUp.setOnClickListener { navigateToAuth() }
        binding.btnGuestLogIn.setOnClickListener { navigateToAuth() }
        binding.btnGuestDismiss.setOnClickListener { dismiss() }
    }

    private fun navigateToAuth() {
        if (navigated) return
        navigated = true
        dismiss()
        // Safe navigation: use the host fragment's NavController, not the dialog's
        try {
            val navController = requireParentFragment().findNavController()
            // Only navigate if auth_graph is a valid destination from current location
            val dest = navController.graph.findNode(R.id.auth_graph)
            if (dest != null) {
                navController.navigate(R.id.auth_graph)
            }
        } catch (e: Exception) {
            // Fragment may be detached — silently ignore
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "GuestPromptDialog"

        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            // Guard against duplicate stacking
            if (fragmentManager.findFragmentByTag(TAG) == null &&
                !fragmentManager.isStateSaved
            ) {
                GuestPromptDialog().show(fragmentManager, TAG)
            }
        }
    }
}