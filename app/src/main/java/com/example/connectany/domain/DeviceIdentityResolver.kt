package com.example.connectany.domain

import com.example.connectany.domain.model.DeviceIdentity

interface DeviceIdentityResolver {
    suspend fun resolve(address: String, name: String?): DeviceIdentity?
}
