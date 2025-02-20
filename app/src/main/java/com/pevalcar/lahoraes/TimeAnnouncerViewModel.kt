package com.pevalcar.lahoraes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeAnnouncerViewModel : ViewModel() {

    private val _wakeLockEnabled = MutableStateFlow(false)
    val wakeLockEnabled: StateFlow<Boolean> = _wakeLockEnabled

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning
    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime
    val availableIntervals = listOf(1, 5, 10, 15, 30, 60)

    private val _selectedInterval = MutableStateFlow(TimeSettingsRepository.getInterval())
    val selectedInterval: StateFlow<Int> = _selectedInterval
    private val _use24HourFormat = MutableStateFlow(TimeSettingsRepository.getTimeFormat())
    val use24HourFormat: StateFlow<Boolean> = _use24HourFormat

    fun updateSelectedInterval(interval: Int) {
        require(interval in availableIntervals) { "Intervalo no válido" }
        _selectedInterval.value = interval
        TimeSettingsRepository.updateInterval(interval)
    }

    fun toggleTimeFormat() {
        val newFormat = !_use24HourFormat.value
        _use24HourFormat.value = newFormat
        TimeSettingsRepository.updateTimeFormat(newFormat)
    }

    init {
        startTimeUpdates()
    }

    private fun startTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                val pattern = if (!_use24HourFormat.value) "HH:mm" else "hh:mm a"
                _currentTime.value = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
                delay(1000) // Actualizar cada segundo
            }
        }
    }


    fun updateWakeLock(enabled: Boolean) {
        _wakeLockEnabled.value = enabled
    }

    fun updateServiceRunning(running: Boolean) {
        _serviceRunning.value = running
    }

}