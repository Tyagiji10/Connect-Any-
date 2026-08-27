package com.example.connectany.runtime.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.example.connectany.domain.health.PowerGateway
import javax.inject.Inject

class AndroidPowerGateway @Inject constructor(
    private val context: Context
) : PowerGateway {

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    override fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    @android.annotation.SuppressLint("BatteryLife")
    override fun requestIgnoreBatteryOptimizations() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
