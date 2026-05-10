package com.verdant.ui.notifications

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.verdant.R
import com.verdant.core.auth.AuthState
import com.verdant.data.model.AppNotification
import com.verdant.data.repository.NotificationRepository
import com.verdant.data.session.UserSessionManager
import com.verdant.databinding.FragmentNotificationInboxBinding
import kotlinx.coroutines.launch

class NotificationInboxFragment : Fragment(R.layout.fragment_notification_inbox) {

    private var _binding: FragmentNotificationInboxBinding? = null
    private val binding get() = _binding!!

    private val notifRepo = NotificationRepository()
    private var currentUid: String? = null

    private val adapter = NotificationAdapter { notification ->
        currentUid?.let { uid ->
            if (!notification.read) {
                lifecycleScope.launch { notifRepo.markRead(uid, notification.id) }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotificationInboxBinding.bind(view)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.btnMarkAllRead.setOnClickListener {
            currentUid?.let { uid ->
                lifecycleScope.launch { notifRepo.markAllRead(uid) }
            }
        }

        binding.recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotifications.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                UserSessionManager.authState.collect { state ->
                    if (state is AuthState.Authenticated && state.user != null) {
                        currentUid = state.uid
                        observeNotifications(state.uid)
                    } else {
                        currentUid = null
                        submitNotifications(emptyList())
                    }
                }
            }
        }
    }

    private fun observeNotifications(uid: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                notifRepo.observeNotifications(uid).collect { list ->
                    submitNotifications(list)
                }
            }
        }
    }

    private fun submitNotifications(list: List<AppNotification>) {
        adapter.submitList(list)
        val hasItems = list.isNotEmpty()
        binding.layoutEmpty.visibility = if (hasItems) View.GONE else View.VISIBLE
        binding.recyclerNotifications.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.tvSectionLabel.visibility = if (hasItems) View.VISIBLE else View.GONE
        binding.btnMarkAllRead.visibility = if (list.any { !it.read }) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
