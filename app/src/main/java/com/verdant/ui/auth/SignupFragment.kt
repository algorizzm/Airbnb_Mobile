package com.verdant.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.material.button.MaterialButton
import com.verdant.R
import com.verdant.core.navigation.AuthNavKeys




class SignupFragment : Fragment() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val etName = view.findViewById<EditText>(R.id.etName)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etConfirm = view.findViewById<EditText>(R.id.etConfirmPassword)

        val btnClient = view.findViewById<MaterialButton>(R.id.btnClient)
        val btnGuide = view.findViewById<MaterialButton>(R.id.btnGuide)

        val tvError = view.findViewById<TextView>(R.id.tvError)
        val tvLoginRedirect = view.findViewById<TextView>(R.id.tvFooter)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (message.isNullOrEmpty()) return@observe

            tvError.text = message
            tvError.visibility = View.VISIBLE
        }

        // 🔵 Continue as Client
        btnClient.setOnClickListener {

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()

            if (!validateInputs(name, email, password, confirm, tvError)) return@setOnClickListener

            viewModel.signup(name, email, password, "client")
        }


        // 🟢 Continue as Guide
        btnGuide.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()
            val name = etName.text.toString().trim()

            if (!validateInputs(name, email, password, confirm, tvError)) return@setOnClickListener

            viewModel.signup(name, email, password, "guide")
        }

        // 🔗 Go to login
        tvLoginRedirect.setOnClickListener {
            val forwardArgs = Bundle().apply {
                arguments?.getInt(AuthNavKeys.POST_LOGIN_DEST_ID)?.let { putInt(AuthNavKeys.POST_LOGIN_DEST_ID, it) }
                arguments?.getBundle(AuthNavKeys.POST_LOGIN_ARGS)?.let { putBundle(AuthNavKeys.POST_LOGIN_ARGS, it) }
            }
            findNavController().navigate(
                R.id.action_signupFragment_to_loginFragment,
                forwardArgs,
                navOptions { popUpTo(R.id.signupFragment) { inclusive = true } }
            )
        }

        // ✅ Success observer
        viewModel.authState.observe(viewLifecycleOwner) { success ->
            if (success != true) return@observe
            if (!isAdded) return@observe

            val destId = arguments?.getInt(AuthNavKeys.POST_LOGIN_DEST_ID)
            val destArgs = arguments?.getBundle(AuthNavKeys.POST_LOGIN_ARGS)

            if (destId != null && destId != 0) {
                findNavController().navigate(
                    destId,
                    destArgs,
                    navOptions { popUpTo(R.id.signupFragment) { inclusive = true } }
                )
            } else {
                findNavController().navigate(
                    R.id.homeFragment,
                    null,
                    navOptions { popUpTo(R.id.signupFragment) { inclusive = true } }
                )
            }

            viewModel.resetAuthState()
        }

    }
}
private fun validateInputs(
        name: String,
        email: String,
        password: String,
        confirm: String,
        tvError: TextView
): Boolean {

    if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
        tvError.text = "Please fill in all fields"
        tvError.visibility = View.VISIBLE
        return false
    }

    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
        tvError.text = "Invalid email format"
        tvError.visibility = View.VISIBLE
        return false
    }

    if (password.length < 8) {
        tvError.text = "Password must be at least 8 characters"
        tvError.visibility = View.VISIBLE
        return false
    }

    if (password != confirm) {
        tvError.text = "Passwords do not match"
        tvError.visibility = View.VISIBLE
        return false
    }

    tvError.visibility = View.GONE
    return true
}