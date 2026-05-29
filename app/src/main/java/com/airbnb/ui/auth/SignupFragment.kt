package com.airbnb.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.databinding.FragmentSignupBinding

/**
 * SignupFragment
 *
 * Modern Airbnb-style signup screen with:
 * - Username
 * - Email
 * - Password
 * - Confirm password
 * - Firebase authentication
 * - Firestore user creation
 * - Auto-login after signup
 */
class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {

        // Close button
        binding.btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        // Hide error when typing
        val clearError = {
            if (binding.tvError.visibility == View.VISIBLE) {
                binding.tvError.visibility = View.GONE
                authViewModel.clearError()
            }
        }

        binding.etName.addTextChangedListener { clearError() }
        binding.etEmail.addTextChangedListener { clearError() }
        binding.etPassword.addTextChangedListener { clearError() }
        binding.etConfirmPassword.addTextChangedListener { clearError() }

        // Create account button
        binding.btnHiker.setOnClickListener {

            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            authViewModel.signUpWithEmail(
                name,
                email,
                password,
                confirmPassword
            )
        }

        // Navigate to login
        binding.tvFooter.setOnClickListener {
            findNavController().navigate(
                R.id.action_signupFragment_to_loginFragment
            )
        }
    }

    private fun observeViewModel() {

        // Authentication success observer
        authViewModel.authState.observe(viewLifecycleOwner) { success ->

            if (success == true) {

                authViewModel.resetAuthState()

                Toast.makeText(
                    requireContext(),
                    "Account created successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                // Resume protected navigation if needed
                val destId = arguments?.getInt(ARG_DEST_ID) ?: 0
                val destArgs = arguments?.getBundle(ARG_DEST_ARGS)

                if (destId != 0) {

                    try {
                        findNavController().navigate(destId, destArgs)
                    } catch (e: Exception) {
                        navigateToMain()
                    }

                } else {
                    navigateToMain()
                }
            }
        }

        // Error observer
        authViewModel.error.observe(viewLifecycleOwner) { errorMsg ->

            if (!errorMsg.isNullOrEmpty()) {
                showError(errorMsg)
            }
        }

        // Loading observer
        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->

            binding.btnHiker.isEnabled = !isLoading

            binding.etName.isEnabled = !isLoading
            binding.etEmail.isEnabled = !isLoading
            binding.etPassword.isEnabled = !isLoading
            binding.etConfirmPassword.isEnabled = !isLoading

            binding.btnHiker.text =
                if (isLoading) {
                    "Creating account..."
                } else {
                    "Create account"
                }
        }
    }

    private fun showError(message: String) {

        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun navigateToMain() {

        try {

            // Navigate to main explore screen
            findNavController().navigate(R.id.exploreFragment)

        } catch (e: Exception) {

            try {

                findNavController().popBackStack(
                    R.id.main_graph,
                    false
                )

            } catch (e2: Exception) {

                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        const val ARG_DEST_ID = "destId"
        const val ARG_DEST_ARGS = "destArgs"
    }
}