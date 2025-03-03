package com.pevalcar.lahoraes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pevalcar.lahoraes.domain.CanAccesToApp
import com.pevalcar.lahoraes.usecase.TimeServiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TimeAnnouncerViewModel @Inject constructor(
    private val timeSettingsRepository: TimeSettingsRepository,
    private val canAccesToApp: CanAccesToApp,
    private val timeServiceUseCase: TimeServiceUseCase,
) : ViewModel() {


    private val _wakeLockEnabled = MutableStateFlow(false)
    val wakeLockEnabled: StateFlow<Boolean> = _wakeLockEnabled

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning
    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime
    val availableIntervals = listOf(1, 5, 10, 15, 30, 60)

    private val _selectedInterval = MutableStateFlow(timeSettingsRepository.getInterval())
    val selectedInterval: StateFlow<Int> = _selectedInterval
    private val _use24HourFormat = MutableStateFlow(timeSettingsRepository.getTimeFormat())
    val use24HourFormat: StateFlow<Boolean> = _use24HourFormat

    private val _blockVersion: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val blockVersion: StateFlow<Boolean> = _blockVersion

    fun updateSelectedInterval(interval: Int) {
        require(interval in availableIntervals) { "Intervalo no válido" }
        _selectedInterval.value = interval
        timeSettingsRepository.updateInterval(interval)
    }

    fun toggleTimeFormat() {
        val newFormat = !_use24HourFormat.value
        _use24HourFormat.value = newFormat
        timeSettingsRepository.updateTimeFormat(newFormat)
    }

    init {

        startTimeUpdates()
        checkUserVersion()
    }

    private fun checkUserVersion() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                canAccesToApp.invoke()
            }
            _blockVersion.value = !result
        }
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

    fun toggleService() {
        if (serviceRunning.value) {
            timeServiceUseCase.stopService()
        } else {
            timeServiceUseCase.startService(
                interval = selectedInterval.value,
                use24Format = use24HourFormat.value,
                wakeLock = wakeLockEnabled.value
            )
        }
        _serviceRunning.value = !serviceRunning.value
    }

    fun updateWakeLock(enabled: Boolean) {
        timeSettingsRepository.setWakeLockState(enabled)
        _wakeLockEnabled.value = enabled
    }


}