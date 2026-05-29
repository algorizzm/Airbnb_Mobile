package com.airbnb.ui.auth

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * LoginFragment - Manual Email/Password Authentication
 * 
 * Sprint 10: Implements modern email/password login flow with:
 * - Email and password validation
 * - Google Sign-In integration
 * - Error handling and user feedback
 * - Navigation to signup
 * - Protected flow resumption
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    authViewModel.loginWithGoogle(idToken)
                } else {
                    showError("Google Sign-In failed: ID Token is null")
                }
            } catch (e: ApiException) {
                showError("Google Sign-In failed: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Google Sign-In
        val webClientId = "531642391047-f1q2li806aujucvucj21u9f07gq7jo47.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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

        // Clear error on text change
        binding.etEmail.addTextChangedListener {
            if (binding.tvError.visibility == View.VISIBLE) {
                binding.tvError.visibility = View.GONE
                authViewModel.clearError()
            }
        }

        binding.etPassword.addTextChangedListener {
            if (binding.tvError.visibility == View.VISIBLE) {
                binding.tvError.visibility = View.GONE
                authViewModel.clearError()
            }
        }

        // Continue button - Email/Password login
        binding.btnContinue.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            authViewModel.loginWithEmail(email, password)
        }

        // Google Sign-In button
        binding.btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // Apple button (placeholder)
        binding.btnApple.setOnClickListener {
            showError("Apple Sign-In coming soon")
        }

        // Navigate to signup
        binding.tvFooter.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    private fun observeViewModel() {
        // Auth state observer
        authViewModel.authState.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                authViewModel.resetAuthState()
                
                // Get destination from arguments (if coming from GuestPromptDialog)
                val destId = arguments?.getInt(ARG_DEST_ID) ?: 0
                val destArgs = arguments?.getBundle(ARG_DEST_ARGS)

                if (destId != 0) {
                    // Navigate to protected destination
                    try {
                        findNavController().navigate(destId, destArgs)
                    } catch (e: Exception) {
                        // If navigation fails, go to main
                        navigateToMain()
                    }
                } else {
                    // No specific destination, go to main
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
            binding.btnContinue.isEnabled = !isLoading
            binding.btnGoogle.isEnabled = !isLoading
            binding.btnApple.isEnabled = !isLoading
            binding.etEmail.isEnabled = !isLoading
            binding.etPassword.isEnabled = !isLoading

            binding.btnContinue.text = if (isLoading) "Signing in..." else "Continue"
        }
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMain() {
        try {
            // Navigate to explore fragment (main screen)
            findNavController().navigate(R.id.exploreFragment)
        } catch (e: Exception) {
            try {
                // Fallback: try to pop back to main graph
                findNavController().popBackStack(R.id.main_graph, false)
            } catch (e2: Exception) {
                // Last resort: just pop back
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
