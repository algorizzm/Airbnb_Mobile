package com.verdant.ui.hikes

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.verdant.R
import com.verdant.databinding.FragmentCreateEditHikeBinding
import com.verdant.utils.HikeStatus
import kotlinx.coroutines.launch

class CreateEditHikeFragment : Fragment(R.layout.fragment_create_edit_hike) {

    private var _binding: FragmentCreateEditHikeBinding? = null
    private val binding get() = _binding!!

    private val existingHikeId: String?
        get() = arguments?.getString(HikesFragment.ARG_HIKE_ID)?.takeIf { it.isNotBlank() }

    private val viewModel: CreateEditHikeViewModel by viewModels {
        CreateEditHikeViewModel.Factory(existingHikeId)
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            viewModel.uploadImage(uri)
            binding.imgHikePreview.visibility = View.VISIBLE
            Glide.with(this).load(uri).centerCrop().into(binding.imgHikePreview)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateEditHikeBinding.bind(view)

        val isEdit = existingHikeId != null
        binding.tvTitle.text = if (isEdit) "Edit Hike" else "Create Hike"
        binding.btnDelete.visibility = if (isEdit) View.VISIBLE else View.GONE

        binding.btnBack.setOnClickListener {
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
        binding.etDurationHours.doAfterTextChanged { viewModel.updateDurationHours(it?.toString().orEmpty()) }
        binding.etMaxParticipants.doAfterTextChanged { viewModel.updateMaxParticipants(it?.toString().orEmpty()) }

        binding.spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                viewModel.updateStatus(statusOptions[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.btnUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            imagePickerLauncher.launch(intent)
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
                    if (binding.etDurationHours.text?.toString() != form.durationHours) {
                        binding.etDurationHours.setText(form.durationHours)
                    }
                    if (binding.etMaxParticipants.text?.toString() != form.maxParticipants) {
                        binding.etMaxParticipants.setText(form.maxParticipants)
                    }
                    val idx = statusOptions.indexOf(form.status).coerceAtLeast(0)
                    if (binding.spinnerStatus.selectedItemPosition != idx) {
                        binding.spinnerStatus.setSelection(idx, false)
                    }

                    if (form.imageUrl.isNotBlank()) {
                        binding.imgHikePreview.visibility = View.VISIBLE
                        Glide.with(this@CreateEditHikeFragment)
                            .load(form.imageUrl)
                            .centerCrop()
                            .into(binding.imgHikePreview)
                    }

                    binding.btnSave.isEnabled = !form.loading
                    binding.btnDelete.isEnabled = !form.loading
                    binding.btnUploadImage.isEnabled = !form.loading

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