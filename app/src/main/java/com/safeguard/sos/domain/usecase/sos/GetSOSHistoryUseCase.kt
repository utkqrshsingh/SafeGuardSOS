// app/src/main/java/com/safeguard/sos/domain/usecase/sos/GetSOSHistoryUseCase.kt

package com.safeguard.sos.domain.usecase.sos

import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.repository.SOSRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetSOSHistoryUseCase @Inject constructor(
    private val sosRepository: SOSRepository
) {
    operator fun invoke(limit: Int? = null): Flow<Resource<List<SOSAlert>>> {
        return sosRepository.getSOSHistory().map { resource ->
            if (limit != null && resource is Resource.Success) {
                Resource.Success(resource.data?.take(limit) ?: emptyList())
            } else {
                resource
            }
        }
    }

    suspend fun getRecent(limit: Int = 10): Resource<List<SOSAlert>> {
        return sosRepository.getRecentSOSHistory(limit)
    }
}
