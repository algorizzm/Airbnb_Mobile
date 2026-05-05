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

        val btnClient = view.findViewById<Button>(R.id.btnClient)
        val btnGuide = view.findViewById<Button>(R.id.btnGuide)

        val tvError = view.findViewById<TextView>(R.id.tvError)
        val tvLoginRedirect = view.findViewById<TextView>(R.id.tvFooter)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        // 🔵 Continue as Client
        btnClient.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()
            val name = etName.text.toString().trim()

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
            findNavController().navigate(
                R.id.loginFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.signupFragment, true)
                    .build()
            )
        }

        // ✅ Success observer
        viewModel.authState.observe(viewLifecycleOwner) {
            if (it) {
                Toast.makeText(requireContext(), "Signup Successful", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        // ❌ Error observer
        viewModel.error.observe(viewLifecycleOwner) {
            tvError.text = it
            tvError.visibility = View.VISIBLE
        }
    }

    // 🔍 Validation function
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
}