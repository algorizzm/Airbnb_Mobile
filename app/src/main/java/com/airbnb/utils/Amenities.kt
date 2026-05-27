package com.airbnb.utils

object Amenities {
    val DEFAULT_AMENITIES = listOf(
        "WiFi",
        "Kitchen",
        "Air Conditioning",
        "Pool",
        "Parking",
        "TV",
        "Workspace",
        "Washer",
        "Dryer",
        "Gym",
        "Breakfast",
        "Pet Friendly"
    )

    fun standardize(rawAmenities: List<String>): List<String> {
        val canonicalByKey = DEFAULT_AMENITIES.associateBy { normalizeKey(it) }
        return rawAmenities
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { canonicalByKey[normalizeKey(it)] ?: DEFAULT_AMENITIES.find { option -> option.equals(it, ignoreCase = true) } }
            .distinct()
            .toList()
    }

    private fun normalizeKey(value: String): String {
        return value
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }
}
