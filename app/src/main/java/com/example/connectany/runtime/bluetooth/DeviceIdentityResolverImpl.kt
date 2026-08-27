package com.example.connectany.runtime.bluetooth

import com.example.connectany.data.local.dao.DeviceDao
import com.example.connectany.domain.DeviceIdentityResolver
import com.example.connectany.domain.model.DeviceIdentity
import com.example.connectany.domain.model.DeviceType
import javax.inject.Inject

class DeviceIdentityResolverImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceIdentityResolver {
    override suspend fun resolve(address: String, name: String?): DeviceIdentity? {
        val entity = deviceDao.getDeviceByMac(address) ?: return null
        if (!entity.isEnabled) return null
        
        val type = try {
            DeviceType.valueOf(entity.deviceType)
        } catch (e: Exception) {
            DeviceType.OTHER
        }

        return DeviceIdentity(
            localId = entity.macAddress,
            platformAddress = address,
            normalizedName = entity.name,
            deviceType = type,
            profileHints = emptySet()
        )
    }
}
