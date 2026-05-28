package com.airbnb.utils.formatting

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Centralized date formatting utility for consistent date display across the app.
 * 
 * Responsibilities:
 * - Format reservation date ranges
 * - Format relative dates (e.g., "3 days ago", "in 2 weeks")
 * - Format check-in/check-out messages
 * - Format nights count
 * - Format compact and full readable dates
 * 
 * All methods are stateless and thread-safe.
 */
object DateFormatter {
    
    // Standard date formatters
    private val fullDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    /**
     * Formats a reservation date range.
     * 
     * Examples:
     * - "May 24 - May 29"
     * - "Dec 15 - Jan 3"
     * 
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return Formatted date range string
     */
    fun formatReservationRange(checkIn: Date?, checkOut: Date?): String {
        if (checkIn == null || checkOut == null) return "N/A"
        
        val checkInStr = shortDateFormat.format(checkIn)
        val checkOutStr = shortDateFormat.format(checkOut)
        
        return "$checkInStr - $checkOutStr"
    }
    
    /**
     * Formats a reservation date range from Firestore Timestamps.
     * 
     * @param checkIn Check-in timestamp
     * @param checkOut Check-out timestamp
     * @return Formatted date range string
     */
    fun formatReservationRange(checkIn: Timestamp?, checkOut: Timestamp?): String {
        return formatReservationRange(checkIn?.toDate(), checkOut?.toDate())
    }
    
    /**
     * Formats the number of nights between two dates.
     * 
     * Examples:
     * - "1 night"
     * - "5 nights"
     * - "0 nights" (invalid range)
     * 
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return Formatted nights string
     */
    fun formatNights(checkIn: Date?, checkOut: Date?): String {
        if (checkIn == null || checkOut == null) return "0 nights"
        
        val nights = calculateNights(checkIn, checkOut)
        
        return when (nights) {
            0 -> "0 nights"
            1 -> "1 night"
            else -> "$nights nights"
        }
    }
    
    /**
     * Formats the number of nights between two Firestore Timestamps.
     * 
     * @param checkIn Check-in timestamp
     * @param checkOut Check-out timestamp
     * @return Formatted nights string
     */
    fun formatNights(checkIn: Timestamp?, checkOut: Timestamp?): String {
        return formatNights(checkIn?.toDate(), checkOut?.toDate())
    }
    
    /**
     * Calculates the number of nights between two dates.
     * 
     * @param checkIn Check-in date
     * @param checkOut Check-out date
     * @return Number of nights (0 if invalid)
     */
    fun calculateNights(checkIn: Date, checkOut: Date): Int {
        val millis = checkOut.time - checkIn.time
        if (millis <= 0L) return 0
        return TimeUnit.MILLISECONDS.toDays(millis).toInt()
    }
    
    /**
     * Formats a relative date (e.g., "3 days ago", "in 2 weeks").
     * 
     * Examples:
     * - "Just now"
     * - "5m ago"
     * - "2h ago"
     * - "3d ago"
     * - "May 24" (older than 7 days)
     * 
     * @param timestamp Firestore timestamp
     * @return Formatted relative date string
     */
    fun formatRelativeDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "N/A"
        
