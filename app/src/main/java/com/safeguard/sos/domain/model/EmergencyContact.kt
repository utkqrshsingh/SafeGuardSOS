package com.safeguard.sos.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class EmergencyContact(
    val id: String,
    val userId: String,
    val name: String,
    val phoneNumber: String,
    val relationship: Relationship,
    val isPrimary: Boolean = false,
    val notifyViaSms: Boolean = true,
    val notifyViaCall: Boolean = false,
    val photoUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) : Parcelable {

    val displayName: String
        get() = name.ifEmpty { phoneNumber }

    val initials: String
        get() = name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

    val formattedPhone: String
        get() = "+91 ${phoneNumber.take(5)} ${phoneNumber.takeLast(5)}"
}

enum class Relationship(val value: String, val displayName: String) {
    PARENT("parent", "Parent"),
    SPOUSE("spouse", "Spouse"),
    SIBLING("sibling", "Sibling"),
    CHILD("child", "Child"),
    FRIEND("friend", "Friend"),
    RELATIVE("relative", "Relative"),
    COLLEAGUE("colleague", "Colleague"),
    OTHER("other", "Other");

    companion object {
        fun fromValue(value: String): Relationship {
            return entries.find { it.value == value } ?: OTHER
        }

        fun getAll(): List<Relationship> = entries.toList()
    }
}
