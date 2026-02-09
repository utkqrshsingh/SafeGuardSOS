// app/src/main/java/com/safeguard/sos/core/utils/AadhaarValidator.kt

package com.safeguard.sos.core.utils

/**
 * Utility class for Aadhaar number validation using Verhoeff algorithm
 */
object AadhaarValidator {

    // Multiplication table
    private val d = arrayOf(
        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
        intArrayOf(1, 2, 3, 4, 0, 6, 7, 8, 9, 5),
        intArrayOf(2, 3, 4, 0, 1, 7, 8, 9, 5, 6),
        intArrayOf(3, 4, 0, 1, 2, 8, 9, 5, 6, 7),
        intArrayOf(4, 0, 1, 2, 3, 9, 5, 6, 7, 8),
        intArrayOf(5, 9, 8, 7, 6, 0, 4, 3, 2, 1),
        intArrayOf(6, 5, 9, 8, 7, 1, 0, 4, 3, 2),
        intArrayOf(7, 6, 5, 9, 8, 2, 1, 0, 4, 3),
        intArrayOf(8, 7, 6, 5, 9, 3, 2, 1, 0, 4),
        intArrayOf(9, 8, 7, 6, 5, 4, 3, 2, 1, 0)
    )

    // Permutation table
    private val p = arrayOf(
        intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
        intArrayOf(1, 5, 7, 6, 2, 8, 3, 0, 9, 4),
        intArrayOf(5, 8, 0, 3, 7, 9, 6, 1, 4, 2),
        intArrayOf(8, 9, 1, 6, 0, 4, 3, 5, 2, 7),
        intArrayOf(9, 4, 5, 3, 1, 2, 6, 8, 7, 0),
        intArrayOf(4, 2, 8, 6, 5, 7, 3, 9, 0, 1),
        intArrayOf(2, 7, 9, 3, 8, 0, 6, 4, 1, 5),
        intArrayOf(7, 0, 4, 6, 9, 1, 3, 2, 5, 8)
    )

    // Inverse table
    private val inv = intArrayOf(0, 4, 3, 2, 1, 5, 6, 7, 8, 9)

    /**
     * Validates Aadhaar number using Verhoeff algorithm
     */
    fun isValid(aadhaar: String): Boolean {
        // Remove any spaces or dashes
        val cleanAadhaar = aadhaar.replace(Regex("[\\s-]"), "")

        // Check length
        if (cleanAadhaar.length != 12) {
            return false
        }

        // Check if all characters are digits
        if (!cleanAadhaar.all { it.isDigit() }) {
            return false
        }

        // Aadhaar cannot start with 0 or 1
        if (cleanAadhaar.startsWith("0") || cleanAadhaar.startsWith("1")) {
            return false
        }

        // Apply Verhoeff algorithm
        return validateVerhoeff(cleanAadhaar)
    }

    /**
     * Alias for isValid to support existing code
     */
    fun isValidFormat(aadhaar: String): Boolean = isValid(aadhaar)

    /**
     * Validates using Verhoeff algorithm
     */
    fun isValidVerhoeff(number: String): Boolean {
        val cleaned = number.replace(Regex("[\\s-]"), "")
        if (cleaned.length != 12 || !cleaned.all { it.isDigit() }) return false
        
        var c = 0
        val reversed = cleaned.reversed()

        for (i in reversed.indices) {
            c = d[c][p[(i % 8)][Character.getNumericValue(reversed[i])]]
        }

        return c == 0
    }

    private fun validateVerhoeff(number: String): Boolean {
        var c = 0
        val reversed = number.reversed()

        for (i in reversed.indices) {
            c = d[c][p[(i % 8)][Character.getNumericValue(reversed[i])]]
        }

        return c == 0
    }

    /**
     * Generates check digit using Verhoeff algorithm
     */
    fun generateCheckDigit(number: String): Int {
        var c = 0
        val reversed = number.reversed()

        for (i in reversed.indices) {
            c = d[c][p[((i + 1) % 8)][Character.getNumericValue(reversed[i])]]
        }

        return inv[c]
    }

    /**
     * Formats Aadhaar number with spaces
     */
    fun format(aadhaar: String): String {
        val cleaned = aadhaar.replace(Regex("[^0-9]"), "")
        return when {
            cleaned.length >= 12 -> "${cleaned.substring(0, 4)} ${cleaned.substring(4, 8)} ${cleaned.substring(8, 12)}"
            cleaned.length > 8 -> "${cleaned.substring(0, 4)} ${cleaned.substring(4, 8)} ${cleaned.substring(8)}"
            cleaned.length > 4 -> "${cleaned.substring(0, 4)} ${cleaned.substring(4)}"
            else -> cleaned
        }
    }

    /**
     * Alias for format to support existing code
     */
    fun formatAadhaar(aadhaar: String): String = format(aadhaar)

    /**
     * Masks Aadhaar number for display (shows only last 4 digits)
     */
    fun mask(aadhaar: String): String {
        val cleaned = aadhaar.replace(Regex("[^0-9]"), "")
        return if (cleaned.length == 12) {
            "XXXX XXXX ${cleaned.substring(8)}"
        } else {
            aadhaar
        }
    }

    /**
     * Alias for mask to support existing code
     */
    fun maskAadhaar(aadhaar: String): String = mask(aadhaar)

    /**
     * Returns validation result with message
     */
    fun validate(aadhaar: String): ValidationResult {
        val cleaned = aadhaar.replace(Regex("[\\s-]"), "")

        return when {
            cleaned.isEmpty() -> ValidationResult.InvalidFormat("Aadhaar number is required")
            cleaned.length != 12 -> ValidationResult.InvalidFormat("Aadhaar number must be 12 digits")
            !cleaned.all { it.isDigit() } -> ValidationResult.InvalidFormat("Aadhaar number must contain only digits")
            cleaned.startsWith("0") || cleaned.startsWith("1") ->
                ValidationResult.InvalidFormat("Invalid Aadhaar number")
            !validateVerhoeff(cleaned) -> ValidationResult.InvalidChecksum
            else -> ValidationResult.Valid
        }
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class InvalidFormat(val message: String) : ValidationResult()
        object InvalidChecksum : ValidationResult()
    }
}