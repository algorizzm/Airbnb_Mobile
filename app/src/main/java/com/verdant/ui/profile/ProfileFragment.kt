package com.verdant.ui.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.verdant.R
import com.verdant.data.model.User
import com.verdant.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProfileBinding.bind(view)

        setupClickListeners()
        observeViewModel()
    }

    /**
     * =========================
     * CLICK LISTENERS
     * =========================
     */
    private fun setupClickListeners() {

        binding.btnLogin.setOnClickListener {
            viewModel.onLoginClicked()
        }

        binding.btnSignup.setOnClickListener {
            viewModel.onSignupClicked()
        }

        binding.btnSettings.setOnClickListener {
            viewModel.onSettingsClicked()
        }

        binding.btnNotifications.setOnClickListener {

            // TODO
        }

        binding.btnUploadPhoto.setOnClickListener {

            // TODO
        }

        binding.btnEditBio.setOnClickListener {

            val currentBio =
                viewModel.user.value?.bio ?: ""

            val editText = EditText(requireContext()).apply {

                setText(currentBio)
                hint = "Tell people about yourself..."
                setPadding(50, 40, 50, 40)
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Edit Bio")
                .setView(editText)
                .setPositiveButton("Save") { _, _ ->

                    val newBio = editText.text.toString().trim()

                    viewModel.updateBio(newBio)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.tvSeeAllHikes.setOnClickListener {

            // TODO
        }
    }

    /**
     * =========================
     * OBSERVE VIEWMODEL
     * =========================
     */
    private fun observeViewModel() {

        viewModel.isGuest.observe(viewLifecycleOwner) { isGuest ->

            if (isGuest) {

                showGuestState()

            } else {

                binding.layoutGuest.visibility = View.GONE
                binding.layoutProfile.visibility = View.VISIBLE
            }
        }

        viewModel.user.observe(viewLifecycleOwner) { user ->

            user?.let {

                showAuthenticatedState(it)
            }
        }

        viewModel.navigateToAuth.observe(viewLifecycleOwner) {

            findNavController().navigate(R.id.auth_graph)
        }

        viewModel.navigateToSettings.observe(viewLifecycleOwner) {

            findNavController().navigate(R.id.settingsFragment)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->

            // Optional:
            // show/hide progress bar here
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->

            if (!message.isNullOrEmpty()) {

                Snackbar.make(
                    binding.root,
                    message,
                    Snackbar.LENGTH_SHORT
                ).show()

                viewModel.clearError()
            }
        }

        viewModel.updateStatus.observe(viewLifecycleOwner) { success ->

            if (success) {

                Snackbar.make(
                    binding.root,
                    "Bio updated",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * =========================
     * GUEST UI
     * =========================
     */
    private fun showGuestState() {

        binding.layoutGuest.visibility = View.VISIBLE
        binding.layoutProfile.visibility = View.GONE
    }

    /**
     * =========================
     * AUTHENTICATED UI
     * =========================
     */
    private fun showAuthenticatedState(user: User) {

        binding.layoutGuest.visibility = View.GONE
        binding.layoutProfile.visibility = View.VISIBLE

        bindProfileInfo(user)
        bindStats(user)
        bindProfileImage(user)
        bindRecentHikes(user)
        bindAchievements(user)
    }

    /**
     * =========================
     * PROFILE INFO
     * =========================
     */
    private fun bindProfileInfo(user: User) {

        binding.tvFullName.text = user.name

        binding.tvUsername.text =
            "@${user.name.lowercase().replace(" ", "")}"

        binding.tvBio.text =
            if (!user.bio.isNullOrBlank()) {
                user.bio
            } else {
                "No bio yet"
            }
    }

    /**
     * =========================
     * STATS
     * =========================
     */
    private fun bindStats(user: User) {

        binding.tvStatHikes.text =
            user.totalHikes.toString()

        binding.tvStatDistance.text =
            "${user.totalDistance}km"

        binding.tvStatSummits.text =
            user.totalSummits.toString()
    }

    /**
     * =========================
     * PROFILE IMAGE
     * =========================
     */
    private fun createLetterAvatar(letter: String): Drawable {

        val size = 200

        val bitmap = Bitmap.createBitmap(
            size,
            size,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        // Background circle
        val paint = Paint().apply {
            color = Color.parseColor("#02D083")
            isAntiAlias = true
        }

        canvas.drawCircle(
            size / 2f,
            size / 2f,
            size / 2f,
            paint
        )

        // Letter paint
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 90f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }

        val xPos = size / 2f
        val yPos =
            size / 2f - (textPaint.descent() + textPaint.ascent()) / 2

        canvas.drawText(letter, xPos, yPos, textPaint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun bindProfileImage(user: User) {

        // If user has uploaded image, use it
        if (user.profileImage.isNotEmpty()) {

            Glide.with(requireContext())
                .load(user.profileImage)
                .placeholder(R.drawable.bg_circle)
                .error(R.drawable.bg_circle)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.imgAvatar)

        } else {

            // Get first letter from email
            val firstLetter = user.email
                .trim()
                .firstOrNull()
                ?.uppercase() ?: "?"

            // Create text drawable
            val drawable = createLetterAvatar(firstLetter)

            binding.imgAvatar.setImageDrawable(drawable)
        }
    }

    /**
     * =========================
     * RECENT HIKES
     * =========================
     */
    private fun bindRecentHikes(user: User) {

        binding.tvRecentHike1.text = "Mt. Pulag"
        binding.tvRecentHike2.text = "Mt. Apo"
        binding.tvRecentHike3.text = "Mt. Kanlaon"
    }

    /**
     * =========================
     * ACHIEVEMENTS
     * =========================
     */
    private fun bindAchievements(user: User) {

        // TODO
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}