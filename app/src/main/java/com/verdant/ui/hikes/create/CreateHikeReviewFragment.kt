package com.verdant.ui.hikes.create

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.verdant.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.verdant.databinding.FragmentCreateHikeReviewBinding
import kotlinx.coroutines.launch
import java.text.DateFormat

class CreateHikeReviewFragment : Fragment(R.layout.fragment_create_hike_review) {

    private var _binding: FragmentCreateHikeReviewBinding? = null
    private val binding get() = _binding!!

    private val flowVm get() = createHikeFlowViewModel()

    private val fmt: DateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreateHikeReviewBinding.bind(view)

        binding.btnPublish.setOnClickListener { flowVm.publish() }
        binding.btnSaveDraft.setOnClickListener { flowVm.saveDraft() }
        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete hike")
                .setMessage("This will permanently delete the hike and all its bookings.")
                .setPositiveButton("Delete") { _, _ -> flowVm.deleteHike() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                flowVm.ui.collect { s ->
                    binding.btnDelete.visibility =
                        if (s.isEditMode) View.VISIBLE else View.GONE
                    binding.tvSummary.text = buildSummary(s)
                }
            }
        }
    }

    private fun buildSummary(s: CreateHikeUiState): String = buildString {
        appendLine("Title: ${s.title}")
        appendLine("Description: ${s.description}")
        appendLine("Max slots: ${s.maxSlotsText}")
        appendLine("Inclusions: ${s.inclusionsText.ifBlank { "—" }}")
        appendLine("Requirements: ${s.requirementsText.ifBlank { "—" }}")
        appendLine("Tags: ${s.tagsText.ifBlank { "—" }}")
        appendLine(
            "Start: ${s.startMillis?.let { fmt.format(it) } ?: "—"}"
        )
        appendLine("End: ${s.endMillis?.let { fmt.format(it) } ?: "—"}")
        appendLine("Meetup: ${s.meetupPoint}")
        appendLine("Destination: ${s.destination}")
        appendLine("Difficulty: ${s.routeDifficulty}")
        appendLine("Elevation (m): ${s.elevationMText.ifBlank { "—" }}")
        appendLine("Distance (km): ${s.estimatedDistanceKmText}")
        appendLine("Cover: ${if (s.coverImageUrl.isNotBlank()) "set" else "—"}")
        appendLine("Gallery: ${s.galleryImageUrls.count { it.isNotBlank() }} item(s)")
        appendLine("Price: ${s.priceText}")
        appendLine("Payment methods: ${s.paymentMethodsText.ifBlank { "—" }}")
        appendLine("Pricing notes: ${s.pricingNotes.ifBlank { "—" }}")
    }.trim()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
