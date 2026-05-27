package com.airbnb.ui.main

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.airbnb.R

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Fix: Use the ID "splashLogo" as defined in your XML
        val logo = view.findViewById<ImageView>(R.id.splashLogo)

        // Ensure R.anim.fade_in exists in your res/anim folder
        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        logo.startAnimation(anim)

        // 2. Navigation logic with delay
        view.postDelayed({
            // 3. Safety Check: Ensure fragment is still attached before navigating
            if (isAdded) {
                val currentUser = FirebaseAuth.getInstance().currentUser

                try {
                    if (currentUser != null) {
                        // User is logged in -> Go to Main App
                        findNavController().navigate(R.id.main_graph)
                    } else {
                        // User is NOT logged in -> Go to Onboarding
                        val bundle = Bundle().apply {
                            putInt("layoutId", R.layout.fragment_onboarding)
                        }
                        findNavController().navigate(R.id.onboardingFragment, bundle)
                    }
                } catch (e: Exception) {
                    // Prevents crashes if navigation happens during a state change
                    e.printStackTrace()
                }
            }
        }, 1800)
    }
}