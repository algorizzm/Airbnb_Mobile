package com.airbnb.utils

import com.google.firebase.Timestamp
import java.util.*

/**
 * Utility for normalizing dates to avoid timezone drift and ensure consistent date comparisons.
 * 
 * All date comparisons for reservations and blocked dates should use these utilities
 * to ensure dates are compared at local midnight (00:00:00).
 */
object DateNormalizationUtil {
    
    /**
     * Normalizes a Date to local midnight (00:00:00.000).
     * This removes time-of-day information and ensures consistent date-only comparisons.
     */
    fun normalizeToMidnight(date: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }
    
    /**
     * Normalizes a Timestamp to local midnight.
     */
    fun normalizeToMidnight(timestamp: Timestamp): Timestamp {
        val normalized = normalizeToMidnight(timestamp.toDate())
        return Timestamp(normalized)
    }
    
    /**
     * Checks if two dates represent the same day (ignoring time).
     */
    fun isSameDay(date1: Date, date2: Date): Boolean {
        val normalized1 = normalizeToMidnight(date1)
        val normalized2 = normalizeToMidnight(date2)
        return normalized1 == normalized2
    }
    
    /**
     * Checks if date1 is before date2 (comparing dates only, not time).
     */
    fun isDateBefore(date1: Date, date2: Date): Boolean {
        val normalized1 = normalizeToMidnight(date1)
        val normalized2 = normalizeToMidnight(date2)
        return normalized1.before(normalized2)
    }
    
    /**
     * Checks if date1 is after date2 (comparing dates only, not time).
     */
    fun isDateAfter(date1: Date, date2: Date): Boolean {
        val normalized1 = normalizeToMidnight(date1)
        val normalized2 = normalizeToMidnight(date2)
        return normalized1.after(normalized2)
    }
    
    /**
     * Checks if date1 is before or equal to date2 (comparing dates only).
     */
    fun isDateBeforeOrEqual(date1: Date, date2: Date): Boolean {
        return !isDateAfter(date1, date2)
    }
    
    /**
     * Checks if date1 is after or equal to date2 (comparing dates only).
     */
    fun isDateAfterOrEqual(date1: Date, date2: Date): Boolean {
        return !isDateBefore(date1, date2)
    }
    
    /**
     * Checks if a date falls within a range (inclusive).
     * All dates are normalized to midnight before comparison.
     */
    fun isDateInRange(date: Date, rangeStart: Date, rangeEnd: Date): Boolean {
        val normalized = normalizeToMidnight(date)
        val normalizedStart = normalizeToMidnight(rangeStart)
        val normalizedEnd = normalizeToMidnight(rangeEnd)
        
        return !normalized.before(normalizedStart) && !normalized.after(normalizedEnd)
    }
    
    /**
     * Checks if two date ranges overlap.
     * Ranges overlap if: start1 < end2 AND start2 < end1
     */
    fun doRangesOverlap(
        start1: Date,
        end1: Date,
        start2: Date,
        end2: Date
    ): Boolean {
        val normStart1 = normalizeToMidnight(start1)
        val normEnd1 = normalizeToMidnight(end1)
        val normStart2 = normalizeToMidnight(start2)
        val normEnd2 = normalizeToMidnight(end2)
        
        return normStart1.before(normEnd2) && normStart2.before(normEnd1)
    }
    
    /**
     * Returns the number of days between two dates (inclusive).
     */
    fun daysBetween(startDate: Date, endDate: Date): Int {
        val start = normalizeToMidnight(startDate)
        val end = normalizeToMidnight(endDate)
        
        val diffInMillis = end.time - start.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt() + 1 // +1 for inclusive count
    }
}
