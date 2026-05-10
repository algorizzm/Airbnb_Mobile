package com.verdant.ui.hikes

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.verdant.R
import com.verdant.core.auth.AuthManager
import com.verdant.core.auth.AuthState
import com.verdant.core.navigation.ProtectedNav
import com.verdant.core.ui.toast
import com.verdant.databinding.FragmentHikeDetailBinding
import kotlinx.coroutines.launch

class HikeDetailFragment : Fragment(R.layout.fragment_hike_detail) {

    private var _binding: FragmentHikeDetailBinding? = null
    private val binding get() = _binding!!

    private val hikeId: String by lazy {
        requireArguments().getString(HikesFragment.ARG_HIKE_ID).orEmpty()
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

        fun ensureAuthedOrRedirect(
            intendedDestId: Int,
            intendedArgs: Bundle?
        ): Boolean {
            val state = AuthManager.stateSnapshot()
            return when (state) {
                AuthState.Guest -> {
                    com.verdant.core.ui.GuestPromptDialog.show(childFragmentManager)
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
            val args = Bundle().apply { putString(HikesFragment.ARG_HIKE_ID, hikeId) }
            if (!ensureAuthedOrRedirect(R.id.hikeDetailFragment, args)) return@setOnClickListener
            viewModel.applyToHike()
        }
        binding.btnCancelApplication.setOnClickListener {
            val args = Bundle().apply { putString(HikesFragment.ARG_HIKE_ID, hikeId) }
            if (!ensureAuthedOrRedirect(R.id.hikeDetailFragment, args)) return@setOnClickListener
            viewModel.cancelMyBooking()
        }
        binding.btnLeaveHike.setOnClickListener {
            val args = Bundle().apply { putString(HikesFragment.ARG_HIKE_ID, hikeId) }
            if (!ensureAuthedOrRedirect(R.id.hikeDetailFragment, args)) return@setOnClickListener
            viewModel.leaveHike()
        }

        binding.btnEdit.setOnClickListener {
            val bundle = Bundle().apply { putString(HikesFragment.ARG_HIKE_ID, hikeId) }
            ProtectedNav.navigate(
                navController = findNavController(),
                destId = R.id.createEditHikeFragment,
                args = bundle,
                isProtected = true
            )
        }
        binding.btnApplicants.setOnClickListener {
            val bundle = Bundle().apply { putString(HikesFragment.ARG_HIKE_ID, hikeId) }
            if (!ensureAuthedOrRedirect(R.id.applicantsFragment, bundle)) return@setOnClickListener
            findNavController().navigate(R.id.applicantsFragment, bundle)
        }
        binding.btnStartHike.setOnClickListener {
            val args = Bundle().apply { putString(HikesFragment.ARG_HIKE_ID, hikeId) }
            if (!ensureAuthedOrRedirect(R.id.hikeDetailFragment, args)) return@setOnClickListener
            viewModel.startHike()
        }
        binding.btnEndHike.setOnClickListener {
            val args = Bundle().apply { putString(HikesFragment.ARG_HIKE_ID, hikeId) }
            if (!ensureAuthedOrRedirect(R.id.hikeDetailFragment, args)) return@setOnClickListener
            viewModel.endHike()
        }
        binding.btnDeleteHike.setOnClickListener {
            val args = Bundle().apply { putString(HikesFragment.ARG_HIKE_ID, hikeId) }
            if (!ensureAuthedOrRedirect(R.id.hikeDetailFragment, args)) return@setOnClickListener
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
                        binding.tvTitle.text = hike.title
                        binding.tvStatus.text = "Status: ${hike.status}"
                        binding.tvLocation.text = hike.location
                        binding.tvDifficulty.text = "Difficulty: ${hike.difficulty}"
                        binding.tvDistance.text = "Distance: ${hike.distanceKm} km"
                        binding.tvPrice.text = "Price: ₱%.2f".format(hike.price)
                        binding.tvMaxParticipants.text = "Max participants: ${hike.maxParticipants}"
                        binding.tvDescription.text = hike.description
                        binding.tvGuideName.text = hike.guideName.ifBlank { hike.guideId }

                        val booking = state.myBooking
                        if (booking != null) {
                            binding.tvBookingStatus.visibility = View.VISIBLE
                            binding.tvBookingStatus.text = "Your booking: ${booking.status}"
                        } else {
                            binding.tvBookingStatus.visibility = View.GONE
                        }

                        binding.btnApply.visibility = if (state.showApply) View.VISIBLE else View.GONE
                        binding.btnCancelApplication.visibility =
                            if (state.showCancelApplication) View.VISIBLE else View.GONE
                        binding.btnLeaveHike.visibility = if (state.showLeaveHike) View.VISIBLE else View.GONE

                        val showGuide = state.showGuideActions
                        binding.btnEdit.visibility = if (showGuide) View.VISIBLE else View.GONE
                        binding.btnApplicants.visibility = if (showGuide) View.VISIBLE else View.GONE
                        binding.btnStartHike.visibility = if (state.showStartHike) View.VISIBLE else View.GONE
                        binding.btnEndHike.visibility = if (state.showEndHike) View.VISIBLE else View.GONE
                        binding.btnDeleteHike.visibility = if (showGuide) View.VISIBLE else View.GONE

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