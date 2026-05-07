package com.verdant.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.verdant.R
import com.verdant.data.model.FeedItem
import com.verdant.data.model.HikePost
import com.verdant.databinding.FragmentHomeBinding
import com.verdant.ui.home.adapter.PostAdapter
import androidx.navigation.fragment.findNavController

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)

        observeUser()
    }

    private fun observeUser() {

        homeViewModel.user.observe(viewLifecycleOwner) { user ->

            setupRecyclerView(user?.role)
        }

        homeViewModel.loadUser()
    }

    private fun setupRecyclerView(role: String?) {

        // =================================================
        // DUMMY POSTS
        // =================================================

        val dummyHikes = listOf(

            HikePost(
                username = "James Roldan",
                date = "Apr 9, 2026",
                time = "10:32 AM",
                location = "Paceo Arcenas",
                title = "STARBUKS HIKE!",
                distance = "12.5 km",
                elevation = "980 m",
                duration = "5h 20m",
                images = listOf(
                    R.drawable.sample_hike,
                    R.drawable.sample_hike,
                    R.drawable.sample_hike
                )
            ),

            HikePost(
                username = "James Roldan",
                date = "Apr 9, 2026",
                time = "10:32 AM",
                location = "Paceo Arcenas",
                title = "STARBUKS HIKE!",
                distance = "12.5 km",
                elevation = "980 m",
                duration = "5h 20m",
                images = listOf(
                    R.drawable.sample_hike,
                    R.drawable.sample_hike,
                    R.drawable.sample_hike
                )
            )
        )

        // =================================================
        // FEED ITEMS
        // =================================================

        val feedItems = mutableListOf<FeedItem>()

        // First Item = CTA
        feedItems.add(FeedItem.CTA)

        // Add Posts
        feedItems.addAll(
            dummyHikes.map {
                FeedItem.Post(it)
            }
        )

        // =================================================
        // RECYCLER VIEW
        // =================================================

        binding.homeRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        binding.homeRecyclerView.adapter =
            PostAdapter(
                items = feedItems,
                userRole = role,

                onHikeClick = {

                    findNavController().navigate(
                        R.id.action_homeFragment_to_hikesFragment
                    )
                }
            )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}