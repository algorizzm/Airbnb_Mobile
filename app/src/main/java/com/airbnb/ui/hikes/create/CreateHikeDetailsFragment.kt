package com.airbnb.ui.hikes.create

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.R
import com.airbnb.databinding.FragmentCreateHikeDetailsBinding
import kotlinx.coroutines.launch
import com.google.android.material.chip.Chip

class CreateHikeDetailsFragment : Fragment(R.layout.fragment_create_hike_details) {

    private var _binding: FragmentCreateHikeDetailsBinding? = null
    private val binding get() = _binding!!

    private val flowVm get() = createHikeFlowViewModel()

    private var tagsExpanded = false
    private var descriptionExpanded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCreateHikeDetailsBinding.bind(view)

        setupInputs()
        setupExpandableDescription()
        setupTagsToggle()
        observeUi()
    }

    private fun setupInputs() {

        binding.etTitle.doAfterTextChanged {
            flowVm.updateTitle(it?.toString().orEmpty())
        }

        binding.etDescription.doAfterTextChanged {
            flowVm.updateDescription(it?.toString().orEmpty())
        }

        var participantCount =
            binding.tvParticipantCount.text.toString().toIntOrNull() ?: 1

        binding.tvParticipantCount.text = participantCount.toString()

        flowVm.updateMaxSlotsText(participantCount.toString())

        binding.btnIncrement.setOnClickListener {

            if (participantCount < 25) {

                participantCount++

                binding.tvParticipantCount.text = participantCount.toString()

                flowVm.updateMaxSlotsText(participantCount.toString())
            }
        }

        binding.btnDecrement.setOnClickListener {

            if (participantCount > 1) {

                participantCount--

                binding.tvParticipantCount.text = participantCount.toString()

                flowVm.updateMaxSlotsText(participantCount.toString())
            }
        }
    }

    private fun setupExpandableDescription() {

        binding.etDescription.setOnClickListener {

            if (descriptionExpanded) {
                collapseDescription()
            } else {
                expandDescription()
            }
        }
    }

    private fun expandDescription() {

        descriptionExpanded = true

        val params = binding.etDescription.layoutParams
        params.height = dpToPx(180)
        binding.etDescription.layoutParams = params

        binding.etDescription.maxLines = 8
    }

    private fun collapseDescription() {

        descriptionExpanded = false

        val params = binding.etDescription.layoutParams
        params.height = dpToPx(90)
        binding.etDescription.layoutParams = params

        binding.etDescription.maxLines = 3
    }

    private fun setupTagsToggle() {

        binding.tvToggleTags.setOnClickListener {

            tagsExpanded = !tagsExpanded

            if (tagsExpanded) {

                binding.chipGroupTagsExpanded.visibility = View.VISIBLE
                binding.tvToggleTags.text = "Show Less"

            } else {

                binding.chipGroupTagsExpanded.visibility = View.GONE
                binding.tvToggleTags.text = "Show More"
            }
        }

        setupTagSelectionLimit(binding.chipGroupTagsPrimary)
        setupTagSelectionLimit(binding.chipGroupTagsExpanded)
    }

    private fun setupTagSelectionLimit(chipGroup: ViewGroup) {

        for (i in 0 until chipGroup.childCount) {

            val chip = chipGroup.getChildAt(i) as? Chip ?: continue

            chip.setOnCheckedChangeListener { buttonView, isChecked ->

                val selectedTags = getSelectedTagCount()

                if (isChecked && selectedTags > 3) {

                    buttonView.isChecked = false
                }

                updateSelectedTags()
            }
        }
    }

    private fun updateSelectedTags() {

        val selectedTags = getAllChips()
            .filter { chip -> chip.isChecked }
            .joinToString(", ") { chip -> chip.text.toString() }

        flowVm.updateTagsText(selectedTags)
    }

    private fun getSelectedTagCount(): Int {

        return getAllChips()
            .count { it.isChecked }
    }

    private fun getAllChips(): List<Chip> {

        val chips = mutableListOf<Chip>()

        for (i in 0 until binding.chipGroupTagsPrimary.childCount) {

            val chip = binding.chipGroupTagsPrimary.getChildAt(i)

            if (chip is Chip) {
                chips.add(chip)
            }
        }

        for (i in 0 until binding.chipGroupTagsExpanded.childCount) {

            val chip = binding.chipGroupTagsExpanded.getChildAt(i)

            if (chip is Chip) {
                chips.add(chip)
            }
        }

        return chips
    }

    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                flowVm.ui.collect { s ->

                    if (binding.etTitle.text?.toString() != s.title) {
                        binding.etTitle.setText(s.title)
                    }

                    if (binding.etDescription.text?.toString() != s.description) {
                        binding.etDescription.setText(s.description)
                    }

                    if (binding.tvParticipantCount.text.toString() != s.maxSlotsText &&
                        s.maxSlotsText.isNotEmpty()
                    ) {

                        binding.tvParticipantCount.text = s.maxSlotsText
                    }
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}