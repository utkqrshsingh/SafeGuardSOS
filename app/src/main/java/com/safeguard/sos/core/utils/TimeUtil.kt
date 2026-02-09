// app/src/main/java/com/safeguard/sos/core/utils/TimeUtil.kt

package com.safeguard.sos.core.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Utility class for time and date operations
 */
object TimeUtil {

    private const val API_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    private const val DISPLAY_DATE_FORMAT = "dd MMM yyyy"
    private const val DISPLAY_TIME_FORMAT = "hh:mm a"
    private const val DISPLAY_DATE_TIME_FORMAT = "dd MMM yyyy, hh:mm a"
    private const val DISPLAY_SHORT_DATE_FORMAT = "dd MMM"
    private const val DISPLAY_FULL_DATE_FORMAT = "EEEE, dd MMMM yyyy"

    /**
     * Get current timestamp in milliseconds
     */
    fun getCurrentTimestamp(): Long = System.currentTimeMillis()

    /**
     * Get current date
     */
    fun getCurrentDate(): Date = Date()

    /**
     * Format date to API format
     */
    fun formatForApi(date: Date): String {
        val sdf = SimpleDateFormat(API_DATE_FORMAT, Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    /**
     * Parse API date string
     */
    fun parseApiDate(dateString: String): Date? {
        return try {
            val sdf = SimpleDateFormat(API_DATE_FORMAT, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Format date for display
     */
    fun formatDate(date: Date): String {
        return SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale.getDefault()).format(date)
    }

    /**
     * Format time for display
     */
    fun formatTime(date: Date): String {
        return SimpleDateFormat(DISPLAY_TIME_FORMAT, Locale.getDefault()).format(date)
    }

    /**
     * Format date and time for display
     */
    fun formatDateTime(date: Date): String {
        return SimpleDateFormat(DISPLAY_DATE_TIME_FORMAT, Locale.getDefault()).format(date)
    }

    /**
     * Format timestamp to display date
     */
    fun formatTimestamp(timestamp: Long): String {
        return formatDate(Date(timestamp))
    }

    /**
     * Format timestamp to display date and time
     */
    fun formatTimestampDateTime(timestamp: Long): String {
        return formatDateTime(Date(timestamp))
    }

    /**
     * Get relative time span (e.g., "5 minutes ago")
     */
    fun getRelativeTimeSpan(date: Date): String {
        return DateUtils.getRelativeTimeSpanString(
            date.time,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    /**
     * Get relative time span from timestamp
     */
    fun getRelativeTimeSpan(timestamp: Long): String {
        return getRelativeTimeSpan(Date(timestamp))
    }

    /**
     * Get human readable time difference
     */
    fun getTimeAgo(date: Date): String {
        val now = System.currentTimeMillis()
        val diff = now - date.time

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours ${if (hours == 1L) "hour" else "hours"} ago"
            }
            diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days days ago"
            }
            else -> formatDate(date)
        }
    }

    /**
     * Get time ago from timestamp
     */
    fun getTimeAgo(timestamp: Long): String {
        return getTimeAgo(Date(timestamp))
    }

    /**
     * Check if date is today
     */
    fun isToday(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        calendar.time = date
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Check if date is yesterday
     */
    fun isYesterday(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        calendar.time = date
        return calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Get greeting based on time of day
     */
    fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    /**
     * Format duration in milliseconds to readable string
     */
    fun formatDuration(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60

        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }

    /**
     * Format countdown seconds
     */
    fun formatCountdown(seconds: Int): String {
        return when {
            seconds <= 0 -> "0"
            seconds < 60 -> seconds.toString()
            else -> {
                val mins = seconds / 60
                val secs = seconds % 60
                String.format("%d:%02d", mins, secs)
            }
        }
    }

    /**
     * Get start of day for a date
     */
    fun getStartOfDay(date: Date = Date()): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    /**
     * Get end of day for a date
     */
    fun getEndOfDay(date: Date = Date()): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    /**
     * Add days to a date
     */
    fun addDays(date: Date, days: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.time
    }

    /**
     * Add minutes to a date
     */
    fun addMinutes(date: Date, minutes: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.MINUTE, minutes)
        return calendar.time
    }

    /**
     * Get difference in minutes between two dates
     */
    fun getMinutesDifference(startDate: Date, endDate: Date): Long {
        val diffMs = endDate.time - startDate.time
        return TimeUnit.MILLISECONDS.toMinutes(diffMs)
    }

    /**
     * Check if a date is within a time range
     */
    fun isWithinRange(date: Date, startDate: Date, endDate: Date): Boolean {
        return !date.before(startDate) && !date.after(endDate)
    }
}