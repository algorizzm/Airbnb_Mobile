package com.verdant.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.verdant.R
import com.verdant.databinding.FragmentNotificationSettingsBinding

class NotificationsFragment : Fragment(R.layout.fragment_notification_settings) {

    private var _binding: FragmentNotificationSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotificationSettingsBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
