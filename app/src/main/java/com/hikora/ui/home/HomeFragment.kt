package com.hikora.ui.home

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
import com.hikora.R
import com.hikora.databinding.FragmentHomeBinding
import com.hikora.ui.hikes.HikesFragment
import com.hikora.ui.hikes.HikesViewModel
import com.hikora.ui.hikes.adapter.HikeAdapter
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private val hikesViewModel: HikesViewModel by viewModels()

    // Reuse the adapter
    private val hikeAdapter = HikeAdapter { hike ->
        val bundle = Bundle().apply {
            putString(HikesFragment.ARG_HIKE_ID, hike.id)
        }
        findNavController().navigate(R.id.hikeDetailFragment, bundle)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)

        // =========================
        // 👤 USER (Existing logic)
        // =========================
        homeViewModel.loadUser()

        homeViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvGreeting.text = "Hello, ${user.name} 👋"
            } else {
                binding.tvGreeting.text = "Hello, Hiker 👋"
            }
        }

        // =========================
        // 🥾 HIKES (NEW logic)
        // =========================
        binding.recyclerHikes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHikes.adapter = hikeAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 🔥 Observe hikes list
                launch {
                    hikesViewModel.displayHikes.collect { list ->
                        hikeAdapter.submitList(list)
                    }
                }

                // 🔥 Toast messages
                launch {
                    hikesViewModel.toast.collect { msg ->
                        if (!msg.isNullOrBlank()) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            hikesViewModel.consumeToast()
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