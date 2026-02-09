package com.safeguard.sos.data.mapper

import com.safeguard.sos.data.local.entity.EmergencyContactEntity
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.model.Relationship
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactMapper @Inject constructor() {

    fun mapEntityToDomain(entity: EmergencyContactEntity): EmergencyContact {
        return EmergencyContact(
            id = entity.id,
            userId = entity.userId,
            name = entity.name,
            phoneNumber = entity.phoneNumber,
            relationship = Relationship.fromValue(entity.relationship),
            isPrimary = entity.isPrimary,
            notifyViaSms = entity.notifyViaSms,
            notifyViaCall = entity.notifyViaCall,
            photoUri = entity.photoUri,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    fun mapDomainToEntity(contact: EmergencyContact, userId: String): EmergencyContactEntity {
        return EmergencyContactEntity(
            id = contact.id,
            userId = userId,
            name = contact.name,
            phoneNumber = contact.phoneNumber,
            relationship = contact.relationship.value,
            isPrimary = contact.isPrimary,
            notifyViaSms = contact.notifyViaSms,
            notifyViaCall = contact.notifyViaCall,
            photoUri = contact.photoUri,
            createdAt = contact.createdAt,
            updatedAt = contact.updatedAt
        )
    }
}
