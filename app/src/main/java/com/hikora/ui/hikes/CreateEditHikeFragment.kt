package com.hikora.ui.hikes

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.hikora.R
import com.hikora.databinding.FragmentCreateEditHikeBinding
import com.hikora.utils.HikeStatus
import kotlinx.coroutines.launch

class CreateEditHikeFragment : Fragment(R.layout.fragment_create_edit_hike) {

    private var _binding: FragmentCreateEditHikeBinding? = null
    private val binding get() = _binding!!

    private val existingHikeId: String?
        get() = arguments?.getString(HikesFragment.ARG_HIKE_ID)?.takeIf { it.isNotBlank() }

    private val viewModel: CreateEditHikeViewModel by viewModels {
        CreateEditHikeViewModel.Factory(existingHikeId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateEditHikeBinding.bind(view)

        val isEdit = existingHikeId != null
        binding.toolbar.title = if (isEdit) "Edit hike" else "Create hike"
        binding.btnDelete.visibility = if (isEdit) View.VISIBLE else View.GONE

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        val statusOptions = listOf(HikeStatus.OPEN, HikeStatus.CLOSED)
        binding.spinnerStatus.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            statusOptions
        )

        binding.etTitle.doAfterTextChanged { viewModel.updateTitle(it?.toString().orEmpty()) }
        binding.etDescription.doAfterTextChanged { viewModel.updateDescription(it?.toString().orEmpty()) }
        binding.etLocation.doAfterTextChanged { viewModel.updateLocation(it?.toString().orEmpty()) }
        binding.etDifficulty.doAfterTextChanged { viewModel.updateDifficulty(it?.toString().orEmpty()) }
        binding.etDistanceKm.doAfterTextChanged { viewModel.updateDistanceKm(it?.toString().orEmpty()) }
        binding.etPrice.doAfterTextChanged { viewModel.updatePrice(it?.toString().orEmpty()) }
        binding.etMaxParticipants.doAfterTextChanged { viewModel.updateMaxParticipants(it?.toString().orEmpty()) }

        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                viewModel.updateStatus(statusOptions[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.btnSave.setOnClickListener { viewModel.save() }
        binding.btnDelete.setOnClickListener { viewModel.deleteHike() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.form.collect { form ->
                    if (binding.etTitle.text?.toString() != form.title) binding.etTitle.setText(form.title)
                    if (binding.etDescription.text?.toString() != form.description) {
                        binding.etDescription.setText(form.description)
                    }
                    if (binding.etLocation.text?.toString() != form.location) binding.etLocation.setText(form.location)
                    if (binding.etDifficulty.text?.toString() != form.difficulty) {
                        binding.etDifficulty.setText(form.difficulty)
                    }
                    if (binding.etDistanceKm.text?.toString() != form.distanceKm) {
                        binding.etDistanceKm.setText(form.distanceKm)
                    }
                    if (binding.etPrice.text?.toString() != form.price) binding.etPrice.setText(form.price)
                    if (binding.etMaxParticipants.text?.toString() != form.maxParticipants) {
                        binding.etMaxParticipants.setText(form.maxParticipants)
                    }
                    val idx = statusOptions.indexOf(form.status).coerceAtLeast(0)
                    if (binding.spinnerStatus.selectedItemPosition != idx) {
                        binding.spinnerStatus.setSelection(idx, false)
                    }

                    binding.btnSave.isEnabled = !form.loading
                    binding.btnDelete.isEnabled = !form.loading

                    form.message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }
                    if (form.finished) {
                        findNavController().popBackStack()
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
