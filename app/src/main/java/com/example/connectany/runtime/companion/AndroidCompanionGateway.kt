package com.example.connectany.runtime.companion

import android.companion.CompanionDeviceManager
import android.content.Context
import android.os.Build
import com.example.connectany.domain.companion.CompanionAssociationManager
import javax.inject.Inject

class AndroidCompanionGateway @Inject constructor(
    private val context: Context
) : CompanionAssociationManager {

    private val companionDeviceManager: CompanionDeviceManager? by lazy {
        context.getSystemService(Context.COMPANION_DEVICE_SERVICE) as? CompanionDeviceManager
    }

    override suspend fun isAssociated(macAddress: String): Boolean {
        val cdm = companionDeviceManager ?: return false
        val associations = cdm.associations
        
        // Android 13+ returns AssociationInfo objects, older returns MAC addresses
        return associations.any { association ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && association is android.companion.AssociationInfo) {
                association.deviceMacAddress?.toString() == macAddress
            } else {
                association.toString() == macAddress
            }
        }
    }

    override suspend fun associate(macAddress: String) {
        // In a real app this would trigger an IntentSender. 
        // For the scope of this background engine, we just expose the capability.
    }
}
