package com.airbnb.ui.hikes

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.R
import com.airbnb.core.auth.AuthState
import com.airbnb.core.navigation.ProtectedNav
import com.airbnb.core.ui.toast
import com.airbnb.databinding.FragmentApplicantsBinding
import com.airbnb.ui.explore.ExploreFragment
import com.airbnb.ui.hikes.adapter.ApplicantsAdapter
import kotlinx.coroutines.launch

class ApplicantsFragment : Fragment(R.layout.fragment_applicants) {

    private var deniedAccessHandled = false

    private var _binding: FragmentApplicantsBinding? = null
    private val binding get() = _binding!!

    private val hikeId: String by lazy {
        requireArguments().getString(ExploreFragment.ARG_HIKE_ID).orEmpty()
    }

    private val viewModel: ApplicantsViewModel by viewModels {
        ApplicantsViewModel.Factory(hikeId)
    }

    private val adapter = ApplicantsAdapter(
        onApprove = { viewModel.approve(it.id) },
        onReject = { viewModel.reject(it.id) }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentApplicantsBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.recyclerApplicants.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerApplicants.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.managementEnabled = state.canManage
                    adapter.submitList(state.bookings)

                    // Only deny access once we know the auth/profile state.
                    if (state.hike != null && !state.canManage && !deniedAccessHandled) {
                        when (state.authState) {
                            AuthState.Guest -> {
                                deniedAccessHandled = true
                                val args = Bundle().apply { putString(ExploreFragment.ARG_HIKE_ID, hikeId) }
                                ProtectedNav.navigate(
                                    navController = findNavController(),
                                    destId = R.id.applicantsFragment,
                                    args = args,
                                    isProtected = true
                                )
                            }
                            is AuthState.Loading -> {
                                // Keep the screen; wait for profile to resolve.
                            }
                            is AuthState.Authenticated -> {
                                if ((state.authState as AuthState.Authenticated).user == null) {
                                    // Profile unresolved; don't treat as “no access” yet.
                                } else {
                                    deniedAccessHandled = true
                                    toast(
                                        "You do not have access to manage applicants for this hike.",
                                        duration = android.widget.Toast.LENGTH_LONG
                                    )
                                    findNavController().popBackStack()
                                }
                            }
                        }
                    }

                    state.message?.let {
                        toast(it)
                        viewModel.consumeMessage()
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
