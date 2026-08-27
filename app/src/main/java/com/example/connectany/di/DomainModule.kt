package com.example.connectany.di

import com.example.connectany.core.time.TimeProvider
import com.example.connectany.core.time.TimeProviderImpl
import com.example.connectany.domain.ConnectionStateMachine
import com.example.connectany.domain.ConnectionStateMachineImpl
import com.example.connectany.domain.DeviceIdentityResolver
import com.example.connectany.domain.ProfileConnectionVerifier
import com.example.connectany.runtime.bluetooth.DeviceIdentityResolverImpl
import com.example.connectany.runtime.bluetooth.ProfileConnectionVerifierImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {

    @Binds
    @Singleton
    abstract fun bindConnectionStateMachine(
        impl: ConnectionStateMachineImpl
    ): ConnectionStateMachine

    @Binds
    @Singleton
    abstract fun bindDeviceIdentityResolver(
        impl: DeviceIdentityResolverImpl
    ): DeviceIdentityResolver

    @Binds
    @Singleton
    abstract fun bindProfileConnectionVerifier(
        impl: ProfileConnectionVerifierImpl
    ): ProfileConnectionVerifier

    @Binds
    @Singleton
    abstract fun bindTimeProvider(
        impl: TimeProviderImpl
    ): TimeProvider
}
