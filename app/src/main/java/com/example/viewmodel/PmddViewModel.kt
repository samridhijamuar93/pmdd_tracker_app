package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SymptomRepository
import com.example.engine.CpassClinicalEngine
import com.example.engine.CpassCycleResult
import com.example.engine.CpassProspectiveReport
import com.example.model.DailyLogEntity
import com.example.model.PeriodFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.data.WearableHealthSyncManager
import com.example.model.BiometricCycleCorrelation
import com.example.model.CgmReadingPoint
import com.example.model.DailyBiometricSummary
import com.example.model.DeviceConnectionInfo
import com.example.model.WearableDeviceType
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class AppScreen(val title: String, val subtitle: String) {
    HOME("Homavales", "A mental health - hormone correlation app"),
    LOG_DRSP("Daily Log", "Rate today's 21 symptom items (1-6 scale)"),
    CPASS_DIAGNOSIS("Results", "Cycle assessment & symptom trends"),
    CYCLE_TRENDS("Cycle Trends", "Luteal surge vs Follicular remission"),
    WEARABLES_CGM("Wearables & CGM", "Apple Watch, Pixel Watch & CGM Biometrics"),
    CLINICAL_REPORT("Export Report", "Official clinical PDF export for OB/GYN or Psychiatrist"),
    DSM5_GUIDE("Your Guide", "PMS, PME & PMDD differential guide")
}

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

class PmddViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SymptomRepository

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val _selectedDate = MutableStateFlow(LocalDate.now().format(dateFormatter))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _activeDailyLog = MutableStateFlow(DailyLogEntity(dateString = LocalDate.now().format(dateFormatter)))
    val activeDailyLog: StateFlow<DailyLogEntity> = _activeDailyLog.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    // Wearables & CGM Biometric States
    private val _connectedDevices = MutableStateFlow<List<DeviceConnectionInfo>>(WearableHealthSyncManager.getInitialDevices())
    val connectedDevices: StateFlow<List<DeviceConnectionInfo>> = _connectedDevices.asStateFlow()

    private val _isSyncingDevices = MutableStateFlow(false)
    val isSyncingDevices: StateFlow<Boolean> = _isSyncingDevices.asStateFlow()

    private val _cgmIntradayTrace = MutableStateFlow<List<CgmReadingPoint>>(WearableHealthSyncManager.generate24HourCgmTrace(isLutealPhase = true))
    val cgmIntradayTrace: StateFlow<List<CgmReadingPoint>> = _cgmIntradayTrace.asStateFlow()

    private val _selectedWearable = MutableStateFlow(WearableDeviceType.PIXEL_WATCH)
    val selectedWearable: StateFlow<WearableDeviceType> = _selectedWearable.asStateFlow()

    private val _selectedCgm = MutableStateFlow(WearableDeviceType.DEXCOM_CGM)
    val selectedCgm: StateFlow<WearableDeviceType> = _selectedCgm.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SymptomRepository(database.dailyLogDao())

        viewModelScope.launch {
            repository.allLogs.collect { logs ->
                loadLogForDate(_selectedDate.value)
            }
        }
    }

    val allLogs: StateFlow<List<DailyLogEntity>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val cpassReport: StateFlow<CpassProspectiveReport> = repository.allLogs
        .combine(_selectedDate) { logs, _ ->
            CpassClinicalEngine.evaluateProspectiveTracking(logs)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CpassClinicalEngine.evaluateProspectiveTracking(emptyList())
        )

    val segmentedCycles: StateFlow<List<CpassCycleResult>> = repository.allLogs
        .combine(_selectedDate) { logs, _ ->
            CpassClinicalEngine.segmentAndScoreCycles(logs)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val biometricCorrelation: StateFlow<BiometricCycleCorrelation> = repository.allLogs
        .combine(_selectedDate) { logs, _ ->
            WearableHealthSyncManager.computeCorrelation(logs)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WearableHealthSyncManager.computeCorrelation(emptyList())
        )

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setSelectedDate(dateString: String) {
        _selectedDate.value = dateString
        loadLogForDate(dateString)
    }

    fun stepDay(offsetDays: Long) {
        try {
            val parsed = LocalDate.parse(_selectedDate.value, dateFormatter)
            val newDate = parsed.plusDays(offsetDays)
            setSelectedDate(newDate.format(dateFormatter))
        } catch (e: Exception) {
            setSelectedDate(LocalDate.now().format(dateFormatter))
        }
    }

    fun setToday() {
        setSelectedDate(LocalDate.now().format(dateFormatter))
    }

    private fun loadLogForDate(dateString: String) {
        viewModelScope.launch {
            val existing = repository.getLogByDate(dateString)
            _activeDailyLog.value = existing ?: DailyLogEntity(dateString = dateString)
        }
    }

    // DRSP Item Scoring
    fun updateDrspItem(itemNumber: Int, score: Int) {
        val validScore = score.coerceIn(1, 6)
        val cur = _activeDailyLog.value
        _activeDailyLog.value = when (itemNumber) {
            1 -> cur.copy(drsp1_depressed = validScore)
            2 -> cur.copy(drsp2_hopeless = validScore)
            3 -> cur.copy(drsp3_worthless = validScore)
            4 -> cur.copy(drsp4_anxious = validScore)
            5 -> cur.copy(drsp5_mood_swings = validScore)
            6 -> cur.copy(drsp6_rejection_sensitive = validScore)
            7 -> cur.copy(drsp7_angry_irritable = validScore)
            8 -> cur.copy(drsp8_conflicts = validScore)
            9 -> cur.copy(drsp9_decreased_interest = validScore)
            10 -> cur.copy(drsp10_social_withdrawal = validScore)
            11 -> cur.copy(drsp11_concentration = validScore)
            12 -> cur.copy(drsp12_tired_low_energy = validScore)
            13 -> cur.copy(drsp13_appetite_increase = validScore)
            14 -> cur.copy(drsp14_food_cravings = validScore)
            15 -> cur.copy(drsp15_insomnia = validScore)
            16 -> cur.copy(drsp16_hypersomnia = validScore)
            17 -> cur.copy(drsp17_overwhelmed = validScore)
            18 -> cur.copy(drsp18_out_of_control = validScore)
            19 -> cur.copy(drsp19_breast_tenderness = validScore)
            20 -> cur.copy(drsp20_headaches_body_aches = validScore)
            21 -> cur.copy(drsp21_bloating_weight = validScore)
            22 -> cur.copy(drsp22_impairment_work = validScore)
            23 -> cur.copy(drsp23_impairment_relationships = validScore)
            24 -> cur.copy(drsp24_impairment_social = validScore)
            else -> cur
        }
    }

    // Cycle Day Override & Validation
    fun updateCycleDay(day: Int?): Boolean {
        if (day == null) {
            _activeDailyLog.value = _activeDailyLog.value.copy(cycleDay = null)
            return true
        }
        val coerced = day.coerceIn(1, 60)
        if (coerced == 1 && !_activeDailyLog.value.isBleeding) {
            showNotification(
                "Validation: Day 1 marks menstrual onset. Please set a bleeding flow (Light, Medium, Heavy, Spotting) to select Day 1.",
                isError = true
            )
            return false
        }
        _activeDailyLog.value = _activeDailyLog.value.copy(cycleDay = coerced)
        return true
    }

    fun editLogForDate(dateString: String) {
        setSelectedDate(dateString)
        navigateTo(AppScreen.LOG_DRSP)
    }

    fun updatePeriodFlow(flow: PeriodFlow) {
        val currentLog = _activeDailyLog.value
        // If switching to no bleeding while cycleDay is 1, clear or adjust cycle day
        val updatedDay = if (!flow.isBleeding && currentLog.cycleDay == 1) {
            null
        } else {
            currentLog.cycleDay
        }
        _activeDailyLog.value = currentLog.copy(periodFlow = flow.name, cycleDay = updatedDay)
    }

    fun updateNotes(notes: String) {
        _activeDailyLog.value = _activeDailyLog.value.copy(notes = notes)
    }

    fun updateMedication(tookMed: Boolean, details: String) {
        _activeDailyLog.value = _activeDailyLog.value.copy(
            tookMedication = tookMed,
            medicationDetails = details
        )
    }

    fun setAllSymptomsScore(score: Int) {
        val s = score.coerceIn(1, 6)
        val cur = _activeDailyLog.value
        _activeDailyLog.value = cur.copy(
            drsp1_depressed = s,
            drsp2_hopeless = s,
            drsp3_worthless = s,
            drsp4_anxious = s,
            drsp5_mood_swings = s,
            drsp6_rejection_sensitive = s,
            drsp7_angry_irritable = s,
            drsp8_conflicts = s,
            drsp9_decreased_interest = s,
            drsp10_social_withdrawal = s,
            drsp11_concentration = s,
            drsp12_tired_low_energy = s,
            drsp13_appetite_increase = s,
            drsp14_food_cravings = s,
            drsp15_insomnia = s,
            drsp16_hypersomnia = s,
            drsp17_overwhelmed = s,
            drsp18_out_of_control = s,
            drsp19_breast_tenderness = s,
            drsp20_headaches_body_aches = s,
            drsp21_bloating_weight = s,
            drsp22_impairment_work = s,
            drsp23_impairment_relationships = s,
            drsp24_impairment_social = s
        )
    }

    fun saveCurrentLog() {
        viewModelScope.launch {
            try {
                repository.saveLog(_activeDailyLog.value)
                showNotification("Daily log saved for ${_activeDailyLog.value.dateString}")
            } catch (e: Exception) {
                showNotification("Failed to save log: ${e.message}", isError = true)
            }
        }
    }

    fun deleteCurrentLog() {
        viewModelScope.launch {
            repository.deleteLog(_selectedDate.value)
            loadLogForDate(_selectedDate.value)
            showNotification("Deleted log for ${_selectedDate.value}")
        }
    }

    fun loadDemoCpassData() {
        viewModelScope.launch {
            try {
                repository.loadDemoProspectiveCpassCycles()
                loadLogForDate(_selectedDate.value)
                showNotification("Loaded 2 consecutive prospective cycles (56 daily DRSP logs with confirmed C-PASS PMDD pattern)")
            } catch (e: Exception) {
                showNotification("Error loading demo data: ${e.message}", isError = true)
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
            loadLogForDate(_selectedDate.value)
            showNotification("All tracking data cleared")
        }
    }

    fun selectWearableDevice(type: WearableDeviceType) {
        _selectedWearable.value = type
        showNotification("Selected ${type.displayName}")
    }

    fun selectCgmDevice(type: WearableDeviceType) {
        _selectedCgm.value = type
        showNotification("Selected ${type.displayName}")
    }

    fun toggleDeviceConnection(type: WearableDeviceType) {
        val currentList = _connectedDevices.value.toMutableList()
        val index = currentList.indexOfFirst { it.deviceType == type }
        if (index != -1) {
            val dev = currentList[index]
            val updated = dev.copy(
                isConnected = !dev.isConnected,
                lastSyncFormatted = if (!dev.isConnected) "Just now" else dev.lastSyncFormatted
            )
            currentList[index] = updated
            _connectedDevices.value = currentList
            showNotification("${type.displayName} is now ${if (updated.isConnected) "Connected" else "Disconnected"}")
        }
    }

    fun syncAllDevices() {
        viewModelScope.launch {
            _isSyncingDevices.value = true
            showNotification("Syncing Pixel Watch, Apple Watch & Dexcom CGM...")
            delay(1200) // Simulated Bluetooth & Cloud Health Connect handshake

            val curLog = _activeDailyLog.value
            val isLuteal = (curLog.cycleDay ?: 22) in 20..28
            val biometrics = WearableHealthSyncManager.synthesizeBiometricsForDay(
                dateString = curLog.dateString,
                cycleDay = curLog.cycleDay,
                isLuteal = isLuteal,
                wearableSource = "${_selectedWearable.value.displayName} & ${_selectedCgm.value.displayName}"
            )

            _cgmIntradayTrace.value = WearableHealthSyncManager.generate24HourCgmTrace(
                isLutealPhase = isLuteal,
                baseSeed = curLog.dateString.hashCode().toLong() + System.currentTimeMillis()
            )

            // Update active log with synced metrics
            val updatedLog = curLog.copy(
                restingHeartRate = biometrics.restingHeartRate,
                hrvRmssd = biometrics.hrvRmssd,
                sleepDurationHours = biometrics.sleepDurationHours,
                deepSleepMinutes = biometrics.deepSleepMinutes,
                remSleepMinutes = biometrics.remSleepMinutes,
                basalBodyTempF = biometrics.basalBodyTempF,
                stepCount = biometrics.stepCount,
                cgmCurrentGlucose = biometrics.cgmCurrentGlucose,
                cgmMeanGlucose = biometrics.cgmMeanGlucose,
                cgmTimeInRangePct = biometrics.cgmTimeInRangePct,
                cgmGlucoseCvPct = biometrics.cgmGlucoseCvPct,
                wearableSource = biometrics.wearableSource
            )
            _activeDailyLog.value = updatedLog
            repository.saveLog(updatedLog)

            _isSyncingDevices.value = false
            showNotification("Wearable & CGM sync complete! Synced HRV ${biometrics.hrvRmssd.toInt()}ms, RHR ${biometrics.restingHeartRate}bpm, CGM ${biometrics.cgmCurrentGlucose.toInt()} mg/dL")
        }
    }

    fun updateManualBiometrics(
        restingHeartRate: Int?,
        hrvRmssd: Double?,
        sleepDurationHours: Double?,
        deepSleepMinutes: Int?,
        basalBodyTempF: Double?,
        stepCount: Int?,
        cgmGlucose: Double?
    ) {
        val cur = _activeDailyLog.value
        _activeDailyLog.value = cur.copy(
            restingHeartRate = restingHeartRate ?: cur.restingHeartRate,
            hrvRmssd = hrvRmssd ?: cur.hrvRmssd,
            sleepDurationHours = sleepDurationHours ?: cur.sleepDurationHours,
            deepSleepMinutes = deepSleepMinutes ?: cur.deepSleepMinutes,
            basalBodyTempF = basalBodyTempF ?: cur.basalBodyTempF,
            stepCount = stepCount ?: cur.stepCount,
            cgmCurrentGlucose = cgmGlucose ?: cur.cgmCurrentGlucose
        )
    }

    fun dismissNotification() {
        _notification.value = null
    }

    fun showNotification(msg: String, isError: Boolean = false) {
        _notification.value = UiNotification(msg, isError)
    }
}
