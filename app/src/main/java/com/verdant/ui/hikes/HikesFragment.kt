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
import androidx.recyclerview.widget.LinearLayoutManager
import com.verdant.R
import com.verdant.core.auth.AuthState
import com.verdant.core.navigation.ProtectedNav
import com.verdant.data.session.UserSessionManager
import com.verdant.databinding.FragmentHikesBinding
import com.verdant.ui.explore.ExploreFragment
import com.verdant.ui.explore.adapter.HikeAdapter
import kotlinx.coroutines.launch

class HikesFragment : Fragment(R.layout.fragment_hikes) {

    private var _binding: FragmentHikesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HikesViewModel by viewModels()

    private val adapter = HikeAdapter { hike ->

        val bundle = Bundle().apply {
            putString(ExploreFragment.ARG_HIKE_ID, hike.id)
        }

        findNavController().navigate(
            R.id.action_hikeFragment_to_hikeDetailFragment,
            bundle
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHikesBinding.bind(view)

        setupRecycler()
        setupClicks()
        observeUi()
    }

    private fun setupRecycler() {

        binding.recyclerHikes.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerHikes.adapter = adapter
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

        binding.fabCreateHike.setOnClickListener {

            ProtectedNav.navigate(
                navController = findNavController(),
                destId = R.id.action_hikeFragment_to_createEditHikeFragment,
                args = Bundle(),
                isProtected = true,
                fragmentManager = childFragmentManager
            )

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

                    viewModel.myHikes.collect { hikes ->

                        adapter.submitList(hikes)

                        binding.tvEmpty.visibility =
                            if (hikes.isEmpty())
                                View.VISIBLE
                            else
                                View.GONE
                    }
                }

                launch {

                    viewModel.isGuide.collect { guide ->

                        binding.fabCreateHike.visibility =
                            if (guide)
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