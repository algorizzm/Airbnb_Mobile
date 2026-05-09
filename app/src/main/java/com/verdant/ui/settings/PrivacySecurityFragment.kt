package com.verdant.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.verdant.R
import com.verdant.databinding.FragmentPrivacySecurityBinding

class PrivacySecurityFragment : Fragment(R.layout.fragment_privacy_security) {

    private var _binding: FragmentPrivacySecurityBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPrivacySecurityBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.rowChangePassword.setOnClickListener {
            findNavController().navigate(R.id.changePasswordFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
