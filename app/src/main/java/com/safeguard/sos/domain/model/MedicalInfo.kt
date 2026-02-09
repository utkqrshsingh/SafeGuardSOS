package com.safeguard.sos.domain.model

data class MedicalInfo(
    val allergies: List<String> = emptyList(),
    val medications: List<String> = emptyList(),
    val conditions: List<String> = emptyList(),
    val emergencyNotes: String = "",
    val organDonor: Boolean = false,
    val insuranceProvider: String = "",
    val insurancePolicyNumber: String = "",
    val primaryPhysician: String = "",
    val physicianPhone: String = ""
)