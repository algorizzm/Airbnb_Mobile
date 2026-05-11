package com.verdant.ui.hikes

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
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
import com.verdant.ui.explore.ExploreFragment
import com.verdant.utils.HikeStatus
import kotlinx.coroutines.launch

class CreateEditHikeFragment : Fragment(R.layout.fragment_create_edit_hike) {

    private var _binding: FragmentCreateEditHikeBinding? = null
    private val binding get() = _binding!!

    private val existingHikeId: String?
        get() = arguments?.getString(ExploreFragment.ARG_HIKE_ID)?.takeIf { it.isNotBlank() }

    private val viewModel: CreateEditHikeViewModel by viewModels {
        CreateEditHikeViewModel.Factory(existingHikeId)
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@registerForActivityResult
        binding.layoutImageHint.visibility = View.GONE
        binding.progressImage.visibility = View.VISIBLE
        Glide.with(this).load(uri).centerCrop().into(binding.imgHikePreview)
        viewModel.uploadImage(uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateEditHikeBinding.bind(view)

        val isEdit = existingHikeId != null
        binding.tvTitle.text = if (isEdit) "Edit Hike" else "Create Hike"
        binding.btnSave.text = if (isEdit) "Save Changes" else "Publish Hike"
        binding.btnDelete.visibility = if (isEdit) View.VISIBLE else View.GONE

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // ── Image picker ─────────────────────────────────────────────────────
        binding.imgPickerArea.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // ── Text fields ──────────────────────────────────────────────────────
        binding.etTitle.doAfterTextChanged { viewModel.updateTitle(it?.toString().orEmpty()) }
        binding.etDescription.doAfterTextChanged { viewModel.updateDescription(it?.toString().orEmpty()) }
        binding.etLocation.doAfterTextChanged { viewModel.updateLocation(it?.toString().orEmpty()) }
        binding.etDistanceKm.doAfterTextChanged { viewModel.updateDistanceKm(it?.toString().orEmpty()) }
        binding.etPrice.doAfterTextChanged { viewModel.updatePrice(it?.toString().orEmpty()) }
        binding.etDurationHours.doAfterTextChanged { viewModel.updateDurationHours(it?.toString().orEmpty()) }
        binding.etMaxParticipants.doAfterTextChanged { viewModel.updateMaxParticipants(it?.toString().orEmpty()) }

        // ── Difficulty chips ─────────────────────────────────────────────────
        binding.chipGroupDifficulty.setOnCheckedStateChangeListener { _, checkedIds ->
            val difficulty = when (checkedIds.firstOrNull()) {
                R.id.chipEasy     -> "Easy"
                R.id.chipModerate -> "Moderate"
                R.id.chipHard     -> "Hard"
                R.id.chipExpert   -> "Expert"
                else              -> "Moderate"
            }
            viewModel.updateDifficulty(difficulty)
        }

        // ── Status chips ─────────────────────────────────────────────────────
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val status = when (checkedIds.firstOrNull()) {
                R.id.chipOpen   -> HikeStatus.OPEN
                R.id.chipClosed -> HikeStatus.CLOSED
                else            -> HikeStatus.OPEN
            }
            viewModel.updateStatus(status)
        }

        // ── Save ─────────────────────────────────────────────────────────────
        binding.btnSave.setOnClickListener { viewModel.save() }

        // ── Delete with confirmation ──────────────────────────────────────────
        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Hike")
                .setMessage("This will permanently delete the hike and all its bookings. This cannot be undone.")
                .setPositiveButton("Delete") { _, _ -> viewModel.deleteHike() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // ── Observe state ─────────────────────────────────────────────────────
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.form.collect { form ->

                    // Text fields — only update if different to avoid cursor jumping
                    if (binding.etTitle.text?.toString() != form.title)
                        binding.etTitle.setText(form.title)
                    if (binding.etDescription.text?.toString() != form.description)
                        binding.etDescription.setText(form.description)
                    if (binding.etLocation.text?.toString() != form.location)
                        binding.etLocation.setText(form.location)
                    if (binding.etDistanceKm.text?.toString() != form.distanceKm)
                        binding.etDistanceKm.setText(form.distanceKm)
                    if (binding.etPrice.text?.toString() != form.price)
                        binding.etPrice.setText(form.price)
                    if (binding.etDurationHours.text?.toString() != form.durationHours)
                        binding.etDurationHours.setText(form.durationHours)
                    if (binding.etMaxParticipants.text?.toString() != form.maxParticipants)
                        binding.etMaxParticipants.setText(form.maxParticipants)

                    // Difficulty chip
                    val diffChipId = when (form.difficulty.lowercase()) {
                        "easy"   -> R.id.chipEasy
                        "hard"   -> R.id.chipHard
                        "expert" -> R.id.chipExpert
                        else     -> R.id.chipModerate
                    }
                    if (binding.chipGroupDifficulty.checkedChipId != diffChipId)
                        binding.chipGroupDifficulty.check(diffChipId)

                    // Status chip
                    val statusChipId = if (form.status == HikeStatus.CLOSED)
                        R.id.chipClosed else R.id.chipOpen
                    if (binding.chipGroupStatus.checkedChipId != statusChipId)
                        binding.chipGroupStatus.check(statusChipId)

                    // Cover image
                    if (form.imageUrl.isNotBlank()) {
                        binding.layoutImageHint.visibility = View.GONE
                        binding.progressImage.visibility = View.GONE
                        Glide.with(this@CreateEditHikeFragment)
                            .load(form.imageUrl)
                            .centerCrop()
                            .into(binding.imgHikePreview)
                    }

                    // Loading state
                    val loading = form.loading
                    binding.progressSaving.visibility = if (loading) View.VISIBLE else View.GONE
                    binding.btnSave.isEnabled = !loading
                    binding.btnDelete.isEnabled = !loading
                    binding.imgPickerArea.isEnabled = !loading
                    binding.progressImage.visibility =
                        if (loading && form.imageUrl.isBlank()) View.VISIBLE else View.GONE

                    // Toast
                    form.message?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.consumeMessage()
                    }

                    if (form.finished) findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
