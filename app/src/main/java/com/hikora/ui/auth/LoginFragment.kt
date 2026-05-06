package com.hikora.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.hikora.R
import com.hikora.ui.auth.AuthViewModel

class LoginFragment : Fragment() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnContinue = view.findViewById<Button>(R.id.btnContinue)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val tvSignupRedirect = view.findViewById<TextView>(R.id.tvFooter)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        btnContinue.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // 🔴 Validation
            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Please fill in all fields"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tvError.text = "Invalid email format"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (password.length < 8) {
                tvError.text = "Password must be at least 8 characters"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE

            viewModel.login(email, password)
        }

        tvSignupRedirect.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }

        viewModel.authState.observe(viewLifecycleOwner) {
            if (it == true && isAdded) {

                viewModel.resetAuthState()

                findNavController().navigate(
                    R.id.action_signupFragment_to_loginFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.signupFragment, true)
                        .build()
                )
            }
        }

        viewModel.error.observe(viewLifecycleOwner) {
            tvError.text = it
            tvError.visibility = View.VISIBLE
        }
    }
}