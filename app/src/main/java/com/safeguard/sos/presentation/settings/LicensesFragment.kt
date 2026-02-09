package com.safeguard.sos.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.safeguard.sos.core.base.BaseFragment
import com.safeguard.sos.databinding.FragmentLicensesBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LicensesFragment : BaseFragment<FragmentLicensesBinding>() {

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLicensesBinding {
        return FragmentLicensesBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        loadLicenses()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            navController.navigateUp()
        }
    }

    private fun loadLicenses() {
        binding.webView.apply {
            settings.javaScriptEnabled = false
            loadDataWithBaseURL(
                null,
                getLicensesHtml(),
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    private fun getLicensesHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                        padding: 16px;
                        background-color: #0D0D0D;
                        color: #E0E0E0;
                        line-height: 1.6;
                    }
                    h1 {
                        color: #00D9FF;
                        font-size: 24px;
                        border-bottom: 1px solid #333;
                        padding-bottom: 8px;
                    }
                    h2 {
                        color: #FFFFFF;
                        font-size: 18px;
                        margin-top: 24px;
                    }
                    h3 {
                        color: #B0B0B0;
                        font-size: 14px;
                        font-weight: normal;
                    }
                    pre {
                        background-color: #1A1A2E;
                        padding: 12px;
                        border-radius: 8px;
                        overflow-x: auto;
                        font-size: 12px;
                    }
                    a {
                        color: #00D9FF;
                    }
                </style>
            </head>
            <body>
                <h1>Open Source Licenses</h1>
                
                <p>SafeGuard SOS is built using the following open source libraries:</p>
                
                <h2>Kotlin</h2>
                <h3>Apache License 2.0</h3>
                <p>Copyright JetBrains s.r.o.</p>
                
                <h2>AndroidX Libraries</h2>
                <h3>Apache License 2.0</h3>
                <p>Copyright The Android Open Source Project</p>
                
                <h2>Material Components for Android</h2>
                <h3>Apache License 2.0</h3>
                <p>Copyright Google Inc.</p>
                
                <h2>Retrofit</h2>
                <h3>Apache License 2.0</h3>
                <p>Copyright Square, Inc.</p>
                
                <h2>OkHttp</h2>
                <h3>Apache License 2.0</h3>
                <p>Copyright Square, Inc.</p>
                
                <h2>Glide</h2>
                <h3>BSD, part MIT and Apache 2.0</h3>
                <p>Copyright Bumptech</p>
                
                <h2>Lottie</h2>
                <h3>Apache License 2.0</h3>
                <p>Copyright Airbnb, Inc.</p>
                
                <h2>Hilt</h2>
                <h3>Apache License 2.0</h3>
                <p>Copyright Google Inc.</p>
                
                <h2>Firebase</h2>
                <h3>Apache License 2.0</h3>
                <p>Copyright Google Inc.</p>
                
                <h2>Timber</h2>
                <h3>Apache License 2.0</h3>
                <p>Copyright Jake Wharton</p>
                
                <h2>Kotlin Coroutines</h2>
                <h3>Apache License 2.0</h3>
                <p>Copyright JetBrains s.r.o.</p>
                
                <hr style="border-color: #333; margin: 32px 0;">
                
                <h2>Apache License 2.0</h2>
                <pre>
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
                </pre>
                
            </body>
            </html>
        """.trimIndent()
    }
}
