package com.hikora.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.hikora.R
import com.hikora.databinding.FragmentHomeBinding
import com.hikora.data.model.HikePost
import com.hikora.ui.home.adapter.PostAdapter
import androidx.recyclerview.widget.LinearLayoutManager

class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)

        // ✅ Dummy data
        val dummyHikes = listOf(
            HikePost(
                username = "James Roldan",
                date = "Apr 9, 2026",
                time = "10:32 AM",
                location = "Paceo Arcenas",
                title = "Starbuks",
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

        // ✅ Setup RecyclerView
        binding.homeRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.homeRecyclerView.adapter = PostAdapter(dummyHikes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}