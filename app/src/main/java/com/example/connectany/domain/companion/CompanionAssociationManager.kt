package com.example.connectany.domain.companion

interface CompanionAssociationManager {
    suspend fun isAssociated(macAddress: String): Boolean
    suspend fun associate(macAddress: String)
}
