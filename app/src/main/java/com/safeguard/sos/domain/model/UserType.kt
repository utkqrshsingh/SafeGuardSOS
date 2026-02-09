// app/src/main/java/com/safeguard/sos/domain/model/UserType.kt

package com.safeguard.sos.domain.model

enum class UserType(val value: String, val displayName: String) {
    REGULAR("regular", "Regular User"),
    HELPER("helper", "Helper"),
    BOTH("both", "User & Helper");

    companion object {
        fun fromValue(value: String): UserType {
            return entries.find { it.value == value } ?: REGULAR
        }
    }
}

enum class Gender(val value: String, val displayName: String) {
    MALE("male", "Male"),
    FEMALE("female", "Female"),
    OTHER("other", "Other"),
    PREFER_NOT_TO_SAY("prefer_not_to_say", "Prefer not to say");

    companion object {
        fun fromValue(value: String): Gender {
            return entries.find { it.value == value } ?: PREFER_NOT_TO_SAY
        }
    }
}

enum class VerificationStatus(val value: String, val displayName: String) {
    NOT_VERIFIED("not_verified", "Not Verified"),
    UNVERIFIED("unverified", "Unverified"),
    PENDING("pending", "Pending Verification"),
    VERIFIED("verified", "Verified"),
    FAILED("failed", "Verification Failed");

    companion object {
        fun fromValue(value: String): VerificationStatus {
            return entries.find { it.value == value } ?: NOT_VERIFIED
        }
    }
}

enum class HelperStatus(val value: String, val displayName: String) {
    INACTIVE("inactive", "Inactive"),
    ACTIVE("active", "Active"),
    AVAILABLE("available", "Available"),
    RESPONDING("responding", "Responding"),
    BUSY("busy", "Busy"),
    OFFLINE("offline", "Offline");

    companion object {
        fun fromValue(value: String): HelperStatus {
            return entries.find { it.value == value } ?: INACTIVE
        }
    }
}