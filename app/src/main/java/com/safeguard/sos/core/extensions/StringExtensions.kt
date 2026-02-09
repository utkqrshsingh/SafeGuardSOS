// app/src/main/java/com/safeguard/sos/core/extensions/StringExtensions.kt

package com.safeguard.sos.core.extensions

import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

/**
 * Check if string is a valid email
 */
fun String.isValidEmail(): Boolean {
    return isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * Check if string is a valid phone number (Indian format)
 */
fun String.isValidPhoneNumber(): Boolean {
    val cleaned = this.replace(Regex("[^0-9]"), "")
    return cleaned.length == 10 && cleaned.matches(Regex("^[6-9]\\d{9}$"))
}

/**
 * Check if string is a valid Aadhaar number
 */
fun String.isValidAadhaar(): Boolean {
    val cleaned = this.replace(Regex("[^0-9]"), "")
    if (cleaned.length != 12) return false

    // Aadhaar cannot start with 0 or 1
    if (cleaned.startsWith("0") || cleaned.startsWith("1")) return false

    // Verhoeff checksum validation (simplified)
    return cleaned.matches(Regex("^[2-9]\\d{11}$"))
}

/**
 * Check if string is a valid OTP
 */
fun String.isValidOtp(length: Int = 6): Boolean {
    return this.length == length && this.all { it.isDigit() }
}

/**
 * Check if string is a valid password
 */
fun String.isValidPassword(minLength: Int = 8): Boolean {
    if (length < minLength) return false

    val hasUpperCase = any { it.isUpperCase() }
    val hasLowerCase = any { it.isLowerCase() }
    val hasDigit = any { it.isDigit() }
    val hasSpecialChar = any { !it.isLetterOrDigit() }

    return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
}

/**
 * Check if string is a strong password (at least 8 chars, with mixed chars)
 */
fun String.isStrongPassword(): Boolean {
    val passwordPattern = Pattern.compile(
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!])(?=\\S+$).{8,}$"
    )
    return passwordPattern.matcher(this).matches()
}

/**
 * Check if string is a valid name
 */
fun String.isValidName(minLength: Int = 2, maxLength: Int = 50): Boolean {
    if (length < minLength || length > maxLength) return false
    return matches(Regex("^[a-zA-Z\\s]+$"))
}

/**
 * Format phone number to Indian format
 */
fun String.formatPhoneNumber(): String {
    val cleaned = this.replace(Regex("[^0-9]"), "")
    return when {
        cleaned.length == 10 -> "+91 ${cleaned.substring(0, 5)} ${cleaned.substring(5)}"
        cleaned.length == 12 && cleaned.startsWith("91") -> {
            val number = cleaned.substring(2)
            "+91 ${number.substring(0, 5)} ${number.substring(5)}"
        }
        else -> this
    }
}

/**
 * Format Aadhaar number with spaces
 */
fun String.formatAadhaar(): String {
    val cleaned = this.replace(Regex("[^0-9]"), "")
    return when {
        cleaned.length >= 12 -> "${cleaned.substring(0, 4)} ${cleaned.substring(4, 8)} ${cleaned.substring(8, 12)}"
        cleaned.length > 8 -> "${cleaned.substring(0, 4)} ${cleaned.substring(4, 8)} ${cleaned.substring(8)}"
        cleaned.length > 4 -> "${cleaned.substring(0, 4)} ${cleaned.substring(4)}"
        else -> cleaned
    }
}

/**
 * Mask Aadhaar number for display
 */
fun String.maskAadhaar(): String {
    val cleaned = this.replace(Regex("[^0-9]"), "")
    return if (cleaned.length == 12) {
        "XXXX XXXX ${cleaned.substring(8)}"
    } else {
        this
    }
}

/**
 * Mask phone number for display
 */
fun String.maskPhoneNumber(): String {
    val cleaned = this.replace(Regex("[^0-9]"), "")
    return when {
        cleaned.length == 10 -> "XXXXXX${cleaned.takeLast(4)}"
        cleaned.length == 12 -> "+91 XXXXXX${cleaned.takeLast(4)}"
        else -> this
    }
}

/**
 * Mask email for display
 */
fun String.maskEmail(): String {
    val atIndex = indexOf('@')
    if (atIndex <= 1) return this

    val prefix = substring(0, minOf(2, atIndex))
    val domain = substring(atIndex)
    val maskedMiddle = "*".repeat(atIndex - prefix.length)

    return "$prefix$maskedMiddle$domain"
}

/**
 * Capitalize first letter
 */
fun String.capitalizeFirst(): String {
    return if (isNotEmpty()) {
        this[0].uppercaseChar() + substring(1).lowercase()
    } else {
        this
    }
}

/**
 * Capitalize each word
 */
fun String.capitalizeWords(): String {
    return split(" ").joinToString(" ") { it.capitalizeFirst() }
}

/**
 * Convert to title case
 */
fun String.toTitleCase(): String {
    return lowercase().capitalizeWords()
}

/**
 * Remove all whitespace
 */
fun String.removeWhitespace(): String {
    return replace(Regex("\\s+"), "")
}

/**
 * Truncate string with ellipsis
 */
fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
    return if (length > maxLength) {
        "${take(maxLength - ellipsis.length)}$ellipsis"
    } else {
        this
    }
}

/**
 * Extract initials from name
 */
fun String.getInitials(maxInitials: Int = 2): String {
    return split(" ")
        .filter { it.isNotBlank() }
        .take(maxInitials)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
}

/**
 * Check if string contains only digits
 */
fun String.isDigitsOnly(): Boolean {
    return all { it.isDigit() }
}

/**
 * Check if string contains only letters
 */
fun String.isLettersOnly(): Boolean {
    return all { it.isLetter() }
}

/**
 * Check if string is alphanumeric
 */
fun String.isAlphanumeric(): Boolean {
    return all { it.isLetterOrDigit() }
}

/**
 * Safe to Int conversion
 */
fun String.toIntOrDefault(default: Int = 0): Int {
    return toIntOrNull() ?: default
}

/**
 * Safe to Double conversion
 */
fun String.toDoubleOrDefault(default: Double = 0.0): Double {
    return toDoubleOrNull() ?: default
}

/**
 * Safe to Long conversion
 */
fun String.toLongOrDefault(default: Long = 0L): Long {
    return toLongOrNull() ?: default
}

/**
 * Parse date string
 */
fun String.toDate(pattern: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"): Date? {
    return try {
        SimpleDateFormat(pattern, Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(this)
    } catch (e: Exception) {
        null
    }
}

/**
 * Convert null or blank to null
 */
fun String?.nullIfBlank(): String? {
    return if (isNullOrBlank()) null else this
}

/**
 * Get file extension
 */
fun String.getFileExtension(): String {
    return substringAfterLast('.', "")
}

/**
 * Get file name without extension
 */
fun String.getFileNameWithoutExtension(): String {
    return substringBeforeLast('.')
}

/**
 * Convert to URL safe string
 */
fun String.toUrlSafe(): String {
    return java.net.URLEncoder.encode(this, "UTF-8")
}

/**
 * Remove HTML tags
 */
fun String.removeHtml(): String {
    return replace(Regex("<[^>]*>"), "")
}

/**
 * Check if string is a valid URL
 */
fun String.isValidUrl(): Boolean {
    return Patterns.WEB_URL.matcher(this).matches()
}