        val millis = timestamp.toDate().time
        val now = System.currentTimeMillis()
        val diff = now - millis
        
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
            else -> shortDateFormat.format(Date(millis))
        }
    }
    
    /**
     * Formats a relative date from a Date object.
     * 
     * @param date Date object
     * @return Formatted relative date string
     */
    fun formatRelativeDate(date: Date?): String {
        if (date == null) return "N/A"
        return formatRelativeDate(Timestamp(date))
    }
    
    /**
     * Formats a full readable date.
     * 
     * Example: "May 24, 2026"
     * 
     * @param timestamp Firestore timestamp
     * @return Formatted full date string
     */
    fun formatFullDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "N/A"
        return fullDateFormat.format(timestamp.toDate())
    }
    
    /**
     * Formats a full readable date from a Date object.
     * 
     * @param date Date object
     * @return Formatted full date string
     */
    fun formatFullDate(date: Date?): String {
        if (date == null) return "N/A"
        return fullDateFormat.format(date)
    }
    
    /**
     * Formats a short date.
     * 
     * Example: "May 24"
     * 
     * @param timestamp Firestore timestamp
     * @return Formatted short date string
     */
    fun formatShortDate(timestamp: Timestamp?): String {
        if (timestamp == null) return "N/A"
        return shortDateFormat.format(timestamp.toDate())
    }
    
    /**
     * Formats a short date from a Date object.
     * 
     * @param date Date object
     * @return Formatted short date string
     */
    fun formatShortDate(date: Date?): String {
        if (date == null) return "N/A"
        return shortDateFormat.format(date)
    }
    
    /**
     * Formats a month and year.
     * 
     * Example: "May 2026"
     * 
     * @param timestamp Firestore timestamp
     * @return Formatted month and year string
     */
    fun formatMonthYear(timestamp: Long?): String {
        if (timestamp == null) return "N/A"
        return monthYearFormat.format(Date(timestamp))
    }
    
    /**
     * Formats a check-in message based on reservation dates.
     * 
     * Examples:
     * - "Check-in today"
     * - "Check-in tomorrow"
     * - "Check-in in 3 days"
     * - "Checked in"
     * 
     * @param checkInDate Check-in date
     * @param checkedIn Whether the user has checked in
     * @return Formatted check-in message
     */
    fun formatCheckInMessage(checkInDate: Date?, checkedIn: Boolean): String {
        if (checkedIn) return "Checked in"
        if (checkInDate == null) return "Check-in date unknown"
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val checkInCal = Calendar.getInstance().apply {
            time = checkInDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val daysDiff = TimeUnit.MILLISECONDS.toDays(checkInCal.time - today.time).toInt()
        
        return when {
            daysDiff < 0 -> "Check-in was ${-daysDiff} days ago"
            daysDiff == 0 -> "Check-in today"
            daysDiff == 1 -> "Check-in tomorrow"
            else -> "Check-in in $daysDiff days"
        }
    }
    
    /**
     * Formats a check-in message from Firestore Timestamp.
     * 
     * @param checkInDate Check-in timestamp
     * @param checkedIn Whether the user has checked in
     * @return Formatted check-in message
     */
    fun formatCheckInMessage(checkInDate: Timestamp?, checkedIn: Boolean): String {
        return formatCheckInMessage(checkInDate?.toDate(), checkedIn)
    }
    
    /**
     * Formats a check-out message based on reservation dates.
     * 
     * Examples:
     * - "Check-out today"
     * - "Check-out tomorrow"
     * - "Check-out in 2 days"
     * - "Checked out"
     * 
     * @param checkOutDate Check-out date
     * @param checkedOut Whether the user has checked out
     * @return Formatted check-out message
     */
    fun formatCheckOutMessage(checkOutDate: Date?, checkedOut: Boolean): String {
        if (checkedOut) return "Checked out"
        if (checkOutDate == null) return "Check-out date unknown"
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val checkOutCal = Calendar.getInstance().apply {
            time = checkOutDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val daysDiff = TimeUnit.MILLISECONDS.toDays(checkOutCal.time - today.time).toInt()
        
        return when {
            daysDiff < 0 -> "Check-out was ${-daysDiff} days ago"
            daysDiff == 0 -> "Check-out today"
            daysDiff == 1 -> "Check-out tomorrow"
            else -> "Check-out in $daysDiff days"
        }
    }
    
    /**
     * Formats a check-out message from Firestore Timestamp.
     * 
     * @param checkOutDate Check-out timestamp
     * @param checkedOut Whether the user has checked out
     * @return Formatted check-out message
     */
    fun formatCheckOutMessage(checkOutDate: Timestamp?, checkedOut: Boolean): String {
        return formatCheckOutMessage(checkOutDate?.toDate(), checkedOut)
    }
    
    /**
     * Formats a countdown message for upcoming reservations.
     * 
     * Examples:
     * - "Upcoming in 2 weeks"
     * - "Starts tomorrow"
     * - "Starts today"
     * - "Active stay"
     * - "Stayed 3 days ago"
     * 
     * @param checkInDate Check-in date
     * @param checkOutDate Check-out date
     * @param status Reservation status
     * @return Formatted countdown message
     */
    fun formatCountdownMessage(
        checkInDate: Date?,
        checkOutDate: Date?,
        status: String
    ): String {
        if (checkInDate == null || checkOutDate == null) return ""
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val checkInCal = Calendar.getInstance().apply {
            time = checkInDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val checkOutCal = Calendar.getInstance().apply {
            time = checkOutDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val daysUntilCheckIn = TimeUnit.MILLISECONDS.toDays(checkInCal.time - today.time).toInt()
        val daysSinceCheckOut = TimeUnit.MILLISECONDS.toDays(today.time - checkOutCal.time).toInt()
        
        return when (status.lowercase()) {
            "active_stay" -> "Active stay"
            "completed" -> when {
                daysSinceCheckOut == 0 -> "Completed today"
                daysSinceCheckOut == 1 -> "Stayed yesterday"
                daysSinceCheckOut < 7 -> "Stayed $daysSinceCheckOut days ago"
                daysSinceCheckOut < 30 -> "Stayed ${daysSinceCheckOut / 7} weeks ago"
                else -> "Completed"
            }
            "upcoming", "confirmed" -> when {
                daysUntilCheckIn < 0 -> "Check-in overdue"
                daysUntilCheckIn == 0 -> "Starts today"
                daysUntilCheckIn == 1 -> "Starts tomorrow"
                daysUntilCheckIn < 7 -> "Upcoming in $daysUntilCheckIn days"
                daysUntilCheckIn < 30 -> "Upcoming in ${daysUntilCheckIn / 7} weeks"
                else -> "Upcoming"
            }
            else -> ""
        }
    }
    
    /**
     * Formats a countdown message from Firestore Timestamps.
     * 
     * @param checkInDate Check-in timestamp
     * @param checkOutDate Check-out timestamp
     * @param status Reservation status
     * @return Formatted countdown message
     */
    fun formatCountdownMessage(
        checkInDate: Timestamp?,
        checkOutDate: Timestamp?,
        status: String
    ): String {
        return formatCountdownMessage(checkInDate?.toDate(), checkOutDate?.toDate(), status)
    }
}
