package com.verdant.ui.profile

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.verdant.R
import com.verdant.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        observeAuthState()
        setupGuestActions()


        setupTopBar()

    }

    private fun observeAuthState() {
        viewModel.isGuest.observe(viewLifecycleOwner) { isGuest ->
            if (isGuest) showGuestUi() else showProfileUi()
        }
    }

    private fun showGuestUi() {
        binding.layoutGuest.visibility = View.VISIBLE
        binding.layoutProfile.visibility = View.GONE
    }

    private fun showProfileUi() {
        binding.layoutGuest.visibility = View.GONE
        binding.layoutProfile.visibility = View.VISIBLE
    }

    private fun setupGuestActions() {
        binding.btnLogin.setOnClickListener {
            findNavController().navigate(R.id.auth_graph)
        }
        binding.btnSignup.setOnClickListener {
            findNavController().navigate(R.id.auth_graph)
        }
    }


    private fun setupTopBar() {
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
