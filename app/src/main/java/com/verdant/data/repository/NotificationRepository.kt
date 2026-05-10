package com.verdant.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.verdant.data.model.AppNotification
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class NotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun col(uid: String) =
        db.collection("users").document(uid).collection("notifications")

    /** Real-time stream — newest first. */
    fun observeNotifications(uid: String): Flow<List<AppNotification>> = callbackFlow {
        val reg = col(uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                }.orEmpty()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun markAllRead(uid: String): Result<Unit> = runCatching {
        val snap = col(uid).whereEqualTo("read", false).get().await()
        val batch = db.batch()
        snap.documents.forEach { batch.update(it.reference, "read", true) }
        batch.commit().await()
    }

    suspend fun markRead(uid: String, notificationId: String): Result<Unit> = runCatching {
        col(uid).document(notificationId).update("read", true).await()
    }

    /** Utility — write a notification for a user (called server-side or from booking logic). */
    suspend fun postNotification(uid: String, notification: AppNotification): Result<Unit> =
        runCatching {
            val ref = col(uid).document()
            ref.set(
                mapOf(
                    "type" to notification.type,
                    "title" to notification.title,
                    "body" to notification.body,
                    "timestamp" to notification.timestamp,
                    "read" to false,
                    "refId" to notification.refId
                )
            ).await()
        }

    fun unreadCount(uid: String): Flow<Int> = callbackFlow {
        val reg = col(uid)
            .whereEqualTo("read", false)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.size() ?: 0)
            }
        awaitClose { reg.remove() }
    }
}
