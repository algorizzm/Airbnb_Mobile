package com.airbnb.ui.messages

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.core.auth.AuthState
import com.airbnb.core.auth.AuthManager
import com.airbnb.databinding.FragmentMessagesBinding
import kotlinx.coroutines.launch

class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMessagesBinding.bind(view)

        setupClicks()
        observeAuthState()
    }

    private fun setupClicks() {

        // Search
        binding.btnSearch.setOnClickListener {

            // TODO: Navigate to search/messages search screen
        }

        // Settings/Profile
        binding.btnSettings.setOnClickListener {

            findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun updateGuestUI(isGuest: Boolean) {

        // Guest state visible
        binding.layoutGuestState.visibility =
            if (isGuest) View.VISIBLE else View.GONE

        // Authenticated empty state visible
        binding.layoutAuthenticatedContent.visibility =
            if (isGuest) View.GONE else View.VISIBLE

        // Hide actions for guests if desired
        binding.btnSearch.visibility =
            if (isGuest) View.GONE else View.VISIBLE

        binding.btnSettings.visibility =
            if (isGuest) View.GONE else View.VISIBLE
    }

    private fun observeAuthState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                AuthManager.authState().collect { state ->

                    updateGuestUI(
                        isGuest = state is AuthState.Guest
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}