package com.airbnb.ui.hikes.events

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
import com.airbnb.R
import com.airbnb.core.navigation.ProtectedNav
import com.airbnb.databinding.FragmentEventsBinding
import com.airbnb.ui.explore.ExploreFragment
import com.airbnb.ui.explore.adapter.HikeAdapter
import kotlinx.coroutines.launch

class EventsFragment : Fragment(R.layout.fragment_events) {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventsFragmentViewModel by viewModels()

    private val adapter = HikeAdapter { hike ->

        val bundle = Bundle().apply {
            putString(ExploreFragment.ARG_HIKE_ID, hike.id)
        }

        try {
            findNavController().navigate(
                R.id.action_eventsFragment_to_hikeDetailFragment,
                bundle
            )
        } catch (e: IllegalArgumentException) {
            // ignore
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentEventsBinding.bind(view)

        setupRecycler()
        setupClicks()
        observeUi()
    }

    private fun setupRecycler() {

        binding.recyclerEvents.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerEvents.adapter = adapter
    }

    private fun setupClicks() {

        // Track tab
        binding.tabTrack.setOnClickListener {

            findNavController().navigate(
                R.id.hikeFragment
            )
        }

        // History tab
        binding.tabHistory.setOnClickListener {

            findNavController().navigate(
                R.id.hikeHistoryFragment
            )
        }

        // Notifications
        binding.btnNotifications.setOnClickListener {

            findNavController().navigate(
                R.id.notificationsFragment
            )
        }

        // Settings
        binding.btnSettings.setOnClickListener {

            findNavController().navigate(
                R.id.settingsFragment
            )
        }

        // FAB create event
        binding.fabCreateEvent.setOnClickListener {

            ProtectedNav.navigate(
                navController = findNavController(),
                destId = R.id.createEditHikeFragment,
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

                    viewModel.events.collect { hikes ->

                        adapter.submitList(hikes)

                        val isEmpty = hikes.isEmpty()

                        binding.layoutEmpty.visibility =
                            if (isEmpty)
                                View.VISIBLE
                            else
                                View.GONE

                        binding.recyclerEvents.visibility =
                            if (isEmpty)
                                View.GONE
                            else
                                View.VISIBLE
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