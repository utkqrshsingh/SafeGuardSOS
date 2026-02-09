package com.safeguard.sos.presentation.components.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.safeguard.sos.databinding.ItemNearbyHelperBinding
import com.safeguard.sos.domain.model.Helper

class NearbyHelpersAdapter(
    private val onItemClick: (Helper) -> Unit
) : ListAdapter<Helper, NearbyHelpersAdapter.HelperViewHolder>(HelperDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelperViewHolder {
        val binding = ItemNearbyHelperBinding.inflate(
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
        private val binding: ItemNearbyHelperBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(helper: Helper) {
            binding.apply {
                // Initials
                tvInitials.text = helper.initials

                // Name (first name only)
                tvName.text = helper.name.split(" ").firstOrNull() ?: helper.name

                // Distance (Helper model doesn't have distance, it's usually calculated)
                tvDistance.visibility = android.view.View.GONE

                // Verified badge
                ivVerified.visibility = if (helper.isVerified) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Rating
                ratingContainer.visibility = android.view.View.VISIBLE
                tvRating.text = helper.displayRating

                // Click listener
                root.setOnClickListener {
                    onItemClick(helper)
                }
            }
        }
    }

    class HelperDiffCallback : DiffUtil.ItemCallback<Helper>() {
        override fun areItemsTheSame(oldItem: Helper, newItem: Helper): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Helper, newItem: Helper): Boolean {
            return oldItem == newItem
        }
    }
}
