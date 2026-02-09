package com.safeguard.sos.presentation.components.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safeguard.sos.R
import com.safeguard.sos.databinding.ItemRespondingHelperBinding
import com.safeguard.sos.domain.model.HelperResponse
import com.safeguard.sos.domain.model.ResponseStatus

class RespondingHelpersAdapter(
    private val onCallClick: (HelperResponse) -> Unit,
    private val onMessageClick: (HelperResponse) -> Unit
) : ListAdapter<HelperResponse, RespondingHelpersAdapter.HelperViewHolder>(HelperDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelperViewHolder {
        val binding = ItemRespondingHelperBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HelperViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HelperViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class HelperViewHolder(
        private val binding: ItemRespondingHelperBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(response: HelperResponse) {
            binding.apply {
                // Initials
                tvInitials.text = response.helperName.split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")

                // Name
                tvName.text = response.helperName

                // Verified badge - HelperResponse doesn't have isVerified, 
                // assuming all responders are verified or handled in model
                ivVerified.visibility = android.view.View.GONE

                // Distance
                val distanceText = response.distanceKm?.let { distance ->
                    if (distance < 1.0f) "${(distance * 1000).toInt()}m away"
                    else String.format("%.1f km away", distance)
                } ?: "Distance unknown"
                tvDistance.text = distanceText

                // Status
                val (statusText, statusColor) = when (response.status) {
                    ResponseStatus.RESPONDING -> Pair(
                        root.context.getString(R.string.on_the_way),
                        android.R.color.holo_orange_dark
                    )
                    ResponseStatus.ARRIVED -> Pair(
                        root.context.getString(R.string.arrived),
                        android.R.color.holo_green_dark
                    )
                    else -> Pair(
                        response.status.displayName,
                        android.R.color.darker_gray
                    )
                }
                tvStatus.text = statusText
                tvStatus.setTextColor(ContextCompat.getColor(root.context, statusColor))

                // ETA
                response.estimatedArrivalMinutes?.let { eta ->
                    cardEta.visibility = android.view.View.VISIBLE
                    tvEta.text = root.context.getString(R.string.eta_minutes, eta)
                } ?: run {
                    cardEta.visibility = android.view.View.GONE
                }

                // Avatar color based on status
                val avatarColor = when (response.status) {
                    ResponseStatus.ARRIVED -> android.R.color.holo_green_light
                    ResponseStatus.RESPONDING -> android.R.color.holo_orange_light
                    else -> android.R.color.darker_gray
                }
                cardAvatar.setCardBackgroundColor(
                    ContextCompat.getColor(root.context, avatarColor)
                )

                // Click listeners
                btnCall.setOnClickListener {
                    onCallClick(response)
                }

                btnMessage.setOnClickListener {
                    onMessageClick(response)
                }
            }
        }
    }

    class HelperDiffCallback : DiffUtil.ItemCallback<HelperResponse>() {
        override fun areItemsTheSame(oldItem: HelperResponse, newItem: HelperResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HelperResponse, newItem: HelperResponse): Boolean {
            return oldItem == newItem
        }
    }
}
