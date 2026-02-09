package com.safeguard.sos.presentation.settings

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import com.safeguard.sos.BuildConfig
import com.safeguard.sos.R
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentAboutBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AboutFragment : BaseFragment<FragmentAboutBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentAboutBinding {
        return FragmentAboutBinding.inflate(inflater, container, false)
    }

    override fun setupViews() {
        setupToolbar()
        setupUI()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            navigateBack()
        }
    }

    private fun setupUI() {
        binding.apply {
            textAppVersion.text = getString(R.string.version_format, BuildConfig.VERSION_NAME)
            textBuildNumber.text = getString(R.string.build_format, BuildConfig.VERSION_CODE)
        }
    }

    protected override fun setupClickListeners() {
        binding.apply {
            // Website
            cardWebsite.setOnClickListener {
                openUrl("https://safeguardsos.com")
            }

            // GitHub
            cardGithub.setOnClickListener {
                openUrl("https://github.com/safeguardsos")
            }

            // Twitter
            cardTwitter.setOnClickListener {
                openUrl("https://twitter.com/safeguardsos")
            }

            // Licenses
            cardLicenses.setOnClickListener {
                navigateTo(R.id.action_about_to_licenses)
            }

            // Developers
            cardDevelopers.setOnClickListener {
                showDevelopersInfo()
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            showToast(getString(R.string.error_could_not_open_browser))
        }
    }

    private fun showDevelopersInfo() {
        showAlertDialog(
            title = getString(R.string.development_team),
            message = getString(R.string.developers_info_message),
            positiveButton = getString(R.string.close)
        )
    }
}
