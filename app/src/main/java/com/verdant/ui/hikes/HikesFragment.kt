package com.verdant.ui.hikes

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
import com.verdant.core.auth.AuthState
import com.verdant.core.navigation.ProtectedNav
import com.verdant.data.session.UserSessionManager
import com.verdant.databinding.FragmentHikesBinding
import com.verdant.ui.hikes.adapter.HikeAdapter
import kotlinx.coroutines.launch

class HikesFragment : Fragment(R.layout.fragment_hikes) {

    private var _binding: FragmentHikesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HikesViewModel by viewModels()

    private val adapter = HikeAdapter { hike ->
        val bundle = Bundle().apply { putString(ARG_HIKE_ID, hike.id) }
        findNavController().navigate(R.id.hikeDetailFragment, bundle)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHikesBinding.bind(view)

        binding.recyclerHikes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHikes.adapter = adapter

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        // ── Filter panel toggle ──────────────────────────────────────────────
        binding.btnToggleFilters.setOnClickListener {
            val panel = binding.layoutFilters
            val chevron = binding.ivFilterChevron
            if (panel.visibility == View.GONE) {
                panel.visibility = View.VISIBLE
                chevron.rotation = 270f   // point up
            } else {
                panel.visibility = View.GONE
                chevron.rotation = 90f    // point down
            }
        }

        // ── Clear filters ────────────────────────────────────────────────────
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

        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }

        val difficulties = listOf("All", "Easy", "Moderate", "Hard", "Expert")
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            difficulties
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        binding.spinnerDifficulty.adapter = spinnerAdapter
        binding.spinnerDifficulty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                viewModel.setDifficultyFilter(if (position == 0) null else difficulties[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.etMinDistance.doAfterTextChanged {
            viewModel.setMinDistance(it?.toString()?.toDoubleOrNull())
        }
        binding.etMaxDistance.doAfterTextChanged {
            viewModel.setMaxDistance(it?.toString()?.toDoubleOrNull())
        }
        binding.etMaxPrice.doAfterTextChanged {
            viewModel.setMaxPrice(it?.toString()?.toDoubleOrNull())
        }
        binding.etMaxDuration.doAfterTextChanged {
            viewModel.setMaxDuration(it?.toString()?.toDoubleOrNull())
        }

        binding.fabCreateHike.setOnClickListener {
            ProtectedNav.navigate(
                navController = findNavController(),
                destId = R.id.createEditHikeFragment,
                args = Bundle(),
                isProtected = true,
                fragmentManager = childFragmentManager
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    UserSessionManager.authState.collect { state ->
                        val visible = if (state is AuthState.Guest) View.GONE else View.VISIBLE
                        binding.btnNotifications.visibility = visible
                        binding.btnSettings.visibility = visible
                    }
                }
                launch {
                    viewModel.displayHikes.collect { list ->
                        adapter.submitList(list)
                        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.isGuide.collect { guide ->
                        binding.fabCreateHike.visibility = if (guide) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.toast.collect { msg ->
                        if (!msg.isNullOrBlank()) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
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
