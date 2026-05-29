package com.airbnb.ui.auth

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.databinding.DialogGuestPromptBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * GuestPromptDialog
 *
 * Centralized authentication entry point.
 *
 * Handles:
 * - Google Sign-In
 * - Email link authentication
 * - Navigation to SignupFragment for email/password auth
 * - Protected flow resumption
 *
 * LoginFragment has been removed.
 */
class GuestPromptDialog : BottomSheetDialogFragment() {

    private var _binding: DialogGuestPromptBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    var onAuthSuccess: (() -> Unit)? = null
    var onAuthFailure: ((Exception) -> Unit)? = null

    /**
     * Google Sign-In launcher
     */
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode != Activity.RESULT_OK) {
            showError("Google Sign-In cancelled")
            return@registerForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        try {

            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken.isNullOrEmpty()) {
                showError("Google Sign-In failed: Missing ID token")
                return@registerForActivityResult
            }

            performGoogleAuth(idToken)

        } catch (e: ApiException) {

            showError(
                "Google Sign-In failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    override fun getTheme(): Int = R.style.RoundedBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeGoogleSignIn()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {

            val bottomSheet = dialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.let { sheet ->

                val behavior = BottomSheetBehavior.from(sheet)

                val screenHeight = resources.displayMetrics.heightPixels
                val desiredHeight = (screenHeight * 1f).toInt()

                sheet.layoutParams = sheet.layoutParams.apply {
                    height = desiredHeight
                }

                sheet.minimumHeight = desiredHeight

                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isDraggable = true
                behavior.peekHeight = desiredHeight
            }
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DialogGuestPromptBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    /**
     * Setup UI listeners
     */
    private fun setupUI() {

        /**
         * Continue with Google
         */
        binding.btnGuestLogIn.setOnClickListener {

            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        binding.btnContinue.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty()) {
                showError("Please enter your email")
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                showError("Please enter your password")
                return@setOnClickListener
            }

            authViewModel.loginWithEmail(email, password)
        }

        /**
         * Apple Sign-In
         */
        binding.btnApple.setOnClickListener {
            showError("Apple Sign-In coming soon")
        }

        /**
         * Use another Google account
         */
        binding.tvUseAnotherAccount.setOnClickListener {
            clearGoogleCredentialsAndRelaunch()
        }

        /**
         * Create Account
         */
        binding.btnSignup.setOnClickListener {
            navigateToSignup()
        }

        /**
         * Dismiss dialog
         */
        binding.btnGuestDismiss.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Observe ViewModel state
     */
    private fun observeViewModel() {

        /**
         * Authentication success
         */
        authViewModel.authState.observe(viewLifecycleOwner) { success ->

            if (success == true) {

                authViewModel.resetAuthState()

                val destId = arguments?.getInt(ARG_DEST_ID) ?: 0
                val destArgs = arguments?.getBundle(ARG_DEST_ARGS)

                dismiss()

                if (destId != 0) {

                    try {
                        findNavController().navigate(destId, destArgs)
                    } catch (_: Exception) {
                    }
                }

                onAuthSuccess?.invoke()
            }
        }

        /**
         * Authentication error
         */
        authViewModel.error.observe(viewLifecycleOwner) { errorMsg ->

            if (!errorMsg.isNullOrEmpty()) {

                showError(errorMsg)

                onAuthFailure?.invoke(
                    Exception(errorMsg)
                )
            }
        }

        /**
         * Email link sent
         */
        authViewModel.emailLinkSent.observe(viewLifecycleOwner) { sent ->

            if (sent == true) {

                authViewModel.resetEmailLinkSent()

                showSuccess(
                    "Check your email for your secure sign-in link"
                )

                dismiss()
            }
        }

        /**
         * Loading state
         */
        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->

            binding.btnContinue.isEnabled = !isLoading
            binding.btnSignup.isEnabled = !isLoading
            binding.btnGuestLogIn.isEnabled = !isLoading
            binding.btnApple.isEnabled = !isLoading
            binding.btnGuestDismiss.isEnabled = !isLoading
            binding.etEmail.isEnabled = !isLoading
            binding.etPassword.isEnabled = !isLoading
        }
    }

    /**
     * Initialize Google Sign-In
     */
    private fun initializeGoogleSignIn() {

        val webClientId =
            "531642391047-f1q2li806aujucvucj21u9f07gq7jo47.apps.googleusercontent.com"

        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient =
            GoogleSignIn.getClient(requireActivity(), gso)
    }


    /**
     * Perform Firebase Google authentication
     */
    private fun performGoogleAuth(idToken: String) {

        Toast.makeText(
            requireContext(),
            "Signing in with Google...",
            Toast.LENGTH_SHORT
        ).show()

        authViewModel.loginWithGoogle(idToken)
    }

    /**
     * Clear cached Google credentials
     * and reopen account chooser
     */
    private fun clearGoogleCredentialsAndRelaunch() {

        googleSignInClient.signOut()
            .addOnCompleteListener {

                val signInIntent =
                    googleSignInClient.signInIntent

                googleSignInLauncher.launch(signInIntent)
            }
    }

    /**
     * Navigate to SignupFragment
     *
     * SignupFragment now handles:
     * - Login
     * - Registration
     */
    private fun navigateToSignup() {

        val destId = arguments?.getInt(ARG_DEST_ID) ?: 0
        val destArgs = arguments?.getBundle(ARG_DEST_ARGS)

        val bundle = Bundle().apply {

            putInt(
                SignupFragment.ARG_DEST_ID,
                destId
            )

            if (destArgs != null) {

                putBundle(
                    SignupFragment.ARG_DEST_ARGS,
                    destArgs
                )
            }
        }

        try {

            findNavController().navigate(
                R.id.signupFragment,
                bundle
            )

            dismiss()

        } catch (e: Exception) {

            showError(
                "Navigation failed: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Show success toast
     */
    private fun showSuccess(message: String) {

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Show error toast
     */
    private fun showError(message: String) {

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        const val TAG = "GuestPromptDialog"

        const val ARG_DEST_ID = "destId"
        const val ARG_DEST_ARGS = "destArgs"

        /**
         * Show dialog with protected destination support
         */
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            destId: Int = 0,
            destArgs: Bundle? = null,
            onSuccess: (() -> Unit)? = null,
            onFailure: ((Exception) -> Unit)? = null
        ): GuestPromptDialog? {

            if (fragmentManager.isStateSaved) {
                return null
            }

            val existing =
                fragmentManager.findFragmentByTag(TAG)
                        as? GuestPromptDialog

            if (existing != null) {

                existing.onAuthSuccess = onSuccess
                existing.onAuthFailure = onFailure

                return existing
            }

            val dialog = GuestPromptDialog().apply {

                arguments = Bundle().apply {

                    putInt(ARG_DEST_ID, destId)

                    if (destArgs != null) {
                        putBundle(
                            ARG_DEST_ARGS,
                            destArgs
                        )
                    }
                }

                this.onAuthSuccess = onSuccess
                this.onAuthFailure = onFailure
            }

            dialog.show(fragmentManager, TAG)

            return dialog
        }

        /**
         * Backward compatibility
         */
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager
        ) {
            show(
                fragmentManager,
                0,
                null,
                null,
                null
            )
        }
    }
}