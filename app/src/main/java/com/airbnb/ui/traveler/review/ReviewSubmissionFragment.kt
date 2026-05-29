package com.airbnb.ui.traveler.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.databinding.FragmentReviewSubmissionBinding
import com.airbnb.utils.formatting.DateFormatter
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch

/**
 * Fragment for submitting a review after completing a stay.
 * 
 * Features:
 * - Displays listing snapshot
 * - Overall rating selector
 * - Category-specific ratings
 * - Optional comment input
 * - Validation and submission
 */
class ReviewSubmissionFragment : Fragment() {
    
    private var _binding: FragmentReviewSubmissionBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ReviewSubmissionViewModel by viewModels()
    
    private val reservationId: String by lazy {
        arguments?.getString("reservationId") ?: ""
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewSubmissionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupCategoryRatings()
        setupOverallRating()
        setupSubmitButton()
        setupBackButton()
        observeViewModel()
        
        // Load reservation
        viewModel.loadReservation(reservationId)
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupCategoryRatings() {
        // Setup category rating views
        setupCategoryRating(
            binding.cleanlinessRating.root,
            "Cleanliness"
        )
        setupCategoryRating(
            binding.communicationRating.root,
            "Communication"
        )
        setupCategoryRating(
            binding.checkInRating.root,
            "Check-in"
        )
        setupCategoryRating(
            binding.accuracyRating.root,
            "Accuracy"
        )
        setupCategoryRating(
            binding.locationRating.root,
            "Location"
        )
        setupCategoryRating(
            binding.valueRating.root,
            "Value"
        )
    }
    
    private fun setupCategoryRating(view: View, categoryName: String) {
        val nameTextView = view.findViewById<TextView>(R.id.categoryNameTextView)
        nameTextView.text = categoryName
    }
    
    private fun setupOverallRating() {
        binding.overallRatingBar.setOnRatingBarChangeListener { _, rating, _ ->
            val stars = rating.toInt()
            binding.overallRatingTextView.text = when (stars) {
                1 -> "1 star"
                else -> "$stars stars"
            }
        }
    }
    
    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            submitReview()
        }
    }
    
    private fun submitReview() {
        val overallRating = binding.overallRatingBar.rating.toInt()
        val cleanlinessRating = binding.cleanlinessRating.root
            .findViewById<RatingBar>(R.id.categoryRatingBar).rating.toInt()
        val communicationRating = binding.communicationRating.root
            .findViewById<RatingBar>(R.id.categoryRatingBar).rating.toInt()
        val checkInRating = binding.checkInRating.root
            .findViewById<RatingBar>(R.id.categoryRatingBar).rating.toInt()
        val accuracyRating = binding.accuracyRating.root
            .findViewById<RatingBar>(R.id.categoryRatingBar).rating.toInt()
        val locationRating = binding.locationRating.root
            .findViewById<RatingBar>(R.id.categoryRatingBar).rating.toInt()
        val valueRating = binding.valueRating.root
            .findViewById<RatingBar>(R.id.categoryRatingBar).rating.toInt()
        val comment = binding.commentEditText.text?.toString() ?: ""
        
        // Validate
        if (overallRating < 1) {
            Toast.makeText(requireContext(), "Please provide an overall rating", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.submitReview(
            overallRating = overallRating,
            cleanlinessRating = cleanlinessRating,
            communicationRating = communicationRating,
            checkInRating = checkInRating,
            accuracyRating = accuracyRating,
            locationRating = locationRating,
            valueRating = valueRating,
            comment = comment
        )
    }
    
    private fun observeViewModel() {
        // Observe reservation
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reservation.collect { reservation ->
                reservation?.let {
                    // Display listing info
                    binding.listingTitleTextView.text = it.listingTitle
                    binding.dateRangeTextView.text = DateFormatter.formatReservationRange(
                        it.checkInDate,
                        it.checkOutDate
                    )
                    
                    // Load listing image
                    if (it.listingImageUrl.isNotBlank()) {
                        Glide.with(requireContext())
                            .load(it.listingImageUrl)
                            .centerCrop()
                            .into(binding.listingImageView)
                    }
                }
            }
        }
        
        // Observe UI state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is ReviewSubmissionUiState.Loading -> {
                        binding.loadingProgressBar.visibility = View.VISIBLE
                        binding.submitButton.isEnabled = false
                    }
                    
                    is ReviewSubmissionUiState.Ready -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.submitButton.isEnabled = true
                    }
                    
                    is ReviewSubmissionUiState.Submitting -> {
                        binding.loadingProgressBar.visibility = View.VISIBLE
                        binding.submitButton.isEnabled = false
                    }
                    
                    is ReviewSubmissionUiState.Success -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    
                    is ReviewSubmissionUiState.Error -> {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.submitButton.isEnabled = true
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
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
