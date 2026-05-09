package com.verdant.data.model

/**
 * Represents an in-app notification shown in the notification inbox.
 *
 * @param id        Unique identifier (Firestore doc id or local UUID)
 * @param type      Category: "booking_approved", "booking_rejected", "booking_cancelled",
 *                  "hike_update", "message", "system"
 * @param title     Short headline shown in bold
 * @param body      Supporting detail text
 * @param timestamp Unix millis — used for sorting and relative time display
 * @param read      Whether the user has seen this notification
 * @param refId     Optional reference id (e.g. hikeId or bookingId) for deep-linking
 */
data class AppNotification(
    val id: String = "",
    val type: String = "system",
    val title: String = "",
    val body: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false,
    val refId: String = ""
)
