package com.safeguard.sos.core.utils

import org.junit.Assert.*
import org.junit.Test

class AadhaarValidatorTest {

    @Test
    fun `isValidFormat returns true for valid 12 digit number`() {
        // Valid Aadhaar number: 2345 6789 0128 (passes Verhoeff)
        assertTrue(AadhaarValidator.isValidFormat("234567890128"))
    }

    @Test
    fun `isValidFormat returns false for less than 12 digits`() {
        assertFalse(AadhaarValidator.isValidFormat("12345678901"))
    }

    @Test
    fun `isValidFormat returns false for more than 12 digits`() {
        assertFalse(AadhaarValidator.isValidFormat("1234567890123"))
    }

    @Test
    fun `isValidFormat returns false for non-numeric characters`() {
        assertFalse(AadhaarValidator.isValidFormat("12345678901a"))
    }

    @Test
    fun `isValidFormat returns false for starting with 0`() {
        assertFalse(AadhaarValidator.isValidFormat("012345678901"))
    }

    @Test
    fun `isValidFormat returns false for starting with 1`() {
        assertFalse(AadhaarValidator.isValidFormat("123456789012"))
    }

    @Test
    fun `validate returns Valid for correct Aadhaar`() {
        val result = AadhaarValidator.validate("234567890128")
        assertTrue(result is AadhaarValidator.ValidationResult.Valid)
    }

    @Test
    fun `validate returns InvalidFormat for short number`() {
        val result = AadhaarValidator.validate("1234567890")
        assertTrue(result is AadhaarValidator.ValidationResult.InvalidFormat)
    }

    @Test
    fun `formatAadhaar adds spaces correctly`() {
        val formatted = AadhaarValidator.formatAadhaar("234567890128")
        assertEquals("2345 6789 0128", formatted)
    }

    @Test
    fun `formatAadhaar handles partial input`() {
        val formatted = AadhaarValidator.formatAadhaar("23456789")
        assertEquals("2345 6789", formatted)
    }

    @Test
    fun `maskAadhaar masks correctly`() {
        val masked = AadhaarValidator.maskAadhaar("234567890128")
        assertEquals("XXXX XXXX 0128", masked)
    }
}