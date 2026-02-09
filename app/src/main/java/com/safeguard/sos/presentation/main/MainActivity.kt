package com.safeguard.sos.presentation.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseActivity
import com.safeguard.sos.core.common.UiEvent
import com.safeguard.sos.databinding.ActivityMainBinding
import com.safeguard.sos.domain.model.UserType
import com.safeguard.sos.presentation.auth.AuthActivity
import com.safeguard.sos.service.location.LocationService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController

    private val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startLocationService()
            viewModel.onPermissionsGranted()
        } else {
            showPermissionRationale()
        }
    }

    override fun getViewBinding(): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun setupViews() {
        setupNavigation()
        setupBottomNavigation()
        checkPermissions()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Handle navigation destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment,
                R.id.sosFragment,
                R.id.contactsFragment,
                R.id.helperDashboardFragment,
                R.id.profileFragment -> {
                    showBottomNavigation()
                }
                else -> {
                    hideBottomNavigation()
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)

        // Update bottom navigation based on user type
        viewModel.uiState.value.userType?.let { userType ->
            updateBottomNavigationForUserType(userType)
        }
    }

    private fun updateBottomNavigationForUserType(userType: UserType) {
        val menu = binding.bottomNavigation.menu

        when (userType) {
            UserType.REGULAR -> {
                menu.findItem(R.id.helperDashboardFragment)?.isVisible = false
            }
            UserType.HELPER -> {
                // Show helper-specific navigation
                menu.findItem(R.id.helperDashboardFragment)?.isVisible = true
            }
            UserType.BOTH -> {
                // Show all navigation items
                menu.findItem(R.id.helperDashboardFragment)?.isVisible = true
            }
        }
    }

    private fun showBottomNavigation() {
        binding.bottomNavigation.animate()
            .translationY(0f)
            .setDuration(200)
            .withStartAction {
                binding.bottomNavigation.isVisible = true
            }
            .start()
    }

    private fun hideBottomNavigation() {
        binding.bottomNavigation.animate()
            .translationY(binding.bottomNavigation.height.toFloat())
            .setDuration(200)
            .withEndAction {
                binding.bottomNavigation.isVisible = false
            }
            .start()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collectLatest { event ->
                    handleUiEvent(event)
                }
            }
        }
    }

    private fun updateUI(state: MainUiState) {
        // Update bottom navigation based on user type
        state.userType?.let { userType ->
            updateBottomNavigationForUserType(userType)
        }

        // Show/hide SOS active indicator
        // binding.sosActiveIndicator.isVisible = state.isSOSActive

        // Update connection status
        // binding.offlineBanner.isVisible = !state.isOnline
    }

    private fun checkPermissions() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            startLocationService()
            viewModel.onPermissionsGranted()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun showPermissionRationale() {
        showAlertDialog(
            title = getString(R.string.permissions_required),
            message = getString(R.string.permissions_rationale),
            positiveButton = getString(R.string.grant_permissions),
            negativeButton = getString(R.string.continue_limited),
            onPositiveClick = { checkPermissions() },
            onNegativeClick = { viewModel.onPermissionsDenied() }
        )
    }

    private fun startLocationService() {
        // val intent = Intent(this, LocationService::class.java)
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //    startForegroundService(intent)
        // } else {
        //    startService(intent)
        // }
    }

    private fun stopLocationService() {
        // val intent = Intent(this, LocationService::class.java)
        // stopService(intent)
    }

    private fun handleIntent(intent: Intent) {
        // handle actions from intent if needed
    }

    private fun navigateToAuth() {
        stopLocationService()
        val intent = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        const val ACTION_TRIGGER_SOS = "com.safeguard.sos.ACTION_TRIGGER_SOS"
        const val ACTION_VIEW_ALERT = "com.safeguard.sos.ACTION_VIEW_ALERT"
        const val ACTION_OPEN_HELPER_DASHBOARD = "com.safeguard.sos.ACTION_OPEN_HELPER_DASHBOARD"
        const val EXTRA_ALERT_ID = "extra_alert_id"
    }
}
