package com.example.connectany.core.time

import android.os.SystemClock
import javax.inject.Inject

class TimeProviderImpl @Inject constructor() : TimeProvider {
    override fun elapsedRealtime(): Long = SystemClock.elapsedRealtime()
}
