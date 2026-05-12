package com.verdant.ui.hikes.create

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.verdant.R
import com.verdant.databinding.FragmentCreateHikeDetailsBinding
import kotlinx.coroutines.launch

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

        binding.etMaxSlots.doAfterTextChanged {
            flowVm.updateMaxSlotsText(it?.toString().orEmpty())
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

                    if (binding.etMaxSlots.text?.toString() != s.maxSlotsText) {
                        binding.etMaxSlots.setText(s.maxSlotsText)
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