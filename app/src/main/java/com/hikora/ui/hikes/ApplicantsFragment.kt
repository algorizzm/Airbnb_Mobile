package com.hikora.ui.hikes

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.hikora.R
import com.hikora.databinding.FragmentApplicantsBinding
import com.hikora.ui.hikes.adapter.ApplicantsAdapter
import kotlinx.coroutines.launch

class ApplicantsFragment : Fragment(R.layout.fragment_applicants) {

    private var deniedAccessHandled = false

    private var _binding: FragmentApplicantsBinding? = null
    private val binding get() = _binding!!

    private val hikeId: String by lazy {
        requireArguments().getString(HikesFragment.ARG_HIKE_ID).orEmpty()
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

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.recyclerApplicants.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerApplicants.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.managementEnabled = state.canManage
                    adapter.submitList(state.bookings)
                    if (state.hike != null && !state.canManage && !deniedAccessHandled) {
                        deniedAccessHandled = true
                        Toast.makeText(
                            requireContext(),
                            "You do not have access to manage applicants for this hike.",
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().popBackStack()
                    }
                    state.message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
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
