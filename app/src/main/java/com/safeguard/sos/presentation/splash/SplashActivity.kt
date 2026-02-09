// app/src/main/java/com/safeguard/sos/presentation/splash/SplashActivity.kt

package com.safeguard.sos.presentation.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.safeguard.sos.core.base.BaseActivity
import com.safeguard.sos.databinding.ActivitySplashBinding
import com.safeguard.sos.presentation.auth.AuthActivity
import com.safeguard.sos.presentation.main.MainActivity
import com.safeguard.sos.presentation.onboarding.OnboardingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private val viewModel: SplashViewModel by viewModels()

    override fun getViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep splash screen visible while checking auth state
        splashScreen.setKeepOnScreenCondition { viewModel.isLoading.value }

        setupViews()
        observeData()
    }

    override fun setupViews() {
        // Initial state - hidden
        binding.ivLogo.alpha = 0f
        binding.ivLogo.scaleX = 0.5f
        binding.ivLogo.scaleY = 0.5f
        binding.tvAppName.alpha = 0f
        binding.tvTagline.alpha = 0f
        binding.progressIndicator.alpha = 0f

        // Start animations after a short delay
        lifecycleScope.launch {
            delay(100)
            startAnimations()
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { event ->
                    when (event) {
                        is SplashViewModel.NavigationEvent.NavigateToOnboarding -> {
                            navigateToOnboarding()
                        }
                        is SplashViewModel.NavigationEvent.NavigateToAuth -> {
                            navigateToAuth()
                        }
                        is SplashViewModel.NavigationEvent.NavigateToMain -> {
                            navigateToMain()
                        }
                    }
                }
            }
        }
    }

    private fun startAnimations() {
        // Logo animation
        binding.ivLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        // App name animation
        binding.tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(400)
            .setDuration(600)
            .start()

        // Tagline animation
        binding.tvTagline.animate()
            .alpha(1f)
            .setStartDelay(600)
            .setDuration(600)
            .start()

        // Progress indicator
        binding.progressIndicator.animate()
            .alpha(1f)
            .setStartDelay(800)
            .setDuration(400)
            .start()
    }

    private fun navigateToOnboarding() {
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}