package com.safeguard.sos.presentation.components.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safeguard.sos.R
import com.safeguard.sos.databinding.ItemHelpHistoryBinding
import com.safeguard.sos.domain.model.HelpHistoryItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HelpHistoryAdapter(
    private val onItemClick: (HelpHistoryItem) -> Unit
) : ListAdapter<HelpHistoryItem, HelpHistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHelpHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HistoryViewHolder(
        private val binding: ItemHelpHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        fun bind(item: HelpHistoryItem) {
            binding.apply {
                // User initials
                tvInitials.text = item.userName.split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")

                // User name (first name only)
                tvUserName.text = item.userName.split(" ").firstOrNull() ?: item.userName

                // Emergency type
                tvEmergencyType.text = item.emergencyType.replaceFirstChar { it.uppercase() }

                // Date
                tvDate.text = dateFormat.format(Date(item.timestamp))

                // Response time
                item.responseTime?.let { time ->
                    tvResponseTime.text = root.context.getString(R.string.response_time_format, time)
                }

                // Click listener
                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }
    }

    class HistoryDiffCallback : DiffUtil.ItemCallback<HelpHistoryItem>() {
        override fun areItemsTheSame(oldItem: HelpHistoryItem, newItem: HelpHistoryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HelpHistoryItem, newItem: HelpHistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}