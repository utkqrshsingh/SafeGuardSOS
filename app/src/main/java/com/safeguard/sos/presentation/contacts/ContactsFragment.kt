package com.safeguard.sos.presentation.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentContactsBinding
import com.safeguard.sos.domain.model.EmergencyContact
import com.safeguard.sos.presentation.components.dialogs.ConfirmationDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ContactsFragment : BaseFragment<FragmentContactsBinding>() {

    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var contactsAdapter: ContactsAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentContactsBinding {
        return FragmentContactsBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupRecyclerView()
        setupSwipeToDelete()
        setupSearchView()
    }

    private fun setupRecyclerView() {
        contactsAdapter = ContactsAdapter(
            onContactClick = { contact ->
                viewModel.onContactClick(contact)
            },
            onPrimaryClick = { contact ->
                viewModel.togglePrimary(contact.id)
            },
            onCallClick = { contact ->
                callContact(contact)
            },
            onMessageClick = { contact ->
                messageContact(contact)
            }
        )

        binding.recyclerViewContacts.apply {
            adapter = contactsAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val contact = contactsAdapter.getContactAt(position)
                if (contact != null) {
                    showDeleteConfirmation(contact)
                } else {
                    contactsAdapter.notifyItemChanged(position)
                }
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewContacts)
    }

    private fun showDeleteConfirmation(contact: EmergencyContact) {
        ConfirmationDialog.show(
            requireContext(),
            ConfirmationDialog.Config(
                title = "Delete Contact",
                message = "Are you sure you want to delete ${contact.name} from your emergency contacts?",
                positiveButtonText = "Delete",
                negativeButtonText = "Cancel",
                isDanger = true,
                onPositiveClick = {
                    viewModel.deleteContact(contact.id)
                },
                onNegativeClick = {
                    viewModel.loadContacts()
                }
            )
        )
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onSearchQueryChanged(newText ?: "")
                return true
            }
        })
    }

    override fun setupClickListeners() {
        binding.apply {
            fabAddContact.setOnClickListener {
                viewModel.onAddContactClick()
            }

            buttonAddFirstContact.setOnClickListener {
                viewModel.onAddContactClick()
            }

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.loadContacts()
            }
            
            toolbar.setNavigationOnClickListener {
                navController.navigateUp()
            }
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    updateUI(state)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        is ContactsEvent.ShowError -> {
                            showToast(event.message)
                        }
                        is ContactsEvent.ShowSuccess -> {
                            showToast(event.message)
                        }
                        is ContactsEvent.NavigateToAdd -> {
                            // navController.navigate(R.id.action_contacts_to_addContact)
                        }
                        is ContactsEvent.NavigateToEdit -> {
                            // val action = ContactsFragmentDirections.actionContactsToEditContact(event.contactId)
                            // navController.navigate(action)
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun updateUI(state: ContactsUiState) {
        binding.apply {
            swipeRefreshLayout.isRefreshing = state.isLoading
            progressBar.isVisible = state.isLoading && state.contacts.isEmpty()

            // Show empty state or contacts list
            val isEmpty = state.filteredContacts.isEmpty() && !state.isLoading
            emptyStateLayout.isVisible = isEmpty && state.searchQuery.isEmpty()
            noResultsLayout.isVisible = isEmpty && state.searchQuery.isNotEmpty()
            recyclerViewContacts.isVisible = !isEmpty

            // Update contact count
            textContactCount.text = getString(
                R.string.contact_count_format,
                state.contactCount,
                state.maxContactsLimit
            )

            // Update remaining slots indicator
            textRemainingSlots.text = when {
                state.remainingSlots == 0 -> getString(R.string.max_contacts_reached)
                state.remainingSlots == 1 -> getString(R.string.one_slot_remaining)
                else -> getString(R.string.slots_remaining, state.remainingSlots)
            }
            textRemainingSlots.setTextColor(
                if (state.remainingSlots == 0)
                    requireContext().getColor(android.R.color.holo_orange_dark)
                else
                    requireContext().getColor(android.R.color.darker_gray)
            )

            // Update FAB visibility
            fabAddContact.isVisible = state.canAddMore

            // Submit list to adapter
            contactsAdapter.submitList(state.filteredContacts)
        }
    }

    private fun callContact(contact: EmergencyContact) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:${contact.phoneNumber}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Unable to open dialer")
        }
    }

    private fun messageContact(contact: EmergencyContact) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("smsto:${contact.phoneNumber}")
            }
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Unable to open messaging app")
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadContacts()
    }
}
