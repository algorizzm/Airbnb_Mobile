package com.airbnb.ui.auth

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
import com.airbnb.R
import com.airbnb.core.navigation.AuthNavKeys

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
        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        btnClose.setOnClickListener {

            findNavController().navigate(
                R.id.main_graph,
                null,
                navOptions {
                    popUpTo(R.id.loginFragment) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            )
        }

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
            // Preserve intended destination when user switches to signup.
            val forwardArgs = Bundle().apply {
                arguments?.getInt(AuthNavKeys.POST_LOGIN_DEST_ID)?.let { putInt(AuthNavKeys.POST_LOGIN_DEST_ID, it) }
                arguments?.getBundle(AuthNavKeys.POST_LOGIN_ARGS)?.let { putBundle(AuthNavKeys.POST_LOGIN_ARGS, it) }
            }
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment, forwardArgs)
        }

        viewModel.authState.observe(viewLifecycleOwner) {
            if (it == true && isAdded) {

                viewModel.resetAuthState()

                val destId = arguments?.getInt(AuthNavKeys.POST_LOGIN_DEST_ID)
                val destArgs = arguments?.getBundle(AuthNavKeys.POST_LOGIN_ARGS)

                if (destId != null && destId != 0) {
                    findNavController().navigate(
                        destId,
                        destArgs,
                        navOptions { popUpTo(R.id.loginFragment) { inclusive = true } }
                    )
                } else {
                    findNavController().navigate(
                        R.id.action_loginFragment_to_homeFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)
                            .build()
                    )
                }
            }
        }

        viewModel.error.observe(viewLifecycleOwner) {
            tvError.text = it
            tvError.visibility = View.VISIBLE
        }
    }
}