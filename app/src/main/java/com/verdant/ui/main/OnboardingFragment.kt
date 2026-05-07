package com.verdant.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.verdant.R

class OnboardingFragment : Fragment() {

    private var page = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        page = arguments?.getInt("page", 1) ?: 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val layoutResId = when (page) {
            1 -> R.layout.fragment_onboarding
            2 -> R.layout.fragment_onboarding_2
            3 -> R.layout.fragment_onboarding_3
            else -> R.layout.fragment_onboarding
        }

        return inflater.inflate(layoutResId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnContinue = view.findViewById<Button>(R.id.btnContinue)

        btnContinue.setOnClickListener {

            when (page) {

                1 -> {
                    val bundle = Bundle().apply {
                        putInt("page", 2)
                    }

                    findNavController().navigate(
                        R.id.onboardingFragment,
                        bundle,
                        navOptions {
                            launchSingleTop = true
                            popUpTo(R.id.onboardingFragment) {
                                inclusive = true
                            }
                        }
                    )
                }

                2 -> {
                    val bundle = Bundle().apply {
                        putInt("page", 3)
                    }

                    findNavController().navigate(
                        R.id.onboardingFragment,
                        bundle,
                        navOptions {
                            launchSingleTop = true
                            popUpTo(R.id.onboardingFragment) {
                                inclusive = true
                            }
                        }
                    )
                }

                3 -> {
                    findNavController().navigate(R.id.main_graph)
                }
            }
        }
    }
}