package com.airbnb.ui.messages

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.airbnb.R
import com.airbnb.core.auth.AuthState
import com.airbnb.core.auth.AuthManager
import com.airbnb.databinding.FragmentMessagesBinding
import kotlinx.coroutines.launch

class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    // Guest prompt views
    private lateinit var guestPrompt: View
    private lateinit var btnGuestLogIn: MaterialButton
    private lateinit var btnGuestSignUp: MaterialButton
    private lateinit var btnGuestDismiss: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMessagesBinding.bind(view)

        // Included layout views
        guestPrompt = view.findViewById(R.id.layoutGuestPrompt)

        btnGuestLogIn = view.findViewById(R.id.btnGuestLogIn)
        btnGuestSignUp = view.findViewById(R.id.btnGuestSignUp)
        btnGuestDismiss = view.findViewById(R.id.btnGuestDismiss)

        setupClicks()
        observeAuthState()
    }

    private fun setupClicks() {

        // Notifications
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

        // Settings
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        // Login
        btnGuestLogIn.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }

        // Sign up
        btnGuestSignUp.setOnClickListener {
            findNavController().navigate(R.id.signupFragment)
        }

        // Dismiss popup
        btnGuestDismiss.setOnClickListener {

            AuthManager.dismissGuestPrompt()

            guestPrompt.animate()
                .translationY(300f)
                .alpha(0f)
                .setDuration(220)
                .withEndAction {

                    guestPrompt.visibility = View.GONE

                    binding.viewGuestOverlay.visibility =
                        View.GONE
                }
                .start()
        }
    }

    private fun updateGuestUI(
        isGuest: Boolean,
        dismissed: Boolean
    ) {

        val shouldShowPrompt = isGuest && !dismissed

        // Guest locked state
        binding.layoutGuestState.visibility =
            if (isGuest) View.VISIBLE else View.GONE

        // Authenticated content
        binding.layoutAuthenticatedContent.visibility =
            if (isGuest) View.GONE else View.VISIBLE

        // Overlay
        binding.viewGuestOverlay.visibility =
            if (shouldShowPrompt) View.VISIBLE else View.GONE

        // Popup
        guestPrompt.visibility =
            if (shouldShowPrompt) View.VISIBLE else View.GONE

        // Protected actions
        binding.btnNotifications.visibility =
            if (isGuest) View.GONE else View.VISIBLE

        binding.btnSettings.visibility =
            if (isGuest) View.GONE else View.VISIBLE

        // Animate popup
        if (shouldShowPrompt) {

            guestPrompt.alpha = 0f
            guestPrompt.translationY = 250f

            guestPrompt.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(260)
                .start()
        }
    }

    private fun observeAuthState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {

                    AuthManager.authState().collect { state ->

                        updateGuestUI(
                            isGuest = state is AuthState.Guest,
                            dismissed = AuthManager
                                .guestPromptDismissed()
                                .value
                        )
                    }
                }

                launch {

                    AuthManager.guestPromptDismissed()
                        .collect { dismissed ->

                            updateGuestUI(
                                isGuest = AuthManager
                                    .stateSnapshot() is AuthState.Guest,
                                dismissed = dismissed
                            )
                        }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}