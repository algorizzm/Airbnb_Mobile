package com.airbnb.utils

import java.util.Calendar
import java.util.Date

object ReservationDateValidator {

    fun isCheckInValid(checkIn: Date, now: Date = Date()): Boolean {
        return !startOfDay(checkIn).before(startOfDay(now))
    }

    fun isDateRangeValid(checkIn: Date, checkOut: Date, now: Date = Date()): Boolean {
        val normalizedCheckIn = startOfDay(checkIn)
        val normalizedCheckOut = startOfDay(checkOut)
        return !normalizedCheckIn.before(startOfDay(now)) && normalizedCheckOut.after(normalizedCheckIn)
    }

    fun calculateNights(checkIn: Date, checkOut: Date): Int {
        val millis = startOfDay(checkOut).time - startOfDay(checkIn).time
        if (millis <= 0L) return 0
        val dayMillis = 24L * 60L * 60L * 1000L
        return (millis / dayMillis).toInt()
    }

    fun startOfDay(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
}
