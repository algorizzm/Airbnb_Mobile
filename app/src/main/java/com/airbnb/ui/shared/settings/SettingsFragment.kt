package com.airbnb.ui.shared.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.core.auth.AuthManager
import com.airbnb.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment(R.layout.fragment_settings) {
    // Deprecated in Sprint 6: settings actions are now available in Profile.

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.rowAccountInfo.setOnClickListener {
            findNavController().navigate(R.id.accountInformationFragment)
        }

        binding.rowPrivacy.setOnClickListener {
            findNavController().navigate(R.id.privacySecurityFragment)
        }

        binding.rowNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationSettingsFragment)
        }

        binding.rowSignOut.setOnClickListener {
            AuthManager.signOut()
            findNavController().navigate(R.id.main_graph)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
