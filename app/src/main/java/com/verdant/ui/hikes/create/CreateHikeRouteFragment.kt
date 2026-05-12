package com.verdant.ui.hikes.create

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.verdant.R
import com.verdant.databinding.FragmentCreateHikeRouteBinding
import com.verdant.utils.HikeDifficulty
import kotlinx.coroutines.launch

class CreateHikeRouteFragment : Fragment(R.layout.fragment_create_hike_route) {

    private var _binding: FragmentCreateHikeRouteBinding? = null
    private val binding get() = _binding!!

    private val flowVm get() = createHikeFlowViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateHikeRouteBinding.bind(view)

        binding.etMeetup.doAfterTextChanged { flowVm.updateMeetupPoint(it?.toString().orEmpty()) }
        binding.etDestination.doAfterTextChanged { flowVm.updateDestination(it?.toString().orEmpty()) }
        binding.etElevation.doAfterTextChanged { flowVm.updateElevationMText(it?.toString().orEmpty()) }
        binding.etDistanceKm.doAfterTextChanged {
            flowVm.updateEstimatedDistanceKmText(it?.toString().orEmpty())
        }

        binding.chipGroupDifficulty.setOnCheckedStateChangeListener { _, checkedIds ->
            val diff = when (checkedIds.firstOrNull()) {
                R.id.chipBeginner -> HikeDifficulty.BEGINNER
                R.id.chipAdvanced -> HikeDifficulty.ADVANCED
                R.id.chipIntermediate -> HikeDifficulty.INTERMEDIATE
                else -> HikeDifficulty.BEGINNER
            }
            flowVm.updateRouteDifficulty(diff)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flowVm.ui.collect { s ->
                    if (binding.etMeetup.text?.toString() != s.meetupPoint) {
                        binding.etMeetup.setText(s.meetupPoint)
                    }
                    if (binding.etDestination.text?.toString() != s.destination) {
                        binding.etDestination.setText(s.destination)
                    }
                    if (binding.etElevation.text?.toString() != s.elevationMText) {
                        binding.etElevation.setText(s.elevationMText)
                    }
                    if (binding.etDistanceKm.text?.toString() != s.estimatedDistanceKmText) {
                        binding.etDistanceKm.setText(s.estimatedDistanceKmText)
                    }
                    val chip = when (s.routeDifficulty) {
                        HikeDifficulty.ADVANCED -> R.id.chipAdvanced
                        HikeDifficulty.INTERMEDIATE -> R.id.chipIntermediate
                        else -> R.id.chipBeginner
                    }
                    if (binding.chipGroupDifficulty.checkedChipId != chip) {
                        binding.chipGroupDifficulty.check(chip)
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
