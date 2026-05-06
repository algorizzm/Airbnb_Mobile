package com.hikora.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.hikora.R

class SplashFragment : Fragment(R.layout.fragment_splash) {

    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // 🔥 TEMP: force logout for testing
        FirebaseAuth.getInstance().signOut()

        view.postDelayed({

            findNavController().navigate(R.id.loginFragment)

        }, 1000)
    }
}