package com.example.connectany.domain

import com.example.connectany.domain.model.ProfileType

interface ProfileConnectionVerifier {
    suspend fun verifyConnection(address: String, profile: ProfileType): Boolean
}
