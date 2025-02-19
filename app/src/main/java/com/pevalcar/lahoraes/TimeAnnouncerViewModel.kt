package com.pevalcar.lahoraes

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeAnnouncerViewModel : ViewModel() {
    private val _interval = MutableStateFlow(10)
    val interval: StateFlow<Int> = _interval.asStateFlow()

    private val _wakeLockEnabled = MutableStateFlow(false)
    val wakeLockEnabled: StateFlow<Boolean> = _wakeLockEnabled.asStateFlow()

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()
    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()
    val availableIntervals = listOf(1, 5, 10, 15, 30, 60)

    private val _selectedInterval = MutableStateFlow(TimeSettingsRepository.getInterval())
    val selectedInterval: StateFlow<Int> = _selectedInterval.asStateFlow()
    private val _use24HourFormat = MutableStateFlow(TimeSettingsRepository.getTimeFormat())
    val use24HourFormat: StateFlow<Boolean> = _use24HourFormat.asStateFlow()

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
                _currentTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                delay(1000) // Actualizar cada segundo
            }
        }
    }


    fun updateInterval(newValue: Int) {
        _interval.value = newValue
    }

    fun updateWakeLock(enabled: Boolean) {
        _wakeLockEnabled.value = enabled
    }

    fun updateServiceRunning(running: Boolean) {
        _serviceRunning.value = running
    }
    fun speakNow(context: Context) {
        // Enviar broadcast al servicio
        val intent = Intent("ACTION_SPEAK_NOW").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
}