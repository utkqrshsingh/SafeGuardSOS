package com.safeguard.sos.presentation.contacts

import app.cash.turbine.test
import com.safeguard.sos.core.common.Resource
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.domain.model.Relationship
import com.safeguard.sos.domain.usecase.contact.AddEmergencyContactUseCase
import com.safeguard.sos.domain.usecase.contact.GetEmergencyContactsUseCase
import com.safeguard.sos.domain.usecase.contact.DeleteEmergencyContactUseCase
import com.safeguard.sos.domain.usecase.contact.UpdateEmergencyContactUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactsViewModelTest {

    private lateinit var viewModel: ContactsViewModel
    private lateinit var getEmergencyContactsUseCase: GetEmergencyContactsUseCase
    private lateinit var addEmergencyContactUseCase: AddEmergencyContactUseCase
    private lateinit var updateEmergencyContactUseCase: UpdateEmergencyContactUseCase
    private lateinit var deleteEmergencyContactUseCase: DeleteEmergencyContactUseCase

    private val testDispatcher = StandardTestDispatcher()

    private val testContacts = listOf(
        EmergencyContact(
            id = "1",
            userId = "user1",
            name = "John Doe",
            phoneNumber = "9876543210",
            relationship = Relationship.PARENT,
            isPrimary = true,
            notifyViaCall = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        ),
        EmergencyContact(
            id = "2",
            userId = "user1",
            name = "Jane Doe",
            phoneNumber = "9876543211",
            relationship = Relationship.SPOUSE,
            notifyViaCall = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        getEmergencyContactsUseCase = mockk()
        addEmergencyContactUseCase = mockk()
        updateEmergencyContactUseCase = mockk()
        deleteEmergencyContactUseCase = mockk()

        coEvery { getEmergencyContactsUseCase() } returns flowOf(Resource.Success(testContacts))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadContacts should update state with contacts`() = runTest {
        viewModel = ContactsViewModel(
            getEmergencyContactsUseCase,
            addEmergencyContactUseCase,
            updateEmergencyContactUseCase,
            deleteEmergencyContactUseCase
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.contacts.size)
            assertEquals("John Doe", state.contacts[0].name)
            assertTrue(state.canAddMore)
        }
    }

    @Test
    fun `onSearchQueryChanged should filter contacts`() = runTest {
        viewModel = ContactsViewModel(
            getEmergencyContactsUseCase,
            addEmergencyContactUseCase,
            updateEmergencyContactUseCase,
            deleteEmergencyContactUseCase
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChanged("John")

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.filteredContacts.size)
            assertEquals("John Doe", state.filteredContacts[0].name)
        }
    }

    @Test
    fun `deleteContact should remove contact and emit event`() = runTest {
        coEvery { deleteEmergencyContactUseCase(any()) } returns Resource.Success(true)
        coEvery { getEmergencyContactsUseCase() } returns flowOf(Resource.Success(testContacts.drop(1)))

        viewModel = ContactsViewModel(
            getEmergencyContactsUseCase,
            addEmergencyContactUseCase,
            updateEmergencyContactUseCase,
            deleteEmergencyContactUseCase
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteContact("1")

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.events.test {
            val event = awaitItem()
            assertTrue(event is ContactsEvent.ContactDeleted)
        }
    }

    @Test
    fun `validateName should return error for empty name`() = runTest {
        viewModel = ContactsViewModel(
            getEmergencyContactsUseCase,
            addEmergencyContactUseCase,
            updateEmergencyContactUseCase,
            deleteEmergencyContactUseCase
        )

        viewModel.onAddNameChanged("")

        viewModel.addContactState.test {
            val state = awaitItem()
            assertNotNull(state.nameError)
            assertEquals("Name is required", state.nameError)
        }
    }

    @Test
    fun `validatePhone should return error for invalid phone`() = runTest {
        viewModel = ContactsViewModel(
            getEmergencyContactsUseCase,
            addEmergencyContactUseCase,
            updateEmergencyContactUseCase,
            deleteEmergencyContactUseCase
        )

        viewModel.onAddPhoneChanged("123")

        viewModel.addContactState.test {
            val state = awaitItem()
            assertNotNull(state.phoneError)
        }
    }

    @Test
    fun `saveNewContact should add contact when valid`() = runTest {
        val newContact = EmergencyContact(
            id = "3",
            userId = "user1",
            name = "New Contact",
            phoneNumber = "9876543212",
            relationship = Relationship.FRIEND,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )

        coEvery { addEmergencyContactUseCase(any(), any(), any(), any(), any(), any()) } returns Resource.Success(newContact)

        viewModel = ContactsViewModel(
            getEmergencyContactsUseCase,
            addEmergencyContactUseCase,
            updateEmergencyContactUseCase,
            deleteEmergencyContactUseCase
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAddNameChanged("New Contact")
        viewModel.onAddPhoneChanged("9876543212")
        viewModel.onAddRelationshipChanged("FRIEND")

        viewModel.saveNewContact()

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.events.test {
            val event = awaitItem()
            assertTrue(event is ContactsEvent.ContactAdded)
        }
    }
}
