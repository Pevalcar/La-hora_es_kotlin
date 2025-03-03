package com.pevalcar.lahoraes.usecase

import android.content.Context
import android.content.Intent
import com.pevalcar.lahoraes.TimeService
import com.pevalcar.lahoraes.TimeSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.ContextCompat

@Singleton
class TimeServiceUseCase @Inject constructor(
    private val context: Context,
) {
    fun startService(
        interval: Int,
        use24Format: Boolean,
        wakeLockEnabled: Boolean
    ) {
        val serviceIntent = Intent(context, TimeService::class.java).apply {
            putExtra("interval", interval)
            putExtra("use24Format", use24Format)
            putExtra("wakeLock", wakeLockEnabled)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    fun stopService() {
        context.stopService(Intent(context, TimeService::class.java))
    }
}