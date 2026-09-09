package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drsp_daily_logs")
data class DrspDailyLog(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val cycleDay: Int = 1,
    val isBleeding: Boolean = false,
    val bleedingFlow: String = "None", // None, Spotting, Light, Medium, Heavy

    // Core DRSP Symptoms (1-6 scale: 1=Not at all, 6=Extreme)
    val depressedMood: Int = 1,
    val hopelessness: Int = 1,
    val anxietyTension: Int = 1,
    val moodSwings: Int = 1,
    val suddenTearfulness: Int = 1,
    val angerIrritability: Int = 1,
    val conflictWithOthers: Int = 1,
    val decreasedInterest: Int = 1,
    val difficultyConcentrating: Int = 1,
    val lethargyFatigue: Int = 1,
    val increasedAppetiteCravings: Int = 1,
    val hypersomniaOrInsomnia: Int = 1,
    val feelingOverwhelmed: Int = 1,
    val physicalBreastTenderness: Int = 1,
    val physicalBloatingWeightGain: Int = 1,
    val physicalHeadachesJointPain: Int = 1,

    // Impairment scores (1-6)
    val workProductivityImpairment: Int = 1,
    val socialActivitiesImpairment: Int = 1,
    val relationshipImpairment: Int = 1,

    // Biometric Continuous / Wearable telemetry values
    val restingHeartRate: Int? = 72,
    val hrvRmssd: Double? = 48.0,
    val sleepDurationHours: Double? = 7.5,
    val sleepQualityScore: Int? = 82,
    val cgmCurrentGlucose: Double? = 98.0,
    val cgmMeanDailyGlucose: Double? = 104.0,
    val cgmGlucoseVariabilityCv: Double? = 18.2,
    val cgmPostprandialSpikeAvg: Double? = 138.0,
    val notes: String = ""
) {
    val totalDrspScore: Int
        get() = depressedMood + hopelessness + anxietyTension + moodSwings +
                suddenTearfulness + angerIrritability + conflictWithOthers +
                decreasedInterest + difficultyConcentrating + lethargyFatigue +
                increasedAppetiteCravings + hypersomniaOrInsomnia + feelingOverwhelmed +
                physicalBreastTenderness + physicalBloatingWeightGain + physicalHeadachesJointPain

    val isLateLuteal: Boolean
        get() = cycleDay in 20..35 && !isBleeding

    val isFollicular: Boolean
        get() = cycleDay in 6..13
}
