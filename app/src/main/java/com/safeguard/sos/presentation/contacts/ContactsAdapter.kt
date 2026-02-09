package com.safeguard.sos.presentation.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safeguard.sos.R
import com.safeguard.sos.databinding.ItemContactBinding
import com.safeguard.sos.domain.model.EmergencyContact

class ContactsAdapter(
    private val onContactClick: (EmergencyContact) -> Unit,
    private val onPrimaryClick: (EmergencyContact) -> Unit,
    private val onCallClick: (EmergencyContact) -> Unit,
    private val onMessageClick: (EmergencyContact) -> Unit
) : ListAdapter<EmergencyContact, ContactsAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getContactAt(position: Int): EmergencyContact? {
        return if (position in 0 until itemCount) getItem(position) else null
    }

    inner class ContactViewHolder(
        private val binding: ItemContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onContactClick(getItem(position))
                }
            }

            binding.buttonCall.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCallClick(getItem(position))
                }
            }

            binding.buttonMessage.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMessageClick(getItem(position))
                }
            }

            binding.buttonPrimary.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPrimaryClick(getItem(position))
                }
            }
        }

        fun bind(contact: EmergencyContact) {
            binding.apply {
                // Set contact name with initial avatar
                textContactName.text = contact.name
                textContactInitial.text = contact.initials

                // Set phone number
                textContactPhone.text = contact.phoneNumber

                // Set relationship
                textContactRelationship.text = contact.relationship.displayName
                textContactRelationship.isVisible = true

                // Set primary indicator
                iconPrimary.isVisible = contact.isPrimary
                // Use default icons if custom ones are missing
                buttonPrimary.setImageResource(
                    if (contact.isPrimary) android.R.drawable.star_big_on
                    else android.R.drawable.star_big_off
                )
                buttonPrimary.contentDescription = if (contact.isPrimary) {
                    root.context.getString(R.string.primary_contact)
                } else {
                    root.context.getString(R.string.set_as_primary)
                }

                // Set notification status
                iconNotification.isVisible = contact.notifyViaSms

                // Set avatar background color based on position/id
                val colors = arrayOf(
                    R.color.avatar_color_1,
                    R.color.avatar_color_2,
                    R.color.avatar_color_3,
                    R.color.avatar_color_4,
                    R.color.avatar_color_5
                )
                val colorIndex = contact.id.hashCode().let { Math.abs(it) % colors.size }
                avatarBackground.setBackgroundResource(colors[colorIndex])
            }
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<EmergencyContact>() {
        override fun areItemsTheSame(oldItem: EmergencyContact, newItem: EmergencyContact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EmergencyContact, newItem: EmergencyContact): Boolean {
            return oldItem == newItem
        }
    }
}
