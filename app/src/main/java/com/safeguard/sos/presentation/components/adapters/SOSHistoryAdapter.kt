package com.safeguard.sos.presentation.components.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safeguard.sos.R
import com.safeguard.sos.databinding.ItemSosHistoryBinding
import com.safeguard.sos.domain.model.SOSAlert
import com.safeguard.sos.domain.model.SOSStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SOSHistoryAdapter(
    private val onItemClick: (SOSAlert) -> Unit
) : ListAdapter<SOSAlert, SOSHistoryAdapter.SOSHistoryViewHolder>(SOSAlertDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SOSHistoryViewHolder {
        val binding = ItemSosHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SOSHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SOSHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SOSHistoryViewHolder(
        private val binding: ItemSosHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        fun bind(alert: SOSAlert) {
            binding.apply {
                // Emergency type
                tvEmergencyType.text = alert.alertType.displayName

                // Date and time
                val date = Date(alert.createdAt)
                tvDate.text = dateFormat.format(date)
                tvTime.text = timeFormat.format(date)

                // Status badge
                val (statusText, statusBgColor, statusTextColor) = when (alert.status) {
                    SOSStatus.RESOLVED -> Triple(
                        root.context.getString(R.string.resolved),
                        android.R.color.holo_green_light,
                        android.R.color.white
                    )
                    SOSStatus.CANCELLED -> Triple(
                        root.context.getString(R.string.cancelled),
                        android.R.color.darker_gray,
                        android.R.color.white
                    )
                    SOSStatus.ACTIVE, SOSStatus.PENDING -> Triple(
                        root.context.getString(R.string.active),
                        android.R.color.holo_red_light,
                        android.R.color.white
                    )
                    SOSStatus.HELP_ON_WAY, SOSStatus.RESPONDED -> Triple(
                        root.context.getString(R.string.help_on_way),
                        android.R.color.holo_blue_light,
                        android.R.color.white
                    )
                    SOSStatus.FALSE_ALARM -> Triple(
                        root.context.getString(R.string.status_false_alarm),
                        android.R.color.black,
                        android.R.color.white
                    )
                }
                tvStatus.text = statusText
                cardStatus.setCardBackgroundColor(
                    ContextCompat.getColor(root.context, statusBgColor)
                )
                tvStatus.setTextColor(ContextCompat.getColor(root.context, statusTextColor))

                // Location (if available)
                tvLocation.text = alert.location.displayAddress
                tvLocation.visibility = android.view.View.VISIBLE

                // Helpers count
                val helpersCount = alert.respondersCount
                if (helpersCount > 0) {
                    tvHelpers.text = root.context.getString(R.string.helpers_count, helpersCount)
                    tvHelpers.visibility = android.view.View.VISIBLE
                } else {
                    tvHelpers.visibility = android.view.View.GONE
                }

                // Duration
                val durationMinutes = alert.durationMinutes
                if (durationMinutes > 0) {
                    tvDuration.text = root.context.getString(R.string.duration_minutes, durationMinutes.toInt())
                    tvDuration.visibility = android.view.View.VISIBLE
                } else {
                    tvDuration.visibility = android.view.View.GONE
                }

                // Click listener
                root.setOnClickListener {
                    onItemClick(alert)
                }
            }
        }
    }

    class SOSAlertDiffCallback : DiffUtil.ItemCallback<SOSAlert>() {
        override fun areItemsTheSame(oldItem: SOSAlert, newItem: SOSAlert): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SOSAlert, newItem: SOSAlert): Boolean {
            return oldItem == newItem
        }
    }
}
