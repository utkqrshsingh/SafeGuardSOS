// app/src/main/java/com/safeguard/sos/domain/usecase/contact/GetEmergencyContactsUseCase.kt

package com.safeguard.sos.domain.usecase.contact

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.core.extensions.asResource
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.repository.EmergencyContactRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEmergencyContactsUseCase @Inject constructor(
    private val contactRepository: EmergencyContactRepository
) {
    operator fun invoke(): Flow<Resource<List<EmergencyContact>>> {
        return contactRepository.getContacts().asResource()
    }

    suspend fun getSync(): List<EmergencyContact> {
        return contactRepository.getContactsSync()
    }

    suspend fun getPrimaryContact(): EmergencyContact? {
        return contactRepository.getPrimaryContact()
    }
}