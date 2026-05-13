package com.verdant.ui.hikes

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.verdant.R
import com.verdant.core.auth.AuthState
import com.verdant.data.session.UserSessionManager
import com.verdant.databinding.FragmentHikesBinding
import com.verdant.ui.explore.ExploreFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HikesFragment : Fragment(R.layout.fragment_hikes) {

    private var _binding: FragmentHikesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HikesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHikesBinding.bind(view)

        setupClicks()
        observeUi()
    }

    private fun setupClicks() {

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        // Events tab — visible only for guides
        binding.tabEvents.setOnClickListener {
            findNavController().navigate(R.id.eventsFragment)
        }

        // History tab
        binding.tabHistory.setOnClickListener {
            findNavController().navigate(R.id.hikeHistoryFragment)
        }

        // Empty state action
        binding.btnEmptyAction.setOnClickListener {
            if (viewModel.isGuide.value) {
                findNavController().navigate(R.id.eventsFragment)
            } else {
                findNavController().navigate(R.id.exploreFragment)
            }
        }

        // Active hike — navigate to detail
        val navigateToActiveHikeDetails = {
            val hikeId = viewModel.uiState.value.activeHike?.id
            if (hikeId != null) {
                val bundle = Bundle().apply {
                    putString(ExploreFragment.ARG_HIKE_ID, hikeId)
                }
                try {
                    findNavController().navigate(R.id.action_hikeFragment_to_hikeDetailFragment, bundle)
                } catch (e: IllegalArgumentException) {
                    // Ignore double-click crash
                }
            }
        }

        binding.btnViewActiveHike.setOnClickListener { navigateToActiveHikeDetails() }
        binding.layoutActive.setOnClickListener { navigateToActiveHikeDetails() }

        binding.btnEndHike.setOnClickListener {
            viewModel.endActiveHike()
        }
    }

    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Auth icons visibility
                launch {
                    UserSessionManager.authState.collect { state ->
                        val visible = if (state is AuthState.Guest) View.GONE else View.VISIBLE
                        binding.btnNotifications.visibility = visible
                        binding.btnSettings.visibility = visible
                    }
                }

                // Guide-only tabs
                launch {
                    viewModel.isGuide.collect { guide ->
                        binding.tabEvents.visibility = if (guide) View.VISIBLE else View.GONE
                        binding.btnEmptyAction.text =
                            if (guide) "Go to events" else "Explore hikes"
                        binding.tvEmptySub.text =
                            if (guide)
                                "Create and manage hikes from Events."
                            else
                                "Explore available hikes and join one."
                    }
                }

                // Empty / active layout toggle
                launch {
                    viewModel.hasActiveHike.collect { hasHike ->
                        binding.layoutEmpty.visibility = if (hasHike) View.GONE else View.VISIBLE
                        binding.layoutActive.visibility = if (hasHike) View.VISIBLE else View.GONE
                    }
                }

                // Show active hike title in the card
                launch {
                    viewModel.uiState.collect { state ->
                        val hike = state.activeHike
                        if (hike != null) {
                            binding.tvActiveHikeTitle.text = hike.title
                            binding.tvGuideInfo.text = "Guide: ${hike.guideName}"
                            binding.tvParticipants.text = "Participants: ${state.approvedCount} / ${hike.maxParticipants}"
                            
                            val meetup = if (hike.meetupPoint.isNotBlank()) hike.meetupPoint else hike.location
                            val dest = hike.destination.ifBlank { "N/A" }
                            binding.tvLocation.text = "Route: $meetup → $dest"

                            val df = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            val start = hike.startDateTime?.toDate()?.let { df.format(it) } ?: "TBD"
                            val end = hike.endDateTime?.toDate()?.let { df.format(it) } ?: "TBD"
                            binding.tvDates.text = "Dates: $start - $end"

                            binding.tvDistanceElevation.text = "Distance: ${hike.effectiveDistanceKm()}km, Elevation: ${hike.elevationM}m"
                            binding.tvStatus.text = "Status: ${hike.status}"
                        }
                    }
                }

                // Show end hike button for guides
                launch {
                    viewModel.isGuide.collect { guide ->
                        binding.btnEndHike.visibility = if (guide) View.VISIBLE else View.GONE
                        binding.tvGuideInfo.visibility = if (guide) View.GONE else View.VISIBLE
                    }
                }

                // Toast
                launch {
                    viewModel.toast.collect { msg ->
                        if (!msg.isNullOrBlank()) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            viewModel.consumeToast()
                        }
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