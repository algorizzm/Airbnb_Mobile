package com.airbnb.ui.shared.messages

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.databinding.FragmentMessagesBinding
import com.airbnb.ui.auth.GuestPromptDialog
import com.airbnb.ui.auth.isUserAuthenticated

class MessagesFragment : Fragment(R.layout.fragment_messages) {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private var currentFilter = MessageFilter.ALL

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMessagesBinding.bind(view)

        // Match WishlistFragment authentication flow
        if (!isUserAuthenticated()) {
            showGuestState()
            return
        }

        setupClicks()
        setupFilters()
        updateEmptyState()
    }

    private fun showGuestState() {

        binding.layoutGuestState.visibility = View.VISIBLE

        binding.layoutAuthenticatedContent.visibility = View.GONE

        binding.headerDivider.visibility = View.GONE

        binding.filterContainer.visibility = View.GONE

        binding.btnSearch.visibility = View.GONE

        binding.btnSettings.visibility = View.GONE

        binding.btnLogin.setOnClickListener {

            GuestPromptDialog()
                .show(parentFragmentManager, "GuestPromptDialog")
        }
    }

    private fun setupClicks() {

        binding.btnSearch.setOnClickListener {
            // TODO
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun setupFilters() {

        updateFilterChips()

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

            MessageFilter.ALL ->
                selectChip(binding.chipAll)

            MessageFilter.HOSTING ->
                selectChip(binding.chipHosting)

            MessageFilter.TRAVELLING ->
                selectChip(binding.chipTravelling)

            MessageFilter.SUPPORT ->
                selectChip(binding.chipSupport)
        }
    }

    private fun resetChip(chip: View) {

        chip.setBackgroundResource(
            R.drawable.bg_chip_unselected
        )

        if (chip is TextView) {

            chip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
        }
    }

    private fun selectChip(chip: View) {

        chip.setBackgroundResource(
            R.drawable.bg_chip_selected
        )

        if (chip is TextView) {

            chip.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.white
                )
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