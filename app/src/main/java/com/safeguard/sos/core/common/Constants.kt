package com.safeguard.sos.core.common

import com.safeguard.sos.BuildConfig

object Constants {
    // API
    const val BASE_URL = BuildConfig.BASE_URL
    const val API_TIMEOUT = 30L // seconds
    const val API_CONNECT_TIMEOUT = 15L // seconds

    // Database
    const val DATABASE_NAME = "safeguard_database"
    const val DATABASE_VERSION = 1

    // Notification Channels
    const val NOTIFICATION_CHANNEL_SOS = "sos_channel"
    const val NOTIFICATION_CHANNEL_HELPER = "helper_channel"
    const val NOTIFICATION_CHANNEL_LOCATION = "location_channel"
    const val NOTIFICATION_CHANNEL_GENERAL = "general_channel"
    const val NOTIFICATION_CHANNEL_UPDATES = "updates_channel"

    // Notification IDs
    const val NOTIFICATION_ID_SOS = 1001
    const val NOTIFICATION_ID_HELPER_ALERT = 1002
    const val NOTIFICATION_ID_LOCATION_SERVICE = 1003
    const val NOTIFICATION_ID_RECORDING = 1004

    // Request Codes
    const val REQUEST_CODE_LOCATION_PERMISSION = 100
    const val REQUEST_CODE_SMS_PERMISSION = 101
    const val REQUEST_CODE_PHONE_PERMISSION = 102
    const val REQUEST_CODE_CAMERA_PERMISSION = 103
    const val REQUEST_CODE_MICROPHONE_PERMISSION = 104
    const val REQUEST_CODE_NOTIFICATION_PERMISSION = 105
    const val REQUEST_CODE_SOS_PENDING_INTENT = 200
    const val REQUEST_CODE_CANCEL_SOS_PENDING_INTENT = 201

    // Location
    const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
    const val LOCATION_FASTEST_INTERVAL = 2000L // 2 seconds
    const val LOCATION_SMALLEST_DISPLACEMENT = 10f // 10 meters
    const val DEFAULT_HELPER_RADIUS_KM = 10.0
    const val MIN_HELPER_RADIUS_KM = 5.0
    const val MAX_HELPER_RADIUS_KM = 20.0

    // SOS
    const val DEFAULT_SOS_COUNTDOWN = 5 // seconds
    const val MIN_SOS_COUNTDOWN = 3
    const val MAX_SOS_COUNTDOWN = 10
    const val SOS_AUTO_CANCEL_TIMEOUT = 30 * 60 * 1000L // 30 minutes
    const val MAX_EMERGENCY_CONTACTS = 5

    // Shake Detection
    const val SHAKE_THRESHOLD = 12.0f
    const val SHAKE_TIME_LAPSE = 500L
    const val SHAKE_COUNT_THRESHOLD = 3

    // Power Button Detection
    const val POWER_BUTTON_PRESS_COUNT = 5
    const val POWER_BUTTON_PRESS_INTERVAL = 2000L

    // Audio Recording
    const val MAX_AUDIO_DURATION = 5 * 60 * 1000L // 5 minutes
    const val AUDIO_SAMPLE_RATE = 44100
    const val AUDIO_BIT_RATE = 128000

    // Aadhaar
    const val AADHAAR_LENGTH = 12
    const val OTP_LENGTH = 6
    const val OTP_RESEND_DELAY = 60 // seconds
    const val OTP_EXPIRY = 10 * 60 * 1000L // 10 minutes

    // Pagination
    const val DEFAULT_PAGE_SIZE = 20
    const val INITIAL_LOAD_SIZE = 40

    // Cache
    const val CACHE_SIZE = 10L * 1024 * 1024 // 10 MB
    const val CACHE_MAX_AGE = 5 * 60 // 5 minutes
    const val CACHE_MAX_STALE = 7 * 24 * 60 * 60 // 7 days

    // Firestore Collections
    object Firestore {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_SOS_ALERTS = "sos_alerts"
        const val COLLECTION_HELPERS = "helpers"
        const val COLLECTION_SOS_RESPONSES = "sos_responses"
        const val COLLECTION_HELPER_RESPONSES = "helper_responses"
        const val COLLECTION_EMERGENCY_CONTACTS = "emergency_contacts"
    }

    // SharedPreferences (Legacy support)
    const val PREFS_NAME = "safeguard_prefs"

    // Deep Links
    const val DEEP_LINK_SCHEME = "safeguard"
    const val DEEP_LINK_HOST = "sos"

    // Intent Actions
    const val ACTION_TRIGGER_SOS = "com.safeguard.sos.ACTION_TRIGGER_SOS"
    const val ACTION_CANCEL_SOS = "com.safeguard.sos.ACTION_CANCEL_SOS"
    const val ACTION_RESPOND_SOS = "com.safeguard.sos.ACTION_RESPOND_SOS"
    const val ACTION_VIEW_SOS = "com.safeguard.sos.ACTION_VIEW_SOS"
    const val ACTION_STOP_LOCATION_SERVICE = "com.safeguard.sos.ACTION_STOP_LOCATION_SERVICE"
    const val ACTION_START_LOCATION_SERVICE = "com.safeguard.sos.ACTION_START_LOCATION_SERVICE"

    // Intent Extras
    const val EXTRA_SOS_ID = "extra_sos_id"
    const val EXTRA_EMERGENCY_TYPE = "extra_emergency_type"
    const val EXTRA_LATITUDE = "extra_latitude"
    const val EXTRA_LONGITUDE = "extra_longitude"

    // Emergency Types
    val EMERGENCY_TYPES = listOf(
        "General Emergency",
        "Medical Emergency",
        "Fire",
        "Accident",
        "Crime/Assault",
        "Natural Disaster",
        "Other"
    )

    // Blood Groups
    val BLOOD_GROUPS = listOf(
        "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    )

    // Relationships
    val RELATIONSHIPS = listOf(
        "Parent", "Spouse", "Sibling", "Child", "Friend",
        "Colleague", "Neighbor", "Doctor", "Other"
    )

    // Genders
    val GENDERS = listOf(
        "Male", "Female", "Other", "Prefer not to say"
    )

    // Indian States
    val INDIAN_STATES = listOf(
        "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar",
        "Chhattisgarh", "Goa", "Gujarat", "Haryana", "Himachal Pradesh",
        "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra",
        "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
        "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
        "Uttar Pradesh", "Uttarakhand", "West Bengal",
        "Andaman and Nicobar Islands", "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu",
        "Delhi", "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
    )

    object ErrorCodes {
        const val NETWORK_ERROR = 1001
        const val TIMEOUT_ERROR = 1002
        const val UNKNOWN_ERROR = 1003
        const val AUTH_ERROR = 1004
        const val NOT_FOUND = 1005
        const val PERMISSION_DENIED = 1006
    }

    object AnimationDurations {
        const val SHORT = 200L
        const val MEDIUM = 400L
        const val LONG = 600L
        const val PULSE = 1000L
    }
}
