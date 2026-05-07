package com.verdant.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.verdant.R

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.postDelayed({

            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser != null) {
                // Already logged in
                findNavController().navigate(R.id.main_graph)
            } else {
                // Not logged in -> onboarding
                val bundle = Bundle().apply {
                    putInt("layoutId", R.layout.fragment_onboarding)
                }

                findNavController().navigate(
                    R.id.onboardingFragment,
                    bundle
                )
            }

        }, 1000)
    }
}