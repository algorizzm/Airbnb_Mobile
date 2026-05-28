package com.airbnb.ui.auth

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

class GuestPromptDialog : BottomSheetDialogFragment() {

    private var _binding: DialogGuestPromptBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    private var navigated = false
    private lateinit var googleSignInClient: GoogleSignInClient

    var onAuthSuccess: (() -> Unit)? = null
    var onAuthFailure: ((Exception) -> Unit)? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    performGoogleAuth(idToken)
                } else {
                    showError("Google Sign-In failed: ID Token is null")
                }
            } catch (e: ApiException) {
                showError("Google Sign-In failed: ${e.message}")
            }
        } else {
            showError("Google Sign-In cancelled")
        }
    }

    override fun getTheme(): Int = R.style.RoundedBottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webClientId =
            "531642391047-f1q2li806aujucvucj21u9f07gq7jo47.apps.googleusercontent.com"
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
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
                val desiredHeight = (screenHeight * 0.90f).toInt()

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

        // Continue button - Email Link Flow
        binding.btnGuestSignUp.setOnClickListener {
            val input = binding.etPhoneEmail.text.toString().trim()
            if (input.isEmpty()) {
                showError("Please enter your email")
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                showError("Please enter a valid email address")
                return@setOnClickListener
            }
            sendEmailLink(input)
        }

        // Google Sign-In button
        binding.btnGuestLogIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // Apple button - fallback to legacy auth
        binding.btnApple.setOnClickListener {
            showError("Apple Sign-In coming soon")
        }

        // Use Another Account - clears Google credential cache and reopens chooser
        binding.tvUseAnotherAccount.setOnClickListener {
            clearGoogleCredentialsAndRelaunch()
        }

        binding.btnGuestDismiss.setOnClickListener {
            dismiss()
        }

        // Auth state observer
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

        // Error observer
        authViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            if (!errorMsg.isNullOrEmpty()) {
                showError(errorMsg)
                onAuthFailure?.invoke(Exception(errorMsg))
            }
        }

        // Email link sent observer
        authViewModel.emailLinkSent.observe(viewLifecycleOwner) { sent ->
            if (sent == true) {
                authViewModel.resetEmailLinkSent()
                showSuccess("Check your email for your secure sign-in link")
                dismiss()
            }
        }

        // Loading observer
        authViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnGuestSignUp.isEnabled = !isLoading
            binding.btnGuestLogIn.isEnabled = !isLoading
            binding.btnApple.isEnabled = !isLoading
            binding.etPhoneEmail.isEnabled = !isLoading
        }
    }

    private fun sendEmailLink(email: String) {
        authViewModel.sendSignInLinkToEmail(email, requireContext())
    }

    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun performGoogleAuth(idToken: String) {
        Toast.makeText(requireContext(), "Signing in with Google...", Toast.LENGTH_SHORT).show()
        authViewModel.loginWithGoogle(idToken)
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    /**
     * Clears Google Sign-In cached credential state and immediately reopens the account chooser.
     * This allows users to select a different Google account or create a new Firebase user.
     */
    private fun clearGoogleCredentialsAndRelaunch() {
        googleSignInClient.signOut().addOnCompleteListener {
            // After clearing cached credentials, immediately relaunch Google account chooser
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "GuestPromptDialog"
        const val ARG_DEST_ID = "destId"
        const val ARG_DEST_ARGS = "destArgs"

        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            destId: Int = 0,
            destArgs: Bundle? = null,
            onSuccess: (() -> Unit)? = null,
            onFailure: ((Exception) -> Unit)? = null
        ): GuestPromptDialog? {
            if (fragmentManager.isStateSaved) return null

            val existing = fragmentManager.findFragmentByTag(TAG) as? GuestPromptDialog
            if (existing != null) {
                existing.onAuthSuccess = onSuccess
                existing.onAuthFailure = onFailure
                return existing
            }

            val dialog = GuestPromptDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DEST_ID, destId)
                    if (destArgs != null) {
                        putBundle(ARG_DEST_ARGS, destArgs)
                    }
                }
                this.onAuthSuccess = onSuccess
                this.onAuthFailure = onFailure
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }

        // Retain standard show(fragmentManager) signature for backward compatibility
        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            show(fragmentManager, 0, null, null, null)
        }
    }
}