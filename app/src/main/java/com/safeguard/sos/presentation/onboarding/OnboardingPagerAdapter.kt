// app/src/main/java/com/safeguard/sos/presentation/onboarding/OnboardingPagerAdapter.kt

package com.safeguard.sos.presentation.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.safeguard.sos.databinding.ItemOnboardingPageBinding

data class OnboardingPage(
    val title: String,
    val description: String,
    @DrawableRes val imageRes: Int,
    @ColorRes val backgroundColor: Int
)

class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    class OnboardingViewHolder(
        private val binding: ItemOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OnboardingPage) {
            binding.apply {
                tvTitle.text = page.title
                tvDescription.text = page.description
                ivImage.setImageResource(page.imageRes)

                // Set accent tint for the image
                ivImage.setColorFilter(
                    ContextCompat.getColor(root.context, com.safeguard.sos.R.color.accent_primary)
                )
            }
        }
    }
}