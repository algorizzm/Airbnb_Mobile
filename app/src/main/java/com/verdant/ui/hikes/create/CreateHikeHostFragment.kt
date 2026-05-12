package com.verdant.ui.hikes.create

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.verdant.R
import com.verdant.databinding.FragmentCreateHikeHostBinding
import com.verdant.ui.explore.ExploreFragment
import kotlinx.coroutines.launch
import androidx.fragment.app.activityViewModels

class CreateHikeHostFragment : Fragment(R.layout.fragment_create_hike_host) {

    private var _binding: FragmentCreateHikeHostBinding? = null
    private val binding get() = _binding!!

    private val existingHikeId: String?
        get() = arguments?.getString(ExploreFragment.ARG_HIKE_ID)?.takeIf { it.isNotBlank() }

    private val viewModel: CreateHikeFlowViewModel by activityViewModels {
        CreateHikeFlowViewModel.Factory(existingHikeId)
    }

    private var lastStepIndex: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateHikeHostBinding.bind(view)

        val isEdit = existingHikeId != null
        binding.tvScreenTitle.text = if (isEdit) "Edit hike" else "Create hike"

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnPrev.setOnClickListener {
            viewModel.goBack()
        }

        binding.btnNext.setOnClickListener {
            viewModel.goNext()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.ui.collect { state ->
                    binding.tvStepLabel.text =
                        "Step ${state.stepIndex + 1} of 6 · ${state.step.name}"
                    binding.btnPrev.visibility =
                        if (state.stepIndex > 0) View.VISIBLE else View.INVISIBLE
                    binding.btnNext.visibility =
                        if (state.step == CreateHikeStep.REVIEW) View.GONE else View.VISIBLE
                    binding.progressGlobal.visibility =
                        if (state.loading) View.VISIBLE else View.GONE

                    if (lastStepIndex != state.stepIndex) {
                        lastStepIndex = state.stepIndex
                        showStep(state.step)
                    }

                    state.message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }

                    if (state.finished) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    private fun showStep(step: CreateHikeStep) {
        val frag: Fragment = when (step) {
            CreateHikeStep.DETAILS -> CreateHikeDetailsFragment()
            CreateHikeStep.SCHEDULE -> CreateHikeScheduleFragment()
            CreateHikeStep.ROUTE -> CreateHikeRouteFragment()
            CreateHikeStep.MEDIA -> CreateHikeMediaFragment()
            CreateHikeStep.PRICING -> CreateHikePricingFragment()
            CreateHikeStep.REVIEW -> CreateHikeReviewFragment()
        }
        childFragmentManager.commit {
            replace(R.id.stepContainer, frag, step.name)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
