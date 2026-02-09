// app/src/main/java/com/safeguard/sos/presentation/onboarding/OnboardingActivity.kt

package com.safeguard.sos.presentation.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseActivity
import com.safeguard.sos.databinding.ActivityOnboardingBinding
import com.safeguard.sos.presentation.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>() {

    private val viewModel: OnboardingViewModel by viewModels()
    private lateinit var pagerAdapter: OnboardingPagerAdapter

    override fun getViewBinding(): ActivityOnboardingBinding {
        return ActivityOnboardingBinding.inflate(layoutInflater)
    }

    override fun setupViews() {
        setupViewPager()
        setupIndicators()
        updateButtonState(0)
    }

    override fun setupClickListeners() {
        binding.btnNext.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem < pagerAdapter.itemCount - 1) {
                binding.viewPager.currentItem = currentItem + 1
            } else {
                completeOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener {
            completeOnboarding()
        }

        binding.btnBack.setOnClickListener {
            val currentItem = binding.viewPager.currentItem
            if (currentItem > 0) {
                binding.viewPager.currentItem = currentItem - 1
            }
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigateToAuth.collect { shouldNavigate ->
                    if (shouldNavigate) {
                        navigateToAuth()
                    }
                }
            }
        }
    }

    private fun setupViewPager() {
        val pages = listOf(
            OnboardingPage(
                title = getString(R.string.onboarding_welcome_title),
                description = getString(R.string.onboarding_welcome_description),
                imageRes = R.drawable.ic_shield_check,
                backgroundColor = R.color.surface_primary
            ),
            OnboardingPage(
                title = getString(R.string.onboarding_sos_title),
                description = getString(R.string.onboarding_sos_description),
                imageRes = R.drawable.ic_sos,
                backgroundColor = R.color.surface_secondary
            ),
            OnboardingPage(
                title = getString(R.string.onboarding_helpers_title),
                description = getString(R.string.onboarding_helpers_description),
                imageRes = R.drawable.ic_helper,
                backgroundColor = R.color.surface_tertiary
            ),
            OnboardingPage(
                title = getString(R.string.onboarding_tracking_title),
                description = getString(R.string.onboarding_tracking_description),
                imageRes = R.drawable.ic_location,
                backgroundColor = R.color.surface_primary
            ),
            OnboardingPage(
                title = getString(R.string.onboarding_verification_title),
                description = getString(R.string.onboarding_verification_description),
                imageRes = R.drawable.ic_verified,
                backgroundColor = R.color.surface_secondary
            )
        )

        pagerAdapter = OnboardingPagerAdapter(pages)
        binding.viewPager.adapter = pagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
                updateButtonState(position)
            }
        })
    }

    private fun setupIndicators() {
        val indicators = Array(pagerAdapter.itemCount) { index ->
            View(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.onboarding_indicator_size),
                    resources.getDimensionPixelSize(R.dimen.onboarding_indicator_size)
                ).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.onboarding_indicator_spacing)
                }
                setBackgroundResource(R.drawable.bg_onboarding_indicator_inactive)
            }
        }

        binding.indicatorContainer.removeAllViews()
        indicators.forEach { binding.indicatorContainer.addView(it) }

        updateIndicators(0)
    }

    private fun updateIndicators(position: Int) {
        for (i in 0 until binding.indicatorContainer.childCount) {
            val indicator = binding.indicatorContainer.getChildAt(i)
            if (i == position) {
                indicator.setBackgroundResource(R.drawable.bg_onboarding_indicator_active)
                indicator.layoutParams = (indicator.layoutParams as android.widget.LinearLayout.LayoutParams).apply {
                    width = resources.getDimensionPixelSize(R.dimen.onboarding_indicator_selected_width)
                }
            } else {
                indicator.setBackgroundResource(R.drawable.bg_onboarding_indicator_inactive)
                indicator.layoutParams = (indicator.layoutParams as android.widget.LinearLayout.LayoutParams).apply {
                    width = resources.getDimensionPixelSize(R.dimen.onboarding_indicator_size)
                }
            }
        }
    }

    private fun updateButtonState(position: Int) {
        val isLastPage = position == pagerAdapter.itemCount - 1
        val isFirstPage = position == 0

        binding.btnNext.text = if (isLastPage) {
            getString(R.string.get_started)
        } else {
            getString(R.string.next)
        }

        binding.btnSkip.visibility = if (isLastPage) View.INVISIBLE else View.VISIBLE
        binding.btnBack.visibility = if (isFirstPage) View.INVISIBLE else View.VISIBLE
    }

    private fun completeOnboarding() {
        viewModel.completeOnboarding()
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}