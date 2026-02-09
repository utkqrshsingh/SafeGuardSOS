// app/src/main/java/com/safeguard/sos/di/UseCaseModule.kt

package com.safeguard.sos.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    // Use cases with @Inject constructor are automatically provided by Hilt.
    // Add custom providers here if needed for interfaces or third-party classes.
}
