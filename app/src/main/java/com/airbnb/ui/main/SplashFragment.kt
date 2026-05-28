package com.airbnb.ui.main

import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.R
import com.airbnb.core.mode.AppMode
import com.airbnb.core.mode.AppModeManager

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logo = view.findViewById<ImageView>(R.id.splashLogo)

        val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        logo.startAnimation(anim)

        view.postDelayed({
            if (isAdded) {
                try {

                    val destination =
                        if (AppModeManager.currentModeSnapshot() == AppMode.HOST) {
                            R.id.hostTodayFragment
                        } else {
                            R.id.exploreFragment
                        }

                    findNavController().navigate(destination)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }, 1800)
    }
}