package com.airbnb.ui.shared.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.airbnb.R
import com.airbnb.data.model.User
import com.airbnb.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var currentUser: User? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentEditProfileBinding.bind(view)

        setupViews()
        loadUserData()
    }

    private fun setupViews() {

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Email should NOT be editable
        binding.etEmail.isEnabled = false
        binding.etEmail.isFocusable = false
        binding.etEmail.isClickable = false

        binding.btnSaveChanges.setOnClickListener {
            saveProfile()
        }

        binding.btnSignOut.setOnClickListener {

            auth.signOut()

            Toast.makeText(
                requireContext(),
                "Signed out successfully",
                Toast.LENGTH_SHORT
            ).show()

            findNavController().navigate(R.id.main_graph)
        }
    }

    private fun loadUserData() {

        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) return@addOnSuccessListener

                currentUser = document.toObject(User::class.java)

                currentUser?.let { user ->

                    binding.etUsername.setText(user.name)

                    binding.etEmail.setText(user.email)

                    binding.etLocation.setText(user.location)

                    binding.etBio.setText(user.bio ?: "")
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    requireContext(),
                    "Failed to load profile",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveProfile() {

        val uid = auth.currentUser?.uid ?: return

        val username = binding.etUsername.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()

        if (username.isBlank()) {

            binding.etUsername.error = "Username is required"
            return
        }

        binding.btnSaveChanges.isEnabled = false
        binding.btnSaveChanges.text = "Saving..."

        val updatedUser = currentUser?.copy(
            name = username,
            location = location,
            bio = bio
        ) ?: User(
            id = uid,
            name = username,
            email = auth.currentUser?.email ?: "",
            location = location,
            bio = bio
        )

        firestore.collection("users")
            .document(uid)
            .set(updatedUser)
            .addOnSuccessListener {

                binding.btnSaveChanges.isEnabled = true
                binding.btnSaveChanges.text = "Save Changes"

                Toast.makeText(
                    requireContext(),
                    "Profile updated successfully",
                    Toast.LENGTH_SHORT
                ).show()

                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->

                binding.btnSaveChanges.isEnabled = true
                binding.btnSaveChanges.text = "Save Changes"

                Toast.makeText(
                    requireContext(),
                    e.message ?: "Failed to update profile",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}