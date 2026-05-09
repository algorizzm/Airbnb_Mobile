package com.verdant.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.verdant.R
import com.verdant.data.session.UserSessionManager
import com.verdant.core.auth.AuthState
import com.verdant.databinding.FragmentAccountInformationBinding

class AccountInformationFragment : Fragment(R.layout.fragment_account_information) {

    private var _binding: FragmentAccountInformationBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAccountInformationBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }


        // Populate from session
        val state = UserSessionManager.authState.value
        if (state is AuthState.Authenticated) {
            val user = state.user
            binding.tvFullName.text = user?.name?.ifBlank { "—" } ?: "—"
            binding.tvEmail.text = FirebaseAuth.getInstance().currentUser?.email ?: "—"
            binding.tvRole.text = user?.role?.replaceFirstChar { it.uppercase() } ?: "—"
            binding.tvMemberSince.text = FirebaseAuth.getInstance().currentUser
                ?.metadata?.creationTimestamp
                ?.let {
                    java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(it))
                } ?: "—"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
