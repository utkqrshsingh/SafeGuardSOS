// app/src/main/java/com/safeguard/sos/navigation/NavRoutes.kt

package com.safeguard.sos.navigation

object NavRoutes {

    // Auth Flow
    const val AUTH_GRAPH = "auth_graph"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val USER_TYPE_SELECTION = "user_type_selection"
    const val AADHAAR_VERIFICATION = "aadhaar_verification"
    const val OTP_VERIFICATION = "otp_verification/{phoneNumber}/{verificationType}"
    const val FORGOT_PASSWORD = "forgot_password"

    // Onboarding
    const val ONBOARDING_GRAPH = "onboarding_graph"
    const val ONBOARDING = "onboarding"

    // Main Flow
    const val MAIN_GRAPH = "main_graph"
    const val HOME = "home"
    const val SOS = "sos"
    const val SOS_ACTIVE = "sos_active/{sosId}"
    const val SOS_HISTORY = "sos_history"
    const val SOS_DETAIL = "sos_detail/{sosId}"
    const val MAP = "map"
    const val CONTACTS = "contacts"
    const val ADD_CONTACT = "add_contact"
    const val EDIT_CONTACT = "edit_contact/{contactId}"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val MEDICAL_INFO = "medical_info"
    const val SETTINGS = "settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val ABOUT = "about"

    // Helper Flow
    const val HELPER_GRAPH = "helper_graph"
    const val HELPER_DASHBOARD = "helper_dashboard"
    const val HELPER_REGISTRATION = "helper_registration"
    const val NEARBY_ALERTS = "nearby_alerts"
    const val ALERT_DETAIL = "alert_detail/{sosId}"
    const val NAVIGATION_TO_VICTIM = "navigation/{sosId}"
    const val HELPER_HISTORY = "helper_history"

    // Helper functions for navigation with arguments
    fun otpVerification(phoneNumber: String, verificationType: String): String {
        return "otp_verification/$phoneNumber/$verificationType"
    }

    fun sosActive(sosId: String): String {
        return "sos_active/$sosId"
    }

    fun sosDetail(sosId: String): String {
        return "sos_detail/$sosId"
    }

    fun editContact(contactId: String): String {
        return "edit_contact/$contactId"
    }

    fun alertDetail(sosId: String): String {
        return "alert_detail/$sosId"
    }

    fun navigationToVictim(sosId: String): String {
        return "navigation/$sosId"
    }
}