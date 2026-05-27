package com.airbnb.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object PublicCodeGenerator {
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    private fun generateRandomSuffix(): String {
        return Random.nextInt(1000, 10000).toString()
    }

    private fun getFormattedDate(): String {
        return dateFormat.format(Date())
    }

    fun generateListingCode(): String {
        return "LISTING-${getFormattedDate()}-${generateRandomSuffix()}"
    }

    fun generateReservationCode(): String {
        return "RES-${getFormattedDate()}-${generateRandomSuffix()}"
    }

    fun generateWishlistCode(): String {
        return "WISH-${getFormattedDate()}-${generateRandomSuffix()}"
    }

    fun generateUserCode(): String {
        return "USER-${getFormattedDate()}-${generateRandomSuffix()}"
    }
}
