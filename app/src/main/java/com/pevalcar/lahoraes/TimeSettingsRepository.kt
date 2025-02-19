package com.pevalcar.lahoraes

object TimeSettingsRepository {
    private var currentInterval = 5
    private var use24HourFormat = true

    fun getInterval() = currentInterval
    fun getTimeFormat() = use24HourFormat

    fun updateInterval(interval: Int) {
        currentInterval = interval
    }

    fun updateTimeFormat(use24: Boolean) {
        use24HourFormat = use24
    }
}