package com.airbnb.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.airbnb.R
import com.airbnb.core.auth.AuthState
import com.airbnb.data.session.UserSessionManager
import com.airbnb.databinding.FragmentAccountInformationBinding

class AccountInformationFragment :
    Fragment(R.layout.fragment_account_information) {

    private var _binding: FragmentAccountInformationBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAccountInformationBinding.bind(view)

        setupViews()
        populateUserInfo()
    }

    private fun setupViews() {

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(
                R.id.action_accountInformationFragment_to_editProfileFragment
            )
        }

        binding.btnDeleteAccount.setOnClickListener {

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->

                    FirebaseAuth.getInstance().currentUser
                        ?.delete()
                        ?.addOnSuccessListener {

                            Toast.makeText(
                                requireContext(),
                                "Account deleted",
                                Toast.LENGTH_SHORT
                            ).show()

                            findNavController().navigate(R.id.auth_graph)
                        }
                        ?.addOnFailureListener { e ->

                            Toast.makeText(
                                requireContext(),
                                e.message ?: "Failed to delete account",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .show()
        }
    }

    private fun populateUserInfo() {

        val state = UserSessionManager.authState.value

        if (state is AuthState.Authenticated) {

            val user = state.user
            val firebaseUser = FirebaseAuth.getInstance().currentUser

            binding.tvFullName.text =
                user?.name?.ifBlank { "—" } ?: "—"

            binding.tvEmail.text =
                firebaseUser?.email ?: "—"

            binding.tvRole.text =
                user?.role
                    ?.replaceFirstChar { it.uppercase() }
                    ?: "—"

            binding.tvMemberSince.text =
                firebaseUser?.metadata?.creationTimestamp?.let {

                    java.text.SimpleDateFormat(
                        "MMMM yyyy",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date(it))

                } ?: "—"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}