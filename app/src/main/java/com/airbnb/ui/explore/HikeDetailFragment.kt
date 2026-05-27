package com.airbnb.ui.explore

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.core.auth.AuthManager
import com.airbnb.core.auth.AuthState
import com.airbnb.core.navigation.ProtectedNav
import com.airbnb.core.ui.toast
import com.airbnb.databinding.FragmentHikeDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HikeDetailFragment : Fragment(R.layout.fragment_hike_detail) {

    private var _binding: FragmentHikeDetailBinding? = null
    private val binding get() = _binding!!

    private val dateFmt = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())

    private val hikeId: String by lazy {
        requireArguments().getString(ExploreFragment.ARG_HIKE_ID).orEmpty()
    }

    private val viewModel: HikeDetailViewModel by viewModels {
        HikeDetailViewModel.Factory(hikeId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHikeDetailBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        fun ensureAuthed(): Boolean {
            val state = AuthManager.stateSnapshot()
            return when (state) {
                AuthState.Guest -> {
                    com.airbnb.core.ui.GuestPromptDialog.show(childFragmentManager)
                    false
                }
                is AuthState.Loading -> {
                    toast("Loading your account…")
                    false
                }
                is AuthState.Authenticated -> true
            }
        }

        binding.btnApply.setOnClickListener {
            if (!ensureAuthed()) return@setOnClickListener
            viewModel.applyToHike()
        }
        binding.btnCancelApplication.setOnClickListener {
            if (!ensureAuthed()) return@setOnClickListener
            viewModel.cancelMyBooking()
        }
        binding.btnLeaveHike.setOnClickListener {
            if (!ensureAuthed()) return@setOnClickListener
            viewModel.leaveHike()
        }
        binding.btnEdit.setOnClickListener {
            val bundle = Bundle().apply { putString(ExploreFragment.ARG_HIKE_ID, hikeId) }
            ProtectedNav.navigate(
                navController = findNavController(),
                destId = R.id.createEditHikeFragment,
                args = bundle,
                isProtected = true
            )
        }
        binding.btnApplicants.setOnClickListener {
            val bundle = Bundle().apply { putString(ExploreFragment.ARG_HIKE_ID, hikeId) }
            if (!ensureAuthed()) return@setOnClickListener
            findNavController().navigate(R.id.applicantsFragment, bundle)
        }
        binding.btnStartHike.setOnClickListener {
            if (!ensureAuthed()) return@setOnClickListener
            viewModel.startHike()
        }
        binding.btnEndHike.setOnClickListener {
            if (!ensureAuthed()) return@setOnClickListener
            viewModel.endHike()
        }
        binding.btnDeleteHike.setOnClickListener {
            if (!ensureAuthed()) return@setOnClickListener
            viewModel.deleteHike()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        if (state.shouldPop) {
                            findNavController().popBackStack()
                            return@collect
                        }

                        val hike = state.hike
                        if (hike == null) {
                            binding.tvTitle.text = "Loading…"
                            return@collect
                        }

                        // ── Core fields ───────────────────────────────
                        binding.tvTitle.text = hike.title
                        binding.tvStatus.text = "Status: ${hike.status.replaceFirstChar { it.uppercase() }}"
                        binding.tvLocation.text = hike.summaryLocation().ifBlank { hike.location }
                        binding.tvDifficulty.text = hike.difficulty.replaceFirstChar { it.uppercase() }
                        binding.tvDistance.text = "${hike.effectiveDistanceKm()} km"
                        binding.tvPrice.text = "₱%.2f".format(hike.price)
                        binding.tvMaxParticipants.text = hike.maxParticipants.toString()
                        binding.tvDescription.text = hike.description.ifBlank { "No description provided." }
                        binding.tvGuideName.text = "Guide: ${hike.guideName.ifBlank { hike.guideId }}"

                        // ── Participant count (current / max) ─────────
                        if (state.approvedCount > 0 || hike.maxParticipants > 0) {
                            binding.containerParticipantCount.visibility = View.VISIBLE
                            binding.tvParticipantCount.text = "${state.approvedCount} / ${hike.maxParticipants}"
                        } else {
                            binding.containerParticipantCount.visibility = View.GONE
                        }

                        // ── Meetup point ──────────────────────────────
                        if (hike.meetupPoint.isNotBlank()) {
                            binding.containerMeetup.visibility = View.VISIBLE
                            binding.tvMeetupPoint.text = hike.meetupPoint
                        } else {
                            binding.containerMeetup.visibility = View.GONE
                        }

                        // ── Destination ───────────────────────────────
                        if (hike.destination.isNotBlank()) {
                            binding.containerDestination.visibility = View.VISIBLE
                            binding.tvDestination.text = hike.destination
                        } else {
                            binding.containerDestination.visibility = View.GONE
                        }

                        // ── Elevation ─────────────────────────────────
                        if (hike.elevationM > 0) {
                            binding.containerElevation.visibility = View.VISIBLE
                            binding.tvElevation.text = "${hike.elevationM.toInt()} m"
                        } else {
                            binding.containerElevation.visibility = View.GONE
                        }

                        // ── Start date ────────────────────────────────
                        val startDate = hike.startDateTime?.toDate()
                        if (startDate != null) {
                            binding.containerStartDate.visibility = View.VISIBLE
                            binding.tvStartDate.text = dateFmt.format(startDate)
                        } else {
                            binding.containerStartDate.visibility = View.GONE
                        }

                        // ── End date ──────────────────────────────────
                        val endDate = hike.endDateTime?.toDate()
                        if (endDate != null) {
                            binding.containerEndDate.visibility = View.VISIBLE
                            binding.tvEndDate.text = dateFmt.format(endDate)
                        } else {
                            binding.containerEndDate.visibility = View.GONE
                        }

                        // ── Booking status badge ───────────────────────
                        val booking = state.myBooking
                        if (booking != null) {
                            binding.tvBookingStatus.visibility = View.VISIBLE
                            binding.tvBookingStatus.text =
                                "Your status: ${booking.status.replaceFirstChar { it.uppercase() }}"
                        } else {
                            binding.tvBookingStatus.visibility = View.GONE
                        }

                        // ── Hiker action buttons ──────────────────────
                        binding.btnApply.visibility =
                            if (state.showApply) View.VISIBLE else View.GONE
                        binding.btnCancelApplication.visibility =
                            if (state.showCancelApplication) View.VISIBLE else View.GONE
                        binding.btnLeaveHike.visibility =
                            if (state.showLeaveHike) View.VISIBLE else View.GONE

                        // ── Guide action buttons ──────────────────────
                        val showGuide = state.showGuideActions
                        binding.btnEdit.visibility =
                            if (showGuide) View.VISIBLE else View.GONE
                        binding.btnApplicants.visibility =
                            if (showGuide) View.VISIBLE else View.GONE
                        binding.btnStartHike.visibility =
                            if (state.showStartHike) View.VISIBLE else View.GONE
                        binding.btnEndHike.visibility =
                            if (state.showEndHike) View.VISIBLE else View.GONE
                        binding.btnDeleteHike.visibility =
                            if (showGuide) View.VISIBLE else View.GONE

                        state.message?.let { msg ->
                            toast(msg)
                            viewModel.consumeMessage()
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