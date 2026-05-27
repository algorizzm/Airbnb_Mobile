package com.airbnb.ui.hikes.create

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.airbnb.R
import com.airbnb.databinding.FragmentCreateHikeReviewBinding
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale

class CreateHikeReviewFragment : Fragment(R.layout.fragment_create_hike_review) {

    private var _binding: FragmentCreateHikeReviewBinding? = null
    private val binding get() = _binding!!

    private val flowVm get() = createHikeFlowViewModel()

    private val fmt: DateFormat =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)

    private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCreateHikeReviewBinding.bind(view)

        setupListeners()
        observeUi()
    }

    private fun setupListeners() {

        binding.btnPublish.setOnClickListener {
            flowVm.publish()
        }

        binding.btnSaveDraft.setOnClickListener {
            flowVm.saveDraft()
        }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete hike")
                .setMessage("This will permanently delete the hike and all its bookings.")
                .setPositiveButton("Delete") { _, _ ->
                    flowVm.deleteHike()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                flowVm.ui.collect { s ->

                    // DELETE BUTTON
                    binding.btnDelete.isVisible = s.isEditMode

                    // COVER TITLE
                    binding.tvReviewTitle.text =
                        s.title.ifBlank { "Untitled Hike" }

                    // SCHEDULE
                    binding.tvReviewSchedule.text =
                        buildScheduleText(s)

                    // DESCRIPTION
                    binding.tvReviewDescription.text =
                        s.description.ifBlank {
                            "No description added."
                        }

                    // DETAILS
                    binding.tvReviewDestination.text =
                        s.destination.ifBlank { "Not set" }

                    binding.tvReviewMeetup.text =
                        s.meetupPoint.ifBlank { "Not set" }

                    binding.tvReviewDifficulty.text =
                        s.routeDifficulty.ifBlank { "Beginner" }

                    binding.tvReviewElevation.text =
                        if (s.elevationMText.isBlank()) {
                            "0 m"
                        } else {
                            "${s.elevationMText} m"
                        }

                    binding.tvReviewDistance.text =
                        if (s.estimatedDistanceKmText.isBlank()) {
                            "0 km"
                        } else {
                            "${s.estimatedDistanceKmText} km"
                        }

                    binding.tvReviewSlots.text =
                        s.maxSlotsText.ifBlank { "0" }

                    // TAGS
                    setupTags(s.tagsText)

                    // PRICE
                    binding.tvReviewPrice.text =
                        formatPrice(s.priceText)

                    // PAYMENT METHODS
                    binding.tvReviewPaymentMethods.text =
                        s.paymentMethodsText.ifBlank {
                            "No payment methods selected"
                        }

                    // PRICING NOTES
                    binding.tvReviewPricingNotes.text =
                        s.pricingNotes.ifBlank {
                            "No pricing notes."
                        }

                    // TODO:
                    // Load cover image here using Glide / Coil / Picasso
                    //
                    // Example:
                    // Glide.with(requireContext())
                    //     .load(s.coverImageUrl)
                    //     .placeholder(R.drawable.placeholder_hike)
                    //     .into(binding.imgReviewCover)
                }
            }
        }
    }

    private fun buildScheduleText(s: CreateHikeUiState): String {

        val start = s.startMillis?.let { fmt.format(it) }
        val end = s.endMillis?.let { fmt.format(it) }

        return when {
            start != null && end != null -> "$start • $end"
            start != null -> start
            else -> "No schedule selected"
        }
    }

    private fun setupTags(tagsText: String) {

        binding.chipGroupReviewTags.removeAllViews()

        val tags = tagsText
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        tags.forEach { tag ->

            val chip = Chip(requireContext()).apply {

                text = tag

                isClickable = false
                isCheckable = false
                isCloseIconVisible = false

                // TEXT
                setTextColor(
                    resources.getColor(android.R.color.white, null)
                )

                textSize = 12f

                // FONT
                typeface =
                    androidx.core.content.res.ResourcesCompat.getFont(
                        requireContext(),
                        R.font.poppins_medium
                    )

                // DARK BACKGROUND
                chipBackgroundColor =
                    androidx.core.content.ContextCompat.getColorStateList(
                        requireContext(),
                        R.color.chip_state_color
                    )

                // GREEN OUTLINE
                chipStrokeWidth = 1f

                chipStrokeColor =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#02D083")
                    )

                // ROUNDING
                chipCornerRadius = 50f

                // REMOVE EXTRA TOUCH PADDING
                setEnsureMinTouchTargetSize(false)

                // OPTIONAL:
                // tighter visual appearance
                chipStartPadding = 14f
                chipEndPadding = 14f
            }

            binding.chipGroupReviewTags.addView(chip)
        }
    }

    private fun formatPrice(price: String): String {

        if (price.isBlank()) {
            return "₱ 0.00"
        }

        return try {

            val amount = price.toDouble()
            pesoFormat.format(amount)

        } catch (_: Exception) {

            "₱ $price"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}