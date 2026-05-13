package com.verdant.ui.explore

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.verdant.R
import com.verdant.core.auth.AuthState
import com.verdant.data.session.UserSessionManager
import com.verdant.databinding.FragmentExploreBinding
import com.verdant.ui.explore.adapter.HikeAdapter
import kotlinx.coroutines.launch

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels()

    private val adapter = HikeAdapter { hike ->

        val bundle = Bundle().apply {
            putString(ARG_HIKE_ID, hike.id)
        }

        try {
            findNavController().navigate(
                R.id.action_exploreFragment_to_hikeDetailFragment,
                bundle
            )
        } catch (e: IllegalArgumentException) {
            // ignore double click
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentExploreBinding.bind(view)

        setupTopBar()
        setupRecycler()
        setupSearch()
        setupFilters()
        observeUi()
    }

    private fun setupTopBar() {

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

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

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
        }
    }

    private fun setupRecycler() {

        binding.recyclerHikes.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerHikes.adapter = adapter
    }

    private fun setupSearch() {

        binding.etSearch.doAfterTextChanged { text ->

            viewModel.setSearchQuery(
                text?.toString().orEmpty()
            )
        }
    }

    private fun setupFilters() {

        // ── Filter panel toggle ───────────────────────────────
        binding.btnToggleFilters.setOnClickListener {

            val panel = binding.layoutFilters
            val chevron = binding.ivFilterChevron

            if (panel.visibility == View.GONE) {

                panel.visibility = View.VISIBLE
                chevron.rotation = 270f

            } else {

                panel.visibility = View.GONE
                chevron.rotation = 90f
            }
        }

        // ── Clear filters ─────────────────────────────────────
        binding.tvClearFilters.setOnClickListener {

            binding.chipGroupDifficultyFilter
                .check(R.id.chipFilterAll)

            binding.etMinDistance.text?.clear()
            binding.etMaxDistance.text?.clear()
            binding.etMaxPrice.text?.clear()
            binding.etMaxDuration.text?.clear()

            viewModel.setDifficultyFilter(null)
            viewModel.setMinDistance(null)
            viewModel.setMaxDistance(null)
            viewModel.setMaxPrice(null)
            viewModel.setMaxDuration(null)
        }

        // ── Difficulty chip filter ────────────────────────────
        binding.chipGroupDifficultyFilter
            .setOnCheckedStateChangeListener { _, checkedIds ->

                val difficulty = when (
                    checkedIds.firstOrNull()
                ) {

                    R.id.chipFilterEasy ->
                        "Easy"

                    R.id.chipFilterModerate ->
                        "Moderate"

                    R.id.chipFilterHard ->
                        "Hard"

                    R.id.chipFilterExpert ->
                        "Expert"

                    else ->
                        null
                }

                viewModel.setDifficultyFilter(
                    difficulty
                )
            }

        // ── Numeric filters ───────────────────────────────────
        binding.etMinDistance.doAfterTextChanged {

            viewModel.setMinDistance(
                it?.toString()?.toDoubleOrNull()
            )
        }

        binding.etMaxDistance.doAfterTextChanged {

            viewModel.setMaxDistance(
                it?.toString()?.toDoubleOrNull()
            )
        }

        binding.etMaxPrice.doAfterTextChanged {

            viewModel.setMaxPrice(
                it?.toString()?.toDoubleOrNull()
            )
        }

        binding.etMaxDuration.doAfterTextChanged {

            viewModel.setMaxDuration(
                it?.toString()?.toDoubleOrNull()
            )
        }
    }

    private fun observeUi() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                launch {

                    viewModel.displayHikes.collect { hikes ->

                        adapter.submitList(hikes)

                        binding.tvEmpty.visibility =
                            if (hikes.isEmpty())
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

    companion object {
        const val ARG_HIKE_ID = "hikeId"
    }
}