package com.safeguard.sos.data.repository

import app.cash.turbine.test
import com.google.firebase.firestore.FirebaseFirestore
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.data.local.dao.SOSHistoryDao
import com.safeguard.sos.data.local.datastore.UserPreferences
import com.safeguard.sos.data.mapper.SOSMapper
import com.safeguard.sos.data.remote.api.SOSApi
import com.safeguard.sos.data.remote.dto.response.SOSAlertResponse
import com.safeguard.sos.domain.model.Location
import com.safeguard.sos.domain.model.SOSStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class SOSRepositoryTest {

    private lateinit var sosRepository: SOSRepositoryImpl
    private lateinit var sosApi: SOSApi
    private lateinit var sosHistoryDao: SOSHistoryDao
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userPreferences: UserPreferences
    private lateinit var sosMapper: SOSMapper

    private val testUserId = "test_user_id"
    private val testLocation = Location(
        latitude = 12.9716,
        longitude = 77.5946,
        address = "Bangalore, Karnataka"
    )

    @Before
    fun setup() {
        sosApi = mockk()
        sosHistoryDao = mockk()
        firestore = mockk(relaxed = true)
        userPreferences = mockk()
        sosMapper = SOSMapper()

        every { userPreferences.userIdFlow } returns flowOf(testUserId)

        sosRepository = SOSRepositoryImpl(
            sosApi,
            sosHistoryDao,
            firestore,
            userPreferences,
            sosMapper
        )
    }

    @Test
    fun `triggerSOS should return success when API call succeeds`() = runTest {
        val mockResponse = SOSAlertResponse(
            id = "sos_123",
            userId = testUserId,
            userName = "Test User",
            userPhone = "+919876543210",
            latitude = testLocation.latitude,
            longitude = testLocation.longitude,
            address = testLocation.address,
            emergencyType = "General",
            status = SOSStatus.ACTIVE.name,
            createdAt = System.currentTimeMillis()
        )

        coEvery { sosApi.triggerSOS(any()) } returns Response.success(mockResponse)
        coEvery { sosHistoryDao.insertSOS(any()) } returns Unit

        sosRepository.triggerSOS(testLocation, "General", null).test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)
            assertEquals("sos_123", (success as Resource.Success).data?.id)
            assertEquals(SOSStatus.ACTIVE, success.data?.status)

            awaitComplete()
        }
    }

    @Test
    fun `triggerSOS should return error when user not logged in`() = runTest {
        every { userPreferences.userIdFlow } returns flowOf(null)

        val repository = SOSRepositoryImpl(
            sosApi,
            sosHistoryDao,
            firestore,
            userPreferences,
            sosMapper
        )

        repository.triggerSOS(testLocation, "General", null).test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)

            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals("User not logged in", (error as Resource.Error).message)

            awaitComplete()
        }
    }

    @Test
    fun `cancelSOS should update status to cancelled`() = runTest {
        coEvery { sosApi.cancelSOS(any()) } returns Response.success(mockk(relaxed = true))
        coEvery { sosHistoryDao.updateSOSStatus(any(), any(), any()) } returns Unit

        sosRepository.cancelSOS("sos_123").test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)

            val success = awaitItem()
            assertTrue(success is Resource.Success)

            awaitComplete()
        }
    }

    @Test
    fun `getSOSHistory should return error when API fails`() = runTest {
        coEvery { sosApi.getSOSHistory(any()) } throws Exception("Network error")

        sosRepository.getSOSHistory().test {
            val loading = awaitItem()
            assertTrue(loading is Resource.Loading)

            val error = awaitItem()
            assertTrue(error is Resource.Error)
            assertEquals("Network error", (error as Resource.Error).message)

            awaitComplete()
        }
    }
}
