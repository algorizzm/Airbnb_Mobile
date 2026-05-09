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
import com.verdant.data.session.UserSessionManager
import com.verdant.databinding.FragmentNotificationInboxBinding
import kotlinx.coroutines.launch

class NotificationInboxFragment : Fragment(R.layout.fragment_notification_inbox) {

    private var _binding: FragmentNotificationInboxBinding? = null
    private val binding get() = _binding!!

    private val adapter = NotificationAdapter { notification ->
        // TODO: navigate to the relevant screen based on notification.type / notification.refId
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentNotificationInboxBinding.bind(view)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnMarkAllRead.setOnClickListener {
            // Mark all as read — update list with read = true
            val marked = adapter.currentList.map { it.copy(read = true) }
            submitNotifications(marked)
        }

        binding.recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotifications.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                UserSessionManager.authState.collect { state ->
                    if (state is AuthState.Authenticated && state.user != null) {
                        loadNotifications(state.uid)
                    } else {
                        submitNotifications(emptyList())
                    }
                }
            }
        }
    }

    /**
     * Loads notifications for the given user.
     * Currently uses stub data — replace with a real Firestore query when the
     * notifications collection is ready.
     */
    private fun loadNotifications(uid: String) {
        // Stub: replace with repository call
        val stubs = listOf(
            AppNotification(
                id = "1",
                type = "booking_approved",
                title = "Booking Approved",
                body = "Your application for Mt. Apo Sunrise Hike has been approved.",
                timestamp = System.currentTimeMillis() - 3_600_000,
                read = false
            ),
            AppNotification(
                id = "2",
                type = "hike_update",
                title = "Hike Update",
                body = "Dalaguete Trilogy Hike has been updated. Check the new schedule.",
                timestamp = System.currentTimeMillis() - 86_400_000,
                read = true
            ),
            AppNotification(
                id = "3",
                type = "booking_rejected",
                title = "Booking Rejected",
                body = "Your application for Osmeña Peak Trail was not accepted.",
                timestamp = System.currentTimeMillis() - 172_800_000,
                read = true
            )
        )
        submitNotifications(stubs)
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
