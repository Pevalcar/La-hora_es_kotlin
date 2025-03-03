package com.pevalcar.lahoraes

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeSettingsRepository @Inject constructor() {
    private var currentInterval = 5
    private var use24HourFormat = true
    private var wakeLockEnabled = false
    companion object {
        @Volatile
        private var INSTANCE: TimeSettingsRepository? = null

        fun getInstance(): TimeSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TimeSettingsRepository().also { INSTANCE = it }
            }
        }
    }

    fun getInterval() = currentInterval
    fun getTimeFormat() = use24HourFormat

    fun updateInterval(interval: Int) {
        currentInterval = interval
    }

    fun updateTimeFormat(use24: Boolean) {
        use24HourFormat = use24
    }
    fun getWakeLockState() = wakeLockEnabled
    fun setWakeLockState(enabled: Boolean) { wakeLockEnabled = enabled }
}