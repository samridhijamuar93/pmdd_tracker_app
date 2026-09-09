package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DrspDailyLog
import com.example.data.PmddDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class AppScreen {
    HOME,
    TRACK,
    RESULTS,
    EXPORT,
    GUIDE
}

class PmddVitalsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PmddDatabase.getDatabase(application)
    private val dao = database.drspDao()

    val allLogs: StateFlow<List<DrspDailyLog>> = dao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _selectedRange = MutableStateFlow("30 Days")
    val selectedRange: StateFlow<String> = _selectedRange.asStateFlow()

    private val _activeBiometricTab = MutableStateFlow("Resting HR")
    val activeBiometricTab: StateFlow<String> = _activeBiometricTab.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setSelectedRange(range: String) {
        _selectedRange.value = range
    }

    fun setActiveBiometricTab(tab: String) {
        _activeBiometricTab.value = tab
    }

    fun saveLog(log: DrspDailyLog) {
        viewModelScope.launch {
            dao.insertLog(log)
        }
    }

    fun syncWearables() {
        viewModelScope.launch {
            _isSyncing.value = true
            kotlinx.coroutines.delay(1200)
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val existing = dao.getLogByDate(todayStr) ?: DrspDailyLog(date = todayStr, cycleDay = 26)
            dao.insertLog(
                existing.copy(
                    restingHeartRate = 76,
                    hrvRmssd = 35.0,
                    cgmCurrentGlucose = 114.0,
                    cgmGlucoseVariabilityCv = 23.4
                )
            )
            _isSyncing.value = false
        }
    }
}
