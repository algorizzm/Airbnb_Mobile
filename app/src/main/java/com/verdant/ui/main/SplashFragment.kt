package com.verdant.ui.main

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.verdant.R

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.postDelayed({

            val bundle = Bundle().apply {
                putInt("layoutId", R.layout.fragment_onboarding)
            }

            findNavController().navigate(
                R.id.onboardingFragment,
                bundle
            )

        }, 1000)
    }
}