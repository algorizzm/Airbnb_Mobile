package com.verdant.utils

object UserRole {
    const val GUEST = "guest"
    const val HIKER = "hiker"
    const val GUIDE = "guide"
    const val ADMIN = "admin"
}

object HikeStatus {
    const val DRAFT = "draft"
    const val OPEN = "open"
    const val FULL = "full"
    const val ONGOING = "ongoing"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
    /** Legacy documents only; prefer [CANCELLED]. */
    const val CLOSED = "closed"
}

object BookingStatus {
    const val PENDING = "pending"
    const val APPROVED = "approved"
    const val REJECTED = "rejected"
    const val CANCELLED = "cancelled"
    const val COMPLETED = "completed"
}

object PaymentStatus {
    const val UNPAID = "unpaid"
    const val PENDING = "pending"
    const val PAID = "paid"
    const val REFUNDED = "refunded"
}

object Permissions {

    // ------------------------------------------------
    // ROLE CHECKS
    // ------------------------------------------------

    fun isGuest(role: String?): Boolean =
        role == UserRole.GUEST

    fun isHiker(role: String?): Boolean =
        role == UserRole.HIKER

    fun isGuide(role: String?): Boolean =
        role == UserRole.GUIDE

    fun isAdmin(role: String?): Boolean =
        role == UserRole.ADMIN


    // ------------------------------------------------
// GUEST PERMISSIONS
// ------------------------------------------------
// Guests can:
// - View public/popular feeds
// - Browse hikes
//
// Guests cannot:
// - Like posts
// - Comment
// - Share
// - Message
// - Post
// - Edit profile
// - Access personalized feeds

    fun canViewPublicFeed(role: String?): Boolean =
        isGuest(role)

    fun canViewPersonalizedFeed(role: String?): Boolean =
        !isGuest(role)


// ------------------------------------------------
// HIKER PERMISSIONS
// ------------------------------------------------
// Hikers can:
// - Like posts
// - Comment on posts
// - Share posts
// - Message users
// - Apply to hikes
// - Edit profile
// - Access personalized feeds

    fun canLikePosts(role: String?): Boolean =
        isHiker(role)

    fun canCommentPosts(role: String?): Boolean =
        isHiker(role)

    fun canSharePosts(role: String?): Boolean =
        isHiker(role)

    fun canMessage(role: String?): Boolean =
        isHiker(role)

    fun canEditProfile(role: String?): Boolean =
        isHiker(role)


// ------------------------------------------------
// GUIDE PERMISSIONS
// ------------------------------------------------
// Guides can:
// - Create hikes
// - Manage hikes
// - Manage bookings
// - Message users
// - Edit profile
// - Access personalized feeds
// - Interact with posts

    fun canCreateHike(role: String?): Boolean =
        isGuide(role)

    fun canManageBookings(role: String?): Boolean =
        isGuide(role)

    fun canGuideMessageUsers(role: String?): Boolean =
        isGuide(role)

    fun canGuideEditProfile(role: String?): Boolean =
        isGuide(role)

    fun canGuideInteractWithPosts(role: String?): Boolean =
        isGuide(role)


// ------------------------------------------------
// ADMIN PERMISSIONS
// ------------------------------------------------
// Admins can:
// - Access admin panel
// - View analytics
// - Moderate platform
// - Manage users
// - Full platform access

    fun canAccessAdminPanel(role: String?): Boolean =
        isAdmin(role)

    fun canViewAnalytics(role: String?): Boolean =
        isAdmin(role)

    fun canModeratePosts(role: String?): Boolean =
        isAdmin(role)

    fun canManageUsers(role: String?): Boolean =
        isAdmin(role)


// ------------------------------------------------
// SHARED AUTHORIZATION RULES
// ------------------------------------------------

    fun canManageHike(
        role: String?,
        guideId: String?,
        currentUid: String?
    ): Boolean {

        if (currentUid.isNullOrBlank()) return false

        if (guideId.isNullOrBlank()) return false

        return isGuide(role) && guideId == currentUid
    }

    fun canApplyAsHiker(
        role: String?,
        guideId: String?,
        currentUid: String?
    ): Boolean {

        if (currentUid.isNullOrBlank()) return false

        if (!isHiker(role)) return false

        // Prevent guides applying to their own hikes
        if (guideId == currentUid) return false

        return true
    }

    fun canReviewApplicants(
        role: String?,
        guideId: String?,
        currentUid: String?
    ): Boolean {

        return canManageHike(
            role,
            guideId,
            currentUid
        )
    }

    fun canStartHike(
        role: String?,
        guideId: String?,
        currentUid: String?
    ): Boolean {

        return canManageHike(
            role,
            guideId,
            currentUid
        )
    }

    fun canEndHike(
        role: String?,
        guideId: String?,
        currentUid: String?
    ): Boolean {

        return canManageHike(
            role,
            guideId,
            currentUid
        )
    }

    fun canCancelHike(
        role: String?,
        guideId: String?,
        currentUid: String?
    ): Boolean {

        return canManageHike(
            role,
            guideId,
            currentUid
        )
    }

    fun canLeaveHike(
        role: String?,
        bookingStatus: String?,
        hikeStatus: String?,
        bookingUserId: String?,
        currentUid: String?
    ): Boolean {

        if (currentUid.isNullOrBlank()) return false
        if (bookingUserId != currentUid) return false
        if (!isHiker(role)) return false

        return bookingStatus == BookingStatus.APPROVED && hikeStatus != HikeStatus.COMPLETED
    }

    fun canApplyToHike(
        role: String?,
        hikeStatus: String?,
        guideId: String?,
        currentUid: String?
    ): Boolean {

        if (!isHiker(role)) return false
        if (hikeStatus != HikeStatus.OPEN) return false

        return canApplyAsHiker(
            role,
            guideId,
            currentUid
        )
    }
}