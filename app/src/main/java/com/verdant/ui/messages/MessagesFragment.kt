package com.verdant.ui.messages

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.verdant.R
import com.verdant.core.auth.AuthState
import com.verdant.data.session.UserSessionManager
import com.verdant.databinding.FragmentMessagesBinding
import kotlinx.coroutines.launch

class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMessagesBinding.bind(view)

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                UserSessionManager.authState.collect { state ->
                    val visible = if (state is AuthState.Guest) View.GONE else View.VISIBLE
                    binding.btnNotifications.visibility = visible
                    binding.btnSettings.visibility = visible
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
