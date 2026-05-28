package com.airbnb.ui.shared.messages

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.core.auth.AuthManager
import com.airbnb.core.auth.AuthState
import com.airbnb.databinding.FragmentMessagesBinding
import com.airbnb.ui.auth.GuestPromptDialog
import kotlinx.coroutines.launch

class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private var currentFilter = MessageFilter.ALL

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMessagesBinding.bind(view)

        setupClicks()
        setupFilters()
        observeAuthState()
    }

    private fun setupClicks() {

        binding.btnSearch.setOnClickListener {
            // TODO
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }

        binding.btnLogin.setOnClickListener {
            GuestPromptDialog.show(parentFragmentManager)
        }
    }

    private fun setupFilters() {

        updateFilterChips()
        updateEmptyState()

        binding.chipAll.setOnClickListener {
            currentFilter = MessageFilter.ALL
            updateFilterChips()
            updateEmptyState()
        }

        binding.chipHosting.setOnClickListener {
            currentFilter = MessageFilter.HOSTING
            updateFilterChips()
            updateEmptyState()
        }

        binding.chipTravelling.setOnClickListener {
            currentFilter = MessageFilter.TRAVELLING
            updateFilterChips()
            updateEmptyState()
        }

        binding.chipSupport.setOnClickListener {
            currentFilter = MessageFilter.SUPPORT
            updateFilterChips()
            updateEmptyState()
        }
    }

    private fun updateFilterChips() {

        resetChip(binding.chipAll)
        resetChip(binding.chipHosting)
        resetChip(binding.chipTravelling)
        resetChip(binding.chipSupport)

        when (currentFilter) {

            MessageFilter.ALL -> selectChip(binding.chipAll)

            MessageFilter.HOSTING -> selectChip(binding.chipHosting)

            MessageFilter.TRAVELLING -> selectChip(binding.chipTravelling)

            MessageFilter.SUPPORT -> selectChip(binding.chipSupport)
        }
    }

    private fun resetChip(chip: View) {

        chip.setBackgroundResource(R.drawable.bg_chip_unselected)

        if (chip is android.widget.TextView) {
            chip.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.black)
            )
        }
    }

    private fun selectChip(chip: View) {

        chip.setBackgroundResource(R.drawable.bg_chip_selected)

        if (chip is android.widget.TextView) {
            chip.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.white)
            )
        }
    }

    private fun updateEmptyState() {

        binding.layoutAuthenticatedContent.visibility = View.VISIBLE

        when (currentFilter) {

            MessageFilter.ALL -> {

                binding.tvEmptyTitle.text =
                    getString(R.string.messages_empty_all_title)

                binding.tvEmptySubtitle.text =
                    getString(R.string.messages_empty_all_subtitle)
            }

            MessageFilter.HOSTING -> {

                binding.tvEmptyTitle.text =
                    getString(R.string.messages_empty_hosting_title)

                binding.tvEmptySubtitle.text =
                    getString(R.string.messages_empty_hosting_subtitle)
            }

            MessageFilter.TRAVELLING -> {

                binding.tvEmptyTitle.text =
                    getString(R.string.messages_empty_travelling_title)

                binding.tvEmptySubtitle.text =
                    getString(R.string.messages_empty_travelling_subtitle)
            }

            MessageFilter.SUPPORT -> {

                binding.tvEmptyTitle.text =
                    getString(R.string.messages_empty_support_title)

                binding.tvEmptySubtitle.text =
                    getString(R.string.messages_empty_support_subtitle)
            }
        }
    }

    private fun updateGuestUI(isGuest: Boolean) {

        binding.layoutGuestState.visibility =
            if (isGuest) View.VISIBLE else View.GONE

        binding.layoutAuthenticatedContent.visibility =
            if (isGuest) View.GONE else View.VISIBLE

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

    private enum class MessageFilter {
        ALL,
        HOSTING,
        TRAVELLING,
        SUPPORT
    }
}