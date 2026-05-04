package com.hikora.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.hikora.R
import com.hikora.data.model.User
import com.hikora.databinding.FragmentProfileBinding
import com.hikora.ui.profile.ProfileViewModel
import android.widget.TextView


class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        viewModel.loadUser()
        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                updateUI(user)
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Reference the sign-out menu and its button
        val logoutButton = binding.layoutSignOutMenu.tvMenuTitle
        logoutButton.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(
                R.id.loginFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.profileFragment, true)
                    .build()
            )
        }

        // Reference the edit profile menu and its button
        val editProfileButton = binding.layoutEditProfileMenu.tvMenuTitle
        editProfileButton.setOnClickListener {
            findNavController().navigate(
                R.id.editProfileFragment,
                null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.profileFragment, true)
                    .build()
            )
        }

        binding.layoutMyBookingsMenu.root.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_myBookingsFragment)
        }
    }

    private fun updateUI(user: User) {
        // Update avatar
        binding.avatarContainer.findViewById<TextView>(R.id.avatarText).text = user.name.take(2).uppercase()

        // Update name and email
        binding.tvName.text = user.name
        binding.tvEmail.text = "${user.email} | ${user.role.capitalize()}"

        // Update stats row
        val stats = listOf(
            "Hikes" to user.totalHikes.toString(),
            "Distance" to "${user.totalDistance} km"
        )
        binding.statsRow.removeAllViews()
        stats.forEach { (label, value) ->
            val statView = layoutInflater.inflate(R.layout.item_profile_stat, binding.statsRow, false)
            statView.findViewById<TextView>(R.id.tvStatValue).text = value
            statView.findViewById<TextView>(R.id.tvStatLabel).text = label
            binding.statsRow.addView(statView)
        }

        // Update tags row
        binding.tagsRow.removeAllViews()
        user.badges.forEach { badge ->
            val badgeView = TextView(requireContext(), null, 0, R.style.HikoraTag).apply {
                text = badge
            }
            binding.tagsRow.addView(badgeView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}