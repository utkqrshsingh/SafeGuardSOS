package com.safeguard.sos.di

import com.safeguard.sos.data.repository.AuthRepositoryImpl
import com.safeguard.sos.data.repository.EmergencyContactRepositoryImpl
import com.safeguard.sos.data.repository.HelperRepositoryImpl
import com.safeguard.sos.data.repository.LocationRepositoryImpl
import com.safeguard.sos.data.repository.SOSRepositoryImpl
import com.safeguard.sos.data.repository.UserRepositoryImpl
import com.safeguard.sos.domain.repository.AuthRepository
import com.safeguard.sos.domain.repository.EmergencyContactRepository
import com.safeguard.sos.domain.repository.HelperRepository
import com.safeguard.sos.domain.repository.LocationRepository
import com.safeguard.sos.domain.repository.SOSRepository
import com.safeguard.sos.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindSOSRepository(
        sosRepositoryImpl: SOSRepositoryImpl
    ): SOSRepository

    @Binds
    @Singleton
    abstract fun bindHelperRepository(
        helperRepositoryImpl: HelperRepositoryImpl
    ): HelperRepository

    @Binds
    @Singleton
    abstract fun bindEmergencyContactRepository(
        emergencyContactRepositoryImpl: EmergencyContactRepositoryImpl
    ): EmergencyContactRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository
}