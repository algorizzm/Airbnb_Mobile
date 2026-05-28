package com.airbnb.core.mode

/**
 * Represents the current navigation mode of the application.
 *
 * TRAVELER — default mode; shows Explore, Wishlists, Trips, Messages, Profile tabs.
 * HOST     — hosting mode; shows Today, Calendar, Listings, Messages, Profile tabs.
 */
enum class AppMode {
    TRAVELER,
    HOST
}
