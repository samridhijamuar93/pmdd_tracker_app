package com.example.data

import com.example.model.DailyLogEntity
import com.example.model.PeriodFlow
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class SymptomRepository(private val dailyLogDao: DailyLogDao) {

    val allLogs: Flow<List<DailyLogEntity>> = dailyLogDao.getAllLogsFlow()

    fun getLogByDateFlow(dateString: String): Flow<DailyLogEntity?> =
        dailyLogDao.getLogByDateFlow(dateString)

    suspend fun getLogByDate(dateString: String): DailyLogEntity? =
        dailyLogDao.getLogByDate(dateString)

    suspend fun saveLog(log: DailyLogEntity) {
        dailyLogDao.insertLog(log)
    }

    suspend fun deleteLog(dateString: String) {
        dailyLogDao.deleteLogByDate(dateString)
    }

    suspend fun clearAll() {
        dailyLogDao.clearAll()
    }

    /**
     * Seeds realistic 2-cycle prospective DRSP tracking data (56 days, two 28-day cycles)
     * strictly satisfying C-PASS PMDD criteria:
     * - Premenstrual week (Days 22-28, relative -7 to -1): scores 4-6 on >= 5 DSM-5 domains including core affective.
     * - Postmenstrual week (Days 4-10, relative +4 to +10): absolute clearance with scores 1-2 (<= 3).
     */
    suspend fun loadDemoProspectiveCpassCycles() {
        dailyLogDao.clearAll()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val generatedLogs = mutableListOf<DailyLogEntity>()

        val startDate = today.minusDays(56)

        for (dayOffset in 0..56) {
            val date = startDate.plusDays(dayOffset.toLong())
            val dateStr = date.format(dateFormatter)
            val cycleDay = (dayOffset % 28) + 1 // Day 1 to 28

            val periodFlow: PeriodFlow = when (cycleDay) {
                1 -> PeriodFlow.MEDIUM
                2 -> PeriodFlow.HEAVY
                3 -> PeriodFlow.MEDIUM
                4 -> PeriodFlow.LIGHT
                5 -> PeriodFlow.SPOTTING
                else -> PeriodFlow.NONE
            }

            val isPremenstrualWeek = cycleDay in 22..28 // Days -7 to -1 before next menses
            val isMensesOnset = cycleDay in 1..3
            val isPostmenstrualWeek = cycleDay in 4..10 // C-PASS postmenstrual clearance window (+4 to +10)

            val isLuteal = isPremenstrualWeek || cycleDay >= 19
            val biometrics = WearableHealthSyncManager.synthesizeBiometricsForDay(
                dateString = dateStr,
                cycleDay = cycleDay,
                isLuteal = isLuteal
            )

            val log = when {
                isPremenstrualWeek -> {
                    // Late luteal premenstrual phase: High elevation (4-6 on 1-6 DRSP scale)
                    val base = when (cycleDay) {
                        22 -> 4
                        23, 24 -> 5
                        25, 26 -> 5
                        else -> 6
                    }
                    DailyLogEntity(
                        dateString = dateStr,
                        timestamp = System.currentTimeMillis(),
                        periodFlow = periodFlow.name,
                        cycleDay = cycleDay,
                        // Affective Core (Criteria B)
                        drsp1_depressed = (base - 1 + Random.nextInt(0, 2)).coerceIn(4, 6),
                        drsp2_hopeless = (base - 1 + Random.nextInt(0, 2)).coerceIn(4, 5),
                        drsp3_worthless = (base - 2 + Random.nextInt(0, 2)).coerceIn(3, 5),
                        drsp4_anxious = (base + Random.nextInt(0, 2)).coerceIn(4, 6),
                        drsp5_mood_swings = (base + Random.nextInt(0, 2)).coerceIn(5, 6),
                        drsp6_rejection_sensitive = (base + Random.nextInt(0, 2)).coerceIn(4, 6),
                        drsp7_angry_irritable = (base + Random.nextInt(0, 2)).coerceIn(5, 6),
                        drsp8_conflicts = (base - 1 + Random.nextInt(0, 2)).coerceIn(4, 5),
                        // Additional Cognitive / Somatic (Criteria C)
                        drsp9_decreased_interest = (base - 1 + Random.nextInt(0, 2)).coerceIn(4, 5),
                        drsp10_social_withdrawal = (base + Random.nextInt(0, 2)).coerceIn(4, 5),
                        drsp11_concentration = (base + Random.nextInt(0, 2)).coerceIn(4, 6),
                        drsp12_tired_low_energy = (base + Random.nextInt(0, 2)).coerceIn(5, 6),
                        drsp13_appetite_increase = (base + Random.nextInt(0, 2)).coerceIn(4, 6),
                        drsp14_food_cravings = (base + 1).coerceIn(4, 6),
                        drsp15_insomnia = (base + Random.nextInt(0, 2)).coerceIn(4, 5),
                        drsp16_hypersomnia = (base - 1).coerceIn(3, 5),
                        drsp17_overwhelmed = (base + Random.nextInt(0, 2)).coerceIn(4, 6),
                        drsp18_out_of_control = (base + Random.nextInt(0, 2)).coerceIn(4, 6),
                        drsp19_breast_tenderness = (base + Random.nextInt(0, 2)).coerceIn(4, 6),
                        drsp20_headaches_body_aches = (base - 1 + Random.nextInt(0, 2)).coerceIn(3, 5),
                        drsp21_bloating_weight = (base + Random.nextInt(0, 2)).coerceIn(4, 6),
                        // Impairments
                        drsp22_impairment_work = (base - 1 + Random.nextInt(0, 2)).coerceIn(4, 5),
                        drsp23_impairment_relationships = (base + Random.nextInt(0, 2)).coerceIn(4, 6),
                        drsp24_impairment_social = (base + Random.nextInt(0, 2)).coerceIn(4, 5),
                        notes = if (cycleDay >= 25) "Severe emotional volatility, brain fog, acute breast pain & exhaustion." else "Irritable, crying spells without obvious trigger.",
                        tookMedication = false,
                        somaticTags = "Breast Tenderness,Bloating,Fatigue,Brain Fog",
                        // Biometrics
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
                        wearableSource = "Apple Watch Series 9 & Dexcom G7"
                    )
                }
                isMensesOnset -> {
                    // Menses onset: Symptoms rapidly plummeting
                    val drop = (4 - cycleDay).coerceAtLeast(1)
                    DailyLogEntity(
                        dateString = dateStr,
                        timestamp = System.currentTimeMillis(),
                        periodFlow = periodFlow.name,
                        cycleDay = cycleDay,
                        drsp1_depressed = 2,
                        drsp2_hopeless = 1,
                        drsp3_worthless = 1,
                        drsp4_anxious = 2,
                        drsp5_mood_swings = 2,
                        drsp6_rejection_sensitive = 2,
                        drsp7_angry_irritable = 2,
                        drsp8_conflicts = 1,
                        drsp9_decreased_interest = 2,
                        drsp10_social_withdrawal = 1,
                        drsp11_concentration = 2,
                        drsp12_tired_low_energy = 3,
                        drsp13_appetite_increase = 2,
                        drsp14_food_cravings = 2,
                        drsp15_insomnia = 1,
                        drsp16_hypersomnia = 2,
                        drsp17_overwhelmed = 1,
                        drsp18_out_of_control = 1,
                        drsp19_breast_tenderness = 2,
                        drsp20_headaches_body_aches = 3,
                        drsp21_bloating_weight = 2,
                        drsp22_impairment_work = 2,
                        drsp23_impairment_relationships = 1,
                        drsp24_impairment_social = 1,
                        notes = "Bleeding began. Significant relief in mood tension.",
                        tookMedication = cycleDay <= 2,
                        medicationDetails = if (cycleDay <= 2) "Ibuprofen 400mg" else "",
                        somaticTags = "Cramps",
                        // Biometrics
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
                        wearableSource = "Google Pixel Watch 3 & Dexcom G7"
                    )
                }
                isPostmenstrualWeek -> {
                    // C-PASS Postmenstrual Clearance Window (Days +4 to +10): Absolute remission (1-2, all <= 3)
                    DailyLogEntity(
                        dateString = dateStr,
                        timestamp = System.currentTimeMillis(),
                        periodFlow = periodFlow.name,
                        cycleDay = cycleDay,
                        drsp1_depressed = 1,
                        drsp2_hopeless = 1,
                        drsp3_worthless = 1,
                        drsp4_anxious = if (Random.nextFloat() > 0.8f) 2 else 1,
                        drsp5_mood_swings = 1,
                        drsp6_rejection_sensitive = 1,
                        drsp7_angry_irritable = 1,
                        drsp8_conflicts = 1,
                        drsp9_decreased_interest = 1,
                        drsp10_social_withdrawal = 1,
                        drsp11_concentration = 1,
                        drsp12_tired_low_energy = if (Random.nextFloat() > 0.7f) 2 else 1,
                        drsp13_appetite_increase = 1,
                        drsp14_food_cravings = 1,
                        drsp15_insomnia = 1,
                        drsp16_hypersomnia = 1,
                        drsp17_overwhelmed = 1,
                        drsp18_out_of_control = 1,
                        drsp19_breast_tenderness = 1,
                        drsp20_headaches_body_aches = 1,
                        drsp21_bloating_weight = 1,
                        drsp22_impairment_work = 1,
                        drsp23_impairment_relationships = 1,
                        drsp24_impairment_social = 1,
                        notes = "Feeling calm, energized, clear-minded.",
                        tookMedication = false,
                        // Biometrics
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
                        wearableSource = "Apple Watch Series 9 & Dexcom G7"
                    )
                }
                else -> {
                    // Mid-cycle / Ovulation (Days 11-21): Normal baseline (1-2)
                    DailyLogEntity(
                        dateString = dateStr,
                        timestamp = System.currentTimeMillis(),
                        periodFlow = periodFlow.name,
                        cycleDay = cycleDay,
                        drsp1_depressed = 1,
                        drsp2_hopeless = 1,
                        drsp3_worthless = 1,
                        drsp4_anxious = if (Random.nextFloat() > 0.7f) 2 else 1,
                        drsp5_mood_swings = 1,
                        drsp6_rejection_sensitive = 1,
                        drsp7_angry_irritable = if (Random.nextFloat() > 0.7f) 2 else 1,
                        drsp8_conflicts = 1,
                        drsp9_decreased_interest = 1,
                        drsp10_social_withdrawal = 1,
                        drsp11_concentration = 1,
                        drsp12_tired_low_energy = if (Random.nextFloat() > 0.6f) 2 else 1,
                        drsp13_appetite_increase = if (Random.nextFloat() > 0.7f) 2 else 1,
                        drsp14_food_cravings = 1,
                        drsp15_insomnia = 1,
                        drsp16_hypersomnia = 1,
                        drsp17_overwhelmed = 1,
                        drsp18_out_of_control = 1,
                        drsp19_breast_tenderness = if (cycleDay in 14..16) 2 else 1,
                        drsp20_headaches_body_aches = 1,
                        drsp21_bloating_weight = 1,
                        drsp22_impairment_work = 1,
                        drsp23_impairment_relationships = 1,
                        drsp24_impairment_social = 1,
                        notes = if (cycleDay == 14) "Ovulation day - feeling good." else "",
                        tookMedication = false,
                        // Biometrics
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
                        wearableSource = "Google Pixel Watch 3 & Dexcom G7"
                    )
                }
            }
            generatedLogs.add(log)
        }

        dailyLogDao.insertAll(generatedLogs)
    }
}
