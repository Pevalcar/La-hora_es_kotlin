package com.pevalcar.lahoraes

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimeAnnouncerViewModel : ViewModel() {
    private val _interval = MutableStateFlow(10)
    val interval: StateFlow<Int> = _interval.asStateFlow()

    private val _wakeLockEnabled = MutableStateFlow(false)
    val wakeLockEnabled: StateFlow<Boolean> = _wakeLockEnabled.asStateFlow()

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    fun updateInterval(newValue: Int) {
        _interval.value = newValue
    }

    fun updateWakeLock(enabled: Boolean) {
        _wakeLockEnabled.value = enabled
    }

    fun updateServiceRunning(running: Boolean) {
        _serviceRunning.value = running
    }
}