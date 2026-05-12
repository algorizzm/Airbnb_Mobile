package com.verdant.ui.hikes

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.verdant.R
import com.verdant.core.auth.AuthState
import com.verdant.data.session.UserSessionManager
import com.verdant.databinding.FragmentHikesBinding
import kotlinx.coroutines.launch

class HikesFragment : Fragment(R.layout.fragment_hikes) {

    private var _binding: FragmentHikesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HikesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHikesBinding.bind(view)

        setupClicks()
        observeUi()
    }

    private fun setupClicks() {

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(
                R.id.notificationsFragment
            )
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(
                R.id.settingsFragment
            )
        }

        // Events tab
        binding.tabEvents.setOnClickListener {
            findNavController().navigate(
                R.id.eventsFragment
            )
        }

        // History tab
        binding.tabHistory.setOnClickListener {
            findNavController().navigate(
                R.id.hikeHistoryFragment
            )
        }

        // Empty state button
        binding.btnEmptyAction.setOnClickListener {

            val isGuide = viewModel.isGuide.value

            if (isGuide) {

                findNavController().navigate(
                    R.id.eventsFragment
                )

            } else {

                findNavController().navigate(
                    R.id.exploreFragment
                )
            }
        }
    }

    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {

                    UserSessionManager.authState.collect { state ->

                        val visible =
                            if (state is AuthState.Guest)
                                View.GONE
                            else
                                View.VISIBLE

                        binding.btnNotifications.visibility = visible
                        binding.btnSettings.visibility = visible
                    }
                }

                launch {

                    viewModel.isGuide.collect { guide ->

                        binding.tabEvents.visibility =
                            if (guide)
                                View.VISIBLE
                            else
                                View.GONE

                        binding.btnEmptyAction.text =
                            if (guide)
                                "Go to events"
                            else
                                "Explore hikes"

                        binding.tvEmptySub.text =
                            if (guide)
                                "Create and manage hikes from Events."
                            else
                                "Explore available hikes and join one."
                    }
                }

                launch {

                    viewModel.hasActiveHike.collect { hasHike ->

                        binding.layoutEmpty.visibility =
                            if (hasHike)
                                View.GONE
                            else
                                View.VISIBLE

                        binding.layoutActive.visibility =
                            if (hasHike)
                                View.VISIBLE
                            else
                                View.GONE
                    }
                }

                launch {

                    viewModel.toast.collect { msg ->

                        if (!msg.isNullOrBlank()) {

                            Toast.makeText(
                                requireContext(),
                                msg,
                                Toast.LENGTH_SHORT
                            ).show()

                            viewModel.consumeToast()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}