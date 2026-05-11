package com.verdant.ui.explore

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import com.verdant.databinding.FragmentExploreBinding
import com.verdant.ui.explore.adapter.HikeAdapter
import kotlinx.coroutines.launch
import com.verdant.core.auth.AuthState
import com.verdant.data.session.UserSessionManager

class ExploreFragment : Fragment(R.layout.fragment_explore) {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels()

    private val adapter = HikeAdapter { hike ->

        val bundle = Bundle().apply {
            putString(ARG_HIKE_ID, hike.id)
        }

        findNavController().navigate(
            R.id.hikeDetailFragment,
            bundle
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

        binding.tvClearFilters.setOnClickListener {

            binding.spinnerDifficulty.setSelection(0)

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

        val difficulties = listOf(
            "All",
            "Easy",
            "Moderate",
            "Hard",
            "Expert"
        )

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            difficulties
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        binding.spinnerDifficulty.adapter = spinnerAdapter

        binding.spinnerDifficulty.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    viewModel.setDifficultyFilter(
                        if (position == 0)
                            null
                        else
                            difficulties[position]
                    )
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) = Unit
            }

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