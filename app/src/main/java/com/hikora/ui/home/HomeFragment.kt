package com.hikora.ui.home

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

    private val homeViewModel: HomeViewModel by activityViewModels()
    private val hikesViewModel: HikesViewModel by activityViewModels()

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
        // 👤 USER GREETING
        // =========================
        homeViewModel.loadUser()

        homeViewModel.user.observe(viewLifecycleOwner) { user ->
            binding.tvGreeting.text =
                if (user != null) "Hello, ${user.name} 👋"
                else "Hello, Hiker 👋"
        }

        // =========================
        // 🥾 RECYCLER SETUP
        // =========================
        binding.recyclerHikes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHikes.adapter = hikeAdapter

        // =========================
        // 🔍 SEARCH (YOUR EXISTING EDITTEXT)
        // =========================
        binding.etSearch.doAfterTextChanged { text ->
            hikesViewModel.setSearchQuery(text?.toString().orEmpty())
        }

        // Optional UX improvement: treat search as instant filter
        // (already good with doAfterTextChanged)

        // =========================
        // 🎚️ DIFFICULTY FILTER (OPTIONAL BUT RECOMMENDED)
        // NOTE: only works if you ADD spinner in XML later
        // =========================
        val difficulties = listOf("All", "Easy", "Moderate", "Hard", "Expert")

        // Only run if spinner exists in XML
        binding.spinnerDifficulty.let { spinner ->
            spinner.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                difficulties
            )

            spinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        v: View?,
                        position: Int,
                        id: Long
                    ) {
                        hikesViewModel.setDifficultyFilter(
                            if (position == 0) null else difficulties[position]
                        )
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                }
        }

        // =========================
        // 🔄 OBSERVERS
        // =========================
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Hikes list
                launch {
                    hikesViewModel.displayHikes.collect { list ->
                        hikeAdapter.submitList(list)
                    }
                }

                // Toast messages
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

        // =========================
        // 🚀 QUICK ACTIONS (from XML)
        // =========================
        binding.btnBook.setOnClickListener {
            // Replace with your navigation
            Toast.makeText(requireContext(), "Book Hike clicked", Toast.LENGTH_SHORT).show()
        }

        binding.btnGuides.setOnClickListener {
            // Replace with your navigation
            Toast.makeText(requireContext(), "Find Guide clicked", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}