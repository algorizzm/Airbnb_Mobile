package com.hikora.ui.hikes

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
import com.hikora.R
import com.hikora.databinding.FragmentHikesBinding
import com.hikora.ui.hikes.adapter.HikeAdapter
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

        binding.toolbar.setNavigationOnClickListener(null)

        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }

        val difficulties = listOf("All", "Easy", "Moderate", "Hard", "Expert")
        binding.spinnerDifficulty.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            difficulties
        )
        binding.spinnerDifficulty.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val value = difficulties[position]
                viewModel.setDifficultyFilter(if (position == 0) null else value)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.fabCreateHike.setOnClickListener {
            findNavController().navigate(R.id.createEditHikeFragment, Bundle())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.displayHikes.collect { list ->
                        adapter.submitList(list)
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
