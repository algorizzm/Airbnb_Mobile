package com.verdant.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.verdant.R
import com.verdant.core.auth.AuthState
import com.verdant.data.model.FeedItem
import com.verdant.data.model.HikePost
import com.verdant.data.remote.WeatherState
import com.verdant.data.session.UserSessionManager
import com.verdant.databinding.FragmentHomeBinding
import com.verdant.ui.home.adapter.PostAdapter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

    // ── Location permission launcher ─────────────────────────────────────────
    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        // Whether granted or denied, attempt weather fetch (falls back to Cebu coords)
        homeViewModel.loadWeather()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        // Hide settings/notifications for guests
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                UserSessionManager.authState.collect { state ->
                    val visible = if (state is AuthState.Guest) View.GONE else View.VISIBLE
                    binding.btnNotifications.visibility = visible
                    binding.btnSettings.visibility = visible
                }
            }
        }

        requestLocationAndLoadWeather()
        observeUser()
    }

    // ── Location permission ───────────────────────────────────────────────────
    private fun requestLocationAndLoadWeather() {
        val fine   = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION

        val alreadyGranted = ContextCompat.checkSelfPermission(requireContext(), coarse) ==
                PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            homeViewModel.loadWeather()
        } else {
            locationPermLauncher.launch(arrayOf(fine, coarse))
        }
    }

    // ── User + weather observer ───────────────────────────────────────────────
    private fun observeUser() {
        homeViewModel.user.observe(viewLifecycleOwner) { user ->
            // Collect weather alongside user so adapter always has latest state
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    combine(
                        homeViewModel.weather,
                        homeViewModel.weatherLoading
                    ) { w, loading -> Pair(w, loading) }
                        .collect { (weather, loading) ->
                            setupRecyclerView(user?.role, weather, loading)
                        }
                }
            }
        }
        homeViewModel.loadUser()
    }

    // ── Feed setup ────────────────────────────────────────────────────────────
    private fun setupRecyclerView(
        role: String?,
        weather: WeatherState?,
        weatherLoading: Boolean
    ) {
        val dummyHikes = listOf(
            HikePost(
                username  = "Jedd Davis",
                date      = "December 21, 2025",
                time      = "10:32 AM",
                location  = "Dalaguete",
                title     = "DALAGUETE TRILOGY HIKE!",
                distance  = "21.5 km",
                elevation = "1150 m",
                duration  = "7h 20m",
                images    = listOf(R.drawable.sample_hike3, R.drawable.sample_hike1, R.drawable.sample_hike2)
            ),
            HikePost(
                username  = "James Roldan",
                date      = "Apr 9, 2026",
                time      = "10:32 AM",
                location  = "Paceo Arcenas",
                title     = "STARBUKS HIKE!",
                distance  = "12.5 km",
                elevation = "980 m",
                duration  = "5h 20m",
                images    = listOf(R.drawable.sample_hike, R.drawable.sample_hike, R.drawable.sample_hike)
            )
        )

        val feedItems = mutableListOf<FeedItem>()
        feedItems.add(FeedItem.CTA)
        feedItems.addAll(dummyHikes.map { FeedItem.Post(it) })

        binding.homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.homeRecyclerView.adapter = PostAdapter(
            items           = feedItems,
            userRole        = role,
            fragmentManager = childFragmentManager,
            weather         = weather,
            weatherLoading  = weatherLoading,
            onHikeClick     = {
                findNavController().navigate(R.id.action_homeFragment_to_hikesFragment)
            },
            onPostClick     = { hike ->
                val bundle = PostDetailFragment.args(
                    username = hike.username,
                    date     = hike.date,
                    time     = hike.time,
                    location = hike.location,
                    title    = hike.title,
                    stats    = "${hike.distance} • ${hike.elevation} • ${hike.duration}",
                    images   = hike.images,
                    role     = role
                )
                findNavController().navigate(R.id.postDetailFragment, bundle)
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}