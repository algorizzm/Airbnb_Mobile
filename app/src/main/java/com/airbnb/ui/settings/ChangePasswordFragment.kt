package com.airbnb.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.airbnb.R
import com.airbnb.databinding.FragmentChangePasswordBinding
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView

class ChangePasswordFragment :
    Fragment(R.layout.fragment_change_password) {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentChangePasswordBinding.bind(view)

        setupClicks()
        setupPasswordStrength()

        setupPasswordToggle(
            binding.etCurrentPassword,
            binding.toggleCurrentPassword
        )

        setupPasswordToggle(
            binding.etNewPassword,
            binding.toggleNewPassword
        )

        setupPasswordToggle(
            binding.etConfirmPassword,
            binding.toggleConfirmPassword
        )
    }

    // =========================================================
    // CLICKS
    // =========================================================

    private fun setupClicks() {

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSaveChanges.setOnClickListener {
            changePassword()
        }
    }

    // =========================================================
    // CHANGE PASSWORD
    // =========================================================

    private fun changePassword() {

        val currentPassword =
            binding.etCurrentPassword.text.toString().trim()

        val newPassword =
            binding.etNewPassword.text.toString().trim()

        val confirmPassword =
            binding.etConfirmPassword.text.toString().trim()

        // Validation

        if (currentPassword.isEmpty()) {
            binding.etCurrentPassword.error =
                "Enter current password"
            return
        }

        if (newPassword.isEmpty()) {
            binding.etNewPassword.error =
                "Enter new password"
            return
        }

        if (newPassword.length < 8) {
            binding.etNewPassword.error =
                "Password must be at least 8 characters"
            return
        }

        if (newPassword != confirmPassword) {
            binding.etConfirmPassword.error =
                "Passwords do not match"
            return
        }

        val user = auth.currentUser

        if (user == null) {

            Toast.makeText(
                requireContext(),
                "User not authenticated",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val email = user.email

        if (email.isNullOrEmpty()) {

            Toast.makeText(
                requireContext(),
                "No email associated with account",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        binding.btnSaveChanges.isEnabled = false
        binding.btnSaveChanges.text = "Updating..."

        // Re-authenticate user

        val credential = EmailAuthProvider.getCredential(
            email,
            currentPassword
        )

        user.reauthenticate(credential)
            .addOnSuccessListener {

                // Update password

                user.updatePassword(newPassword)
                    .addOnSuccessListener {

                        binding.btnSaveChanges.isEnabled = true
                        binding.btnSaveChanges.text =
                            "Save Changes"

                        Toast.makeText(
                            requireContext(),
                            "Password updated successfully",
                            Toast.LENGTH_LONG
                        ).show()

                        findNavController().popBackStack()
                    }

                    .addOnFailureListener { e ->

                        binding.btnSaveChanges.isEnabled = true
                        binding.btnSaveChanges.text =
                            "Save Changes"

                        Toast.makeText(
                            requireContext(),
                            e.message ?: "Failed to update password",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }

            .addOnFailureListener {

                binding.btnSaveChanges.isEnabled = true
                binding.btnSaveChanges.text =
                    "Save Changes"

                binding.etCurrentPassword.error =
                    "Current password is incorrect"
            }
    }

    // =========================================================
    // SHOW PASSWORD
    // =========================================================

    private fun setupPasswordToggle(
        editText: EditText,
        toggle: ImageView
    ) {

        var visible = false

        toggle.setOnClickListener {

            visible = !visible

            if (visible) {

                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

                toggle.setImageResource(R.drawable.ic_eye_open)

            } else {

                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD

                toggle.setImageResource(R.drawable.ic_eye_closed)

                toggle.animate()
                    .alpha(0.6f)
                    .setDuration(120)
                    .start()
            }

            // Keep cursor at end
            editText.setSelection(
                editText.text?.length ?: 0
            )
        }
    }

    // =========================================================
    // PASSWORD STRENGTH
    // =========================================================

    private fun setupPasswordStrength() {

        binding.etNewPassword.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    val password = s.toString()

                    updatePasswordStrength(password)
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )
    }

    private fun updatePasswordStrength(password: String) {

        val hasLength = password.length >= 8
        val hasUppercase =
            password.any { it.isUpperCase() }
        val hasNumber =
            password.any { it.isDigit() }

        // Requirement indicators

        updateRequirement(
            binding.icReq1,
            binding.tvReq1,
            hasLength
        )

        updateRequirement(
            binding.icReq2,
            binding.tvReq2,
            hasUppercase
        )

        updateRequirement(
            binding.icReq3,
            binding.tvReq3,
            hasNumber
        )

        // Score

        var score = 0

        if (hasLength) score += 33
        if (hasUppercase) score += 33
        if (hasNumber) score += 34

        binding.passwordStrengthBar.progress = score

        when {

            score < 40 -> {

                binding.tvPasswordStrength.text = "Weak"

                binding.tvPasswordStrength.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_red_light
                    )
                )

                binding.passwordStrengthBar.progressTintList =
                    ContextCompat.getColorStateList(
                        requireContext(),
                        android.R.color.holo_red_light
                    )
            }

            score < 80 -> {

                binding.tvPasswordStrength.text = "Medium"

                binding.tvPasswordStrength.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_orange_light
                    )
                )

                binding.passwordStrengthBar.progressTintList =
                    ContextCompat.getColorStateList(
                        requireContext(),
                        android.R.color.holo_orange_light
                    )
            }

            else -> {

                binding.tvPasswordStrength.text = "Strong"

                binding.tvPasswordStrength.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        android.R.color.holo_green_light
                    )
                )

                binding.passwordStrengthBar.progressTintList =
                    ContextCompat.getColorStateList(
                        requireContext(),
                        android.R.color.holo_green_light
                    )
            }
        }
    }

    private fun updateRequirement(
        icon: View,
        text: android.widget.TextView,
        met: Boolean
    ) {

        icon.alpha = if (met) 1f else 0.4f

        text.setTextColor(
            if (met)
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.white
                )
            else
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.darker_gray
                )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}