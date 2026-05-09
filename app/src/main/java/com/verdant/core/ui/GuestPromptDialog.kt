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

        binding.btnGuestSignUp.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.auth_graph)
        }

        binding.btnGuestLogIn.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.auth_graph)
        }

        binding.btnGuestDismiss.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "GuestPromptDialog"

        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                GuestPromptDialog().show(fragmentManager, TAG)
            }
        }
    }
}
