package com.hikora.utils

object UserRole {
    const val CLIENT = "client"
    const val GUIDE = "guide"
    const val ADMIN = "admin"
}

object HikeStatus {
    const val OPEN = "open"
    const val CLOSED = "closed"
}

object BookingStatus {
    const val PENDING = "pending"
    const val APPROVED = "approved"
    const val REJECTED = "rejected"
    const val CANCELLED = "cancelled"
}

object HikingRbac {

    fun isGuide(role: String): Boolean = role == UserRole.GUIDE

    fun isClient(role: String): Boolean = role == UserRole.CLIENT

    fun canManageHike(role: String, guideId: String?, currentUid: String?): Boolean {
        if (currentUid.isNullOrBlank() || guideId.isNullOrBlank()) return false
        return isGuide(role) && guideId == currentUid
    }

    fun canApplyAsClient(role: String, guideId: String?, currentUid: String?): Boolean {
        if (currentUid.isNullOrBlank()) return false
        if (!isClient(role)) return false
        if (guideId == currentUid) return false
        return true
    }

    fun canReviewApplicants(role: String, guideId: String?, currentUid: String?): Boolean =
        canManageHike(role, guideId, currentUid)
}
