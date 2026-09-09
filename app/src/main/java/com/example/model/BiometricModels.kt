package com.example.model

enum class WearableDeviceType(
    val displayName: String,
    val brand: String,
    val platform: String,
    val isCgm: Boolean
) {
    PIXEL_WATCH("Google Pixel Watch 3 / 2", "Google", "Wear OS / Health Connect", false),
    APPLE_WATCH("Apple Watch Series 9 / Ultra 2", "Apple", "HealthKit Bridge", false),
    FITBIT("Fitbit Sense / Charge 6", "Fitbit", "Health Connect", false),
    SAMSUNG_GALAXY_WATCH("Samsung Galaxy Watch 7 / Ultra", "Samsung", "Wear OS / Health Connect", false),
    DEXCOM_CGM("Dexcom G7 / G6 CGM", "Dexcom", "Continuous Glucose Monitor", true),
    ABBOTT_LIBRE_CGM("Abbott FreeStyle Libre 3", "Abbott", "Continuous Glucose Monitor", true)
}

enum class CgmTrend(val symbol: String, val label: String, val degree: Float) {
    DOUBLE_UP("⇈", "Rising rapidly (>3 mg/dL/min)", 90f),
    SINGLE_UP("↑", "Rising (2-3 mg/dL/min)", 45f),
    FORTY_FIVE_UP("↗", "Rising slightly (1-2 mg/dL/min)", 25f),
    FLAT("→", "Steady (±1 mg/dL/min)", 0f),
    FORTY_FIVE_DOWN("↘", "Falling slightly (1-2 mg/dL/min)", -25f),
    SINGLE_DOWN("↓", "Falling (2-3 mg/dL/min)", -45f),
    DOUBLE_DOWN("⇊", "Falling rapidly (>3 mg/dL/min)", -90f);

    companion object {
        fun fromDelta(deltaPerMin: Double): CgmTrend {
            return when {
                deltaPerMin > 3.0 -> DOUBLE_UP
                deltaPerMin > 1.8 -> SINGLE_UP
                deltaPerMin > 0.8 -> FORTY_FIVE_UP
                deltaPerMin < -3.0 -> DOUBLE_DOWN
                deltaPerMin < -1.8 -> SINGLE_DOWN
                deltaPerMin < -0.8 -> FORTY_FIVE_DOWN
                else -> FLAT
            }
        }
    }
}

data class DeviceConnectionInfo(
    val deviceType: WearableDeviceType,
    val modelName: String,
    val isConnected: Boolean,
    val batteryPercent: Int,
    val lastSyncFormatted: String,
    val isAutoSync: Boolean = true
)

data class CgmReadingPoint(
    val minuteOfDay: Int, // 0 to 1439
    val timeLabel: String, // e.g. "08:30"
    val glucoseMgDl: Double, // e.g. 104.0
    val trend: CgmTrend,
    val isHypo: Boolean = glucoseMgDl < 70.0,
    val isHyper: Boolean = glucoseMgDl > 140.0
)

data class DailyBiometricSummary(
    val dateString: String,
    val restingHeartRate: Int = 66, // bpm
    val hrvRmssd: Double = 52.0, // ms
    val sleepDurationHours: Double = 7.5, // hrs
    val deepSleepMinutes: Int = 70, // min
    val remSleepMinutes: Int = 85, // min
    val sleepEfficiencyPct: Double = 89.0, // %
    val wakeAfterSleepOnsetMinutes: Int = 22, // min
    val basalBodyTempF: Double = 97.6, // °F
    val stepCount: Int = 8450,
    val activeMinutes: Int = 38,
    // CGM
    val cgmCurrentGlucose: Double = 106.0, // mg/dL
    val cgmTrend: CgmTrend = CgmTrend.FLAT,
    val cgmMeanGlucose: Double = 104.2, // mg/dL
    val cgmTimeInRangePct: Double = 94.0, // % (70-140 mg/dL target)
    val cgmTimeBelowRangePct: Double = 1.8, // % (<70)
    val cgmTimeAboveRangePct: Double = 4.2, // % (>140)
    val cgmGlucoseCvPct: Double = 19.5, // Glycemic variability %CV
    val estimatedA1c: Double = 5.2, // %
    val sensorDaysRemaining: Int = 7,
    val wearableSource: String = "Google Pixel Watch 3 & Dexcom G7"
)

data class BiometricCycleCorrelation(
    val follicularRhrMean: Double,
    val lutealRhrMean: Double,
    val follicularHrvMean: Double,
    val lutealHrvMean: Double,
    val follicularDeepSleepMinMean: Double,
    val lutealDeepSleepMinMean: Double,
    val follicularGlucoseCvMean: Double,
    val lutealGlucoseCvMean: Double,
    val follicularBbtMean: Double,
    val lutealBbtMean: Double,
    // Percentage shifts
    val rhrLutealSurgePct: Double,
    val hrvLutealDropPct: Double,
    val deepSleepDropPct: Double,
    val glucoseCvIncreasePct: Double,
    val clinicalSummary: String
)

data class DailyBiometricTrendPoint(
    val dayIndex: Int, // 1 to 30
    val dateString: String,
    val cycleDay: Int,
    val phaseName: String, // "Menses", "Follicular", "Ovulation", "Luteal"
    val isBleeding: Boolean,
    val isOvulation: Boolean,
    val isLuteal: Boolean,
    val restingHeartRate: Double,
    val hrvRmssd: Double,
    val cgmGlucoseCvPct: Double,
    val deepSleepMinutes: Double,
    val basalBodyTempF: Double,
    val drspMeanSeverity: Float? = null
)
