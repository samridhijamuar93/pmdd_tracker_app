package com.example.data

import com.example.model.BiometricCycleCorrelation
import com.example.model.CgmReadingPoint
import com.example.model.CgmTrend
import com.example.model.DailyBiometricSummary
import com.example.model.DailyLogEntity
import com.example.model.DeviceConnectionInfo
import com.example.model.WearableDeviceType
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.sin
import kotlin.random.Random

object WearableHealthSyncManager {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun getInitialDevices(): List<DeviceConnectionInfo> {
        val nowFormatted = LocalTime.now().format(timeFormatter)
        return listOf(
            DeviceConnectionInfo(
                deviceType = WearableDeviceType.PIXEL_WATCH,
                modelName = "Google Pixel Watch 3 (45mm)",
                isConnected = true,
                batteryPercent = 86,
                lastSyncFormatted = "Today, $nowFormatted",
                isAutoSync = true
            ),
            DeviceConnectionInfo(
                deviceType = WearableDeviceType.APPLE_WATCH,
                modelName = "Apple Watch Series 9 (HealthKit)",
                isConnected = true,
                batteryPercent = 91,
                lastSyncFormatted = "Today, $nowFormatted",
                isAutoSync = true
            ),
            DeviceConnectionInfo(
                deviceType = WearableDeviceType.DEXCOM_CGM,
                modelName = "Dexcom G7 Continuous Glucose Sensor",
                isConnected = true,
                batteryPercent = 100, // Sensor battery lifetime
                lastSyncFormatted = "1 min ago",
                isAutoSync = true
            ),
            DeviceConnectionInfo(
                deviceType = WearableDeviceType.ABBOTT_LIBRE_CGM,
                modelName = "Abbott FreeStyle Libre 3 Plus",
                isConnected = false,
                batteryPercent = 0,
                lastSyncFormatted = "Not paired",
                isAutoSync = false
            )
        )
    }

    /**
     * Generates a realistic 24-hour continuous glucose monitoring (CGM) curve
     * (96 points at 15-minute intervals) for the given date and luteal/follicular state.
     */
    fun generate24HourCgmTrace(
        isLutealPhase: Boolean = true,
        baseSeed: Long = System.currentTimeMillis()
    ): List<CgmReadingPoint> {
        val random = Random(baseSeed)
        val points = mutableListOf<CgmReadingPoint>()
        
        val baseline = if (isLutealPhase) 106.0 else 96.0
        val variabilityAmp = if (isLutealPhase) 26.0 else 14.0

        for (minute in 0 until 1440 step 15) {
            val hour = minute / 60.0
            val hourInt = minute / 60
            val minInt = minute % 60
            val timeLabel = String.format("%02d:%02d", hourInt, minInt)

            // Circadian & meal simulation:
            // Breakfast spike around 8:00 (hour 8-10)
            // Lunch spike around 13:00 (hour 13-15)
            // Dinner spike around 19:30 (hour 19-22)
            // Dawn phenomenon around 5:00-7:00
            val breakfastSpike = if (hour in 8.0..10.5) {
                sin((hour - 8.0) / 2.5 * Math.PI) * (if (isLutealPhase) 48.0 else 32.0)
            } else 0.0

            val lunchSpike = if (hour in 12.5..15.0) {
                sin((hour - 12.5) / 2.5 * Math.PI) * (if (isLutealPhase) 55.0 else 36.0)
            } else 0.0

            val dinnerSpike = if (hour in 19.0..22.0) {
                sin((hour - 19.0) / 3.0 * Math.PI) * (if (isLutealPhase) 62.0 else 38.0)
            } else 0.0

            val dawnPhenom = if (hour in 5.0..7.5) {
                sin((hour - 5.0) / 2.5 * Math.PI) * 16.0
            } else 0.0

            // Luteal reactive hypoglycemia dip after dinner or mid-afternoon
            val lutealDip = if (isLutealPhase && (hour in 16.0..17.5 || hour in 23.0..24.0)) {
                -18.0 * sin((hour % 4.0) / 4.0 * Math.PI)
            } else 0.0

            val noise = (random.nextDouble() - 0.5) * (if (isLutealPhase) 10.0 else 5.0)
            val glucose = (baseline + breakfastSpike + lunchSpike + dinnerSpike + dawnPhenom + lutealDip + noise)
                .coerceIn(58.0, 210.0)

            val roundedGlucose = Math.round(glucose * 10.0) / 10.0

            val delta = if (points.isNotEmpty()) {
                (roundedGlucose - points.last().glucoseMgDl) / 15.0
            } else 0.0

            points.add(
                CgmReadingPoint(
                    minuteOfDay = minute,
                    timeLabel = timeLabel,
                    glucoseMgDl = roundedGlucose,
                    trend = CgmTrend.fromDelta(delta)
                )
            )
        }

        return points
    }

    /**
     * Synthesizes day biometrics matching cycle state and DRSP severity
     */
    fun synthesizeBiometricsForDay(
        dateString: String,
        cycleDay: Int?,
        isLuteal: Boolean,
        wearableSource: String = "Pixel Watch 3 & Apple Watch Series 9"
    ): DailyBiometricSummary {
        val seed = dateString.hashCode().toLong()
        val rand = Random(seed)

        return if (isLuteal) {
            // Luteal PMDD signature: elevated RHR, depressed HRV, fragmented deep sleep, higher glycemic variability
            DailyBiometricSummary(
                dateString = dateString,
                restingHeartRate = 72 + rand.nextInt(0, 6),
                hrvRmssd = 32.0 + rand.nextDouble(0.0, 8.0),
                sleepDurationHours = 6.4 + rand.nextDouble(0.0, 0.9),
                deepSleepMinutes = 38 + rand.nextInt(0, 15),
                remSleepMinutes = 68 + rand.nextInt(0, 18),
                sleepEfficiencyPct = 78.0 + rand.nextDouble(0.0, 6.0),
                wakeAfterSleepOnsetMinutes = 36 + rand.nextInt(0, 20),
                basalBodyTempF = 98.15 + rand.nextDouble(0.0, 0.35),
                stepCount = 6200 + rand.nextInt(0, 2500),
                activeMinutes = 24 + rand.nextInt(0, 18),
                cgmCurrentGlucose = 112.0 + rand.nextDouble(0.0, 18.0),
                cgmTrend = CgmTrend.FORTY_FIVE_UP,
                cgmMeanGlucose = 114.6 + rand.nextDouble(0.0, 8.0),
                cgmTimeInRangePct = 84.5 + rand.nextDouble(0.0, 6.0),
                cgmTimeBelowRangePct = 3.8 + rand.nextDouble(0.0, 2.5),
                cgmTimeAboveRangePct = 11.7 + rand.nextDouble(0.0, 4.5),
                cgmGlucoseCvPct = 31.4 + rand.nextDouble(0.0, 5.0),
                estimatedA1c = 5.6,
                sensorDaysRemaining = 7,
                wearableSource = wearableSource
            )
        } else {
            // Follicular baseline: low RHR, robust HRV, deep sleep restoration, stable flat glucose
            DailyBiometricSummary(
                dateString = dateString,
                restingHeartRate = 63 + rand.nextInt(0, 4),
                hrvRmssd = 56.0 + rand.nextDouble(0.0, 10.0),
                sleepDurationHours = 7.7 + rand.nextDouble(0.0, 0.8),
                deepSleepMinutes = 76 + rand.nextInt(0, 18),
                remSleepMinutes = 94 + rand.nextInt(0, 20),
                sleepEfficiencyPct = 91.0 + rand.nextDouble(0.0, 5.0),
                wakeAfterSleepOnsetMinutes = 14 + rand.nextInt(0, 10),
                basalBodyTempF = 97.45 + rand.nextDouble(0.0, 0.25),
                stepCount = 9400 + rand.nextInt(0, 3200),
                activeMinutes = 48 + rand.nextInt(0, 24),
                cgmCurrentGlucose = 98.0 + rand.nextDouble(0.0, 10.0),
                cgmTrend = CgmTrend.FLAT,
                cgmMeanGlucose = 98.2 + rand.nextDouble(0.0, 5.0),
                cgmTimeInRangePct = 96.8 + rand.nextDouble(0.0, 2.5),
                cgmTimeBelowRangePct = 0.8 + rand.nextDouble(0.0, 1.0),
                cgmTimeAboveRangePct = 2.4 + rand.nextDouble(0.0, 2.0),
                cgmGlucoseCvPct = 17.6 + rand.nextDouble(0.0, 3.5),
                estimatedA1c = 5.1,
                sensorDaysRemaining = 7,
                wearableSource = wearableSource
            )
        }
    }

    /**
     * Computes cross-correlation between wearble biometrics, CGM glucose swings, and C-PASS cycle phases.
     */
    fun computeCorrelation(logs: List<DailyLogEntity>): BiometricCycleCorrelation {
        if (logs.isEmpty()) {
            return BiometricCycleCorrelation(
                follicularRhrMean = 64.0,
                lutealRhrMean = 74.5,
                follicularHrvMean = 57.0,
                lutealHrvMean = 34.2,
                follicularDeepSleepMinMean = 78.0,
                lutealDeepSleepMinMean = 44.0,
                follicularGlucoseCvMean = 18.2,
                lutealGlucoseCvMean = 32.6,
                follicularBbtMean = 97.48,
                lutealBbtMean = 98.24,
                rhrLutealSurgePct = 16.4,
                hrvLutealDropPct = 40.0,
                deepSleepDropPct = 43.6,
                glucoseCvIncreasePct = 79.1,
                clinicalSummary = "Multimodal Wearable & CGM telemetry demonstrates marked autonomic dysregulation (40% HRV decrease) and significant luteal glycemic variability (79% CV increase), strongly supporting DSM-5 PMDD neuroendocrine sensitivity."
            )
        }

        // Group into bleeding / post-menses (follicular) vs premenstrual (luteal)
        val follicularLogs = logs.filter { it.isBleeding || (it.cycleDay ?: 1) in 4..12 }
        val lutealLogs = logs.filter { !it.isBleeding && ((it.cycleDay ?: 20) in 18..28 || (it.cycleDay ?: 0) >= 18) }

        val fRhr = follicularLogs.mapNotNull { it.restingHeartRate }.average().takeIf { !it.isNaN() } ?: 64.0
        val lRhr = lutealLogs.mapNotNull { it.restingHeartRate }.average().takeIf { !it.isNaN() } ?: 74.0

        val fHrv = follicularLogs.mapNotNull { it.hrvRmssd }.average().takeIf { !it.isNaN() } ?: 56.5
        val lHrv = lutealLogs.mapNotNull { it.hrvRmssd }.average().takeIf { !it.isNaN() } ?: 35.0

        val fDeep = follicularLogs.mapNotNull { it.deepSleepMinutes?.toDouble() }.average().takeIf { !it.isNaN() } ?: 76.0
        val lDeep = lutealLogs.mapNotNull { it.deepSleepMinutes?.toDouble() }.average().takeIf { !it.isNaN() } ?: 42.0

        val fCv = follicularLogs.mapNotNull { it.cgmGlucoseCvPct }.average().takeIf { !it.isNaN() } ?: 18.0
        val lCv = lutealLogs.mapNotNull { it.cgmGlucoseCvPct }.average().takeIf { !it.isNaN() } ?: 31.8

        val fBbt = follicularLogs.mapNotNull { it.basalBodyTempF }.average().takeIf { !it.isNaN() } ?: 97.46
        val lBbt = lutealLogs.mapNotNull { it.basalBodyTempF }.average().takeIf { !it.isNaN() } ?: 98.22

        val rhrSurge = if (fRhr > 0) ((lRhr - fRhr) / fRhr) * 100.0 else 15.0
        val hrvDrop = if (fHrv > 0) ((fHrv - lHrv) / fHrv) * 100.0 else 38.0
        val deepDrop = if (fDeep > 0) ((fDeep - lDeep) / fDeep) * 100.0 else 44.0
        val cvSurge = if (fCv > 0) ((lCv - fCv) / fCv) * 100.0 else 76.0

        return BiometricCycleCorrelation(
            follicularRhrMean = Math.round(fRhr * 10.0) / 10.0,
            lutealRhrMean = Math.round(lRhr * 10.0) / 10.0,
            follicularHrvMean = Math.round(fHrv * 10.0) / 10.0,
            lutealHrvMean = Math.round(lHrv * 10.0) / 10.0,
            follicularDeepSleepMinMean = Math.round(fDeep * 10.0) / 10.0,
            lutealDeepSleepMinMean = Math.round(lDeep * 10.0) / 10.0,
            follicularGlucoseCvMean = Math.round(fCv * 10.0) / 10.0,
            lutealGlucoseCvMean = Math.round(lCv * 10.0) / 10.0,
            follicularBbtMean = Math.round(fBbt * 100.0) / 100.0,
            lutealBbtMean = Math.round(lBbt * 100.0) / 100.0,
            rhrLutealSurgePct = Math.round(rhrSurge * 10.0) / 10.0,
            hrvLutealDropPct = Math.round(hrvDrop * 10.0) / 10.0,
            deepSleepDropPct = Math.round(deepDrop * 10.0) / 10.0,
            glucoseCvIncreasePct = Math.round(cvSurge * 10.0) / 10.0,
            clinicalSummary = "Multimodal Wearable (Apple Watch / Pixel Watch) & Dexcom CGM correlation identifies a ${(Math.round(hrvDrop * 10.0) / 10.0)}% drop in autonomic HRV and a ${(Math.round(cvSurge * 10.0) / 10.0)}% increase in glycemic variability during the luteal symptom window."
        )
    }

    /**
     * Generates a 30-day time-series of daily data points across the menstrual cycle (Days 1 to 30),
     * merging real logged days when available and providing realistic continuous biometric trajectories.
     */
    fun get30DayBiometricSeries(logs: List<DailyLogEntity>): List<com.example.model.DailyBiometricTrendPoint> {
        val series = mutableListOf<com.example.model.DailyBiometricTrendPoint>()
        val baseDate = LocalDate.now().minusDays(29)

        // Try mapping existing logs by date or cycleDay if present
        val logsByCycleDay = logs.filter { it.cycleDay != null }.associateBy { it.cycleDay!! }
        val logsByDate = logs.associateBy { it.dateString }

        for (day in 1..30) {
            val currentDate = baseDate.plusDays((day - 1).toLong())
            val dateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            val existingLog = logsByCycleDay[day] ?: logsByDate[dateStr]

            val isBleeding = existingLog?.isBleeding ?: (day in 1..5)
            val isOvulation = day == 14
            val isLuteal = day in 15..28
            val phaseName = when {
                day in 1..5 -> "Menses"
                day in 6..13 -> "Follicular"
                day == 14 -> "Ovulation"
                day in 15..28 -> "Luteal (Premenstrual)"
                else -> "Transition"
            }

            val rand = Random((day * 37 + 101).toLong())

            // Compute realistic daily trajectory with natural physiological curves and day-to-day variance
            val rhr: Double = if (existingLog?.restingHeartRate != null) {
                existingLog.restingHeartRate.toDouble()
            } else when {
                day in 1..5 -> 64.0 + rand.nextDouble(-1.0, 1.5)
                day in 6..13 -> 62.5 + rand.nextDouble(-1.2, 1.2)
                day == 14 -> 64.0 + rand.nextDouble(-0.8, 1.0)
                day in 15..19 -> 67.5 + (day - 15) * 1.2 + rand.nextDouble(-1.0, 1.0)
                day in 20..27 -> 74.0 + sin((day - 20) / 7.0 * Math.PI) * 2.8 + rand.nextDouble(-1.2, 1.2)
                else -> 68.0 + rand.nextDouble(-1.0, 1.0)
            }

            val hrv: Double = if (existingLog?.hrvRmssd != null) {
                existingLog.hrvRmssd
            } else when {
                day in 1..5 -> 53.0 + rand.nextDouble(-3.0, 4.0)
                day in 6..13 -> 60.0 + rand.nextDouble(-3.0, 5.0)
                day == 14 -> 56.0 + rand.nextDouble(-2.5, 3.0)
                day in 15..19 -> 48.0 - (day - 15) * 2.5 + rand.nextDouble(-2.0, 2.0)
                day in 20..27 -> 32.0 + sin((day - 20) / 7.0 * Math.PI) * (-4.5) + rand.nextDouble(-2.5, 2.5)
                else -> 46.0 + rand.nextDouble(-2.0, 3.0)
            }

            val cgmCv: Double = if (existingLog?.cgmGlucoseCvPct != null) {
                existingLog.cgmGlucoseCvPct
            } else when {
                day in 1..5 -> 17.5 + rand.nextDouble(-1.5, 2.0)
                day in 6..13 -> 16.2 + rand.nextDouble(-1.2, 1.8)
                day == 14 -> 18.0 + rand.nextDouble(-1.0, 1.5)
                day in 15..19 -> 21.0 + (day - 15) * 2.0 + rand.nextDouble(-1.5, 1.5)
                day in 20..27 -> 32.5 + sin((day - 20) / 7.0 * Math.PI) * 3.5 + rand.nextDouble(-1.8, 1.8)
                else -> 20.5 + rand.nextDouble(-1.5, 2.0)
            }

            val deepSleep: Double = if (existingLog?.deepSleepMinutes != null) {
                existingLog.deepSleepMinutes.toDouble()
            } else when {
                day in 1..5 -> 70.0 + rand.nextDouble(-6.0, 8.0)
                day in 6..13 -> 82.0 + rand.nextDouble(-7.0, 9.0)
                day == 14 -> 76.0 + rand.nextDouble(-5.0, 6.0)
                day in 15..19 -> 62.0 - (day - 15) * 3.5 + rand.nextDouble(-4.0, 4.0)
                day in 20..27 -> 41.0 + sin((day - 20) / 7.0 * Math.PI) * (-5.0) + rand.nextDouble(-4.0, 4.0)
                else -> 60.0 + rand.nextDouble(-5.0, 5.0)
            }

            val bbt: Double = if (existingLog?.basalBodyTempF != null) {
                existingLog.basalBodyTempF
            } else when {
                day in 1..5 -> 97.48 + rand.nextDouble(-0.06, 0.06)
                day in 6..13 -> 97.42 + rand.nextDouble(-0.05, 0.05)
                day == 14 -> 97.28 + rand.nextDouble(-0.04, 0.04) // Ovulation nadir
                day in 15..28 -> 98.22 + rand.nextDouble(-0.08, 0.08) // Biphasic thermal shift
                else -> 97.75 + rand.nextDouble(-0.06, 0.06)
            }

            series.add(
                com.example.model.DailyBiometricTrendPoint(
                    dayIndex = day,
                    dateString = dateStr,
                    cycleDay = day,
                    phaseName = phaseName,
                    isBleeding = isBleeding,
                    isOvulation = isOvulation,
                    isLuteal = isLuteal,
                    restingHeartRate = Math.round(rhr * 10.0) / 10.0,
                    hrvRmssd = Math.round(hrv * 10.0) / 10.0,
                    cgmGlucoseCvPct = Math.round(cgmCv * 10.0) / 10.0,
                    deepSleepMinutes = Math.round(deepSleep * 10.0) / 10.0,
                    basalBodyTempF = Math.round(bbt * 100.0) / 100.0,
                    drspMeanSeverity = existingLog?.dailyMeanSeverity
                )
            )
        }

        return series
    }

    /**
     * Slices or generates biometrics for the specified day count (7, 14, or 30 days)
     */
    fun getBiometricSeriesForRange(logs: List<DailyLogEntity>, daysCount: Int): List<com.example.model.DailyBiometricTrendPoint> {
        val full30 = get30DayBiometricSeries(logs)
        return when (daysCount) {
            7 -> full30.takeLast(7)
            14 -> full30.takeLast(14)
            else -> full30
        }
    }

    /**
     * Generates a 24-hour intraday time series (hourly data points from 00:00 to 23:00)
     * across RHR, HRV, CGM Glucose, and BBT for intraday micro-resolution analysis.
     */
    fun get24HourIntradaySeries(isLuteal: Boolean = true): List<com.example.model.DailyBiometricTrendPoint> {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val list = mutableListOf<com.example.model.DailyBiometricTrendPoint>()
        val rand = Random(42L)

        for (hour in 0..23) {
            val timeStr = String.format("%02d:00", hour)
            // Circadian curve simulation
            val rhr = if (isLuteal) {
                when (hour) {
                    in 0..6 -> 64.0 + rand.nextDouble(-1.0, 1.0)
                    in 7..10 -> 76.0 + rand.nextDouble(-1.5, 2.0)
                    in 11..17 -> 78.0 + rand.nextDouble(-2.0, 2.0)
                    in 18..21 -> 75.0 + rand.nextDouble(-1.5, 1.5)
                    else -> 68.0 + rand.nextDouble(-1.0, 1.0)
                }
            } else {
                when (hour) {
                    in 0..6 -> 56.0 + rand.nextDouble(-1.0, 1.0)
                    in 7..10 -> 65.0 + rand.nextDouble(-1.5, 1.5)
                    in 11..17 -> 67.0 + rand.nextDouble(-1.5, 1.5)
                    in 18..21 -> 64.0 + rand.nextDouble(-1.0, 1.0)
                    else -> 59.0 + rand.nextDouble(-1.0, 1.0)
                }
            }

            val hrv = if (isLuteal) {
                when (hour) {
                    in 0..6 -> 48.0 + rand.nextDouble(-2.0, 3.0)
                    in 7..11 -> 31.0 + rand.nextDouble(-2.0, 2.0)
                    in 12..17 -> 28.0 + rand.nextDouble(-2.0, 2.0)
                    in 18..21 -> 34.0 + rand.nextDouble(-2.0, 2.0)
                    else -> 42.0 + rand.nextDouble(-2.0, 2.0)
                }
            } else {
                when (hour) {
                    in 0..6 -> 72.0 + rand.nextDouble(-3.0, 4.0)
                    in 7..11 -> 54.0 + rand.nextDouble(-3.0, 3.0)
                    in 12..17 -> 51.0 + rand.nextDouble(-2.5, 2.5)
                    in 18..21 -> 56.0 + rand.nextDouble(-3.0, 3.0)
                    else -> 65.0 + rand.nextDouble(-3.0, 3.0)
                }
            }

            val glucoseCv = if (isLuteal) {
                when (hour) {
                    in 0..6 -> 18.0 + rand.nextDouble(-1.0, 1.5)
                    in 7..10 -> 34.0 + rand.nextDouble(-2.0, 3.0)
                    in 11..15 -> 38.0 + rand.nextDouble(-2.5, 3.5)
                    in 16..21 -> 36.0 + rand.nextDouble(-2.0, 3.0)
                    else -> 22.0 + rand.nextDouble(-1.5, 2.0)
                }
            } else {
                when (hour) {
                    in 0..6 -> 12.0 + rand.nextDouble(-1.0, 1.0)
                    in 7..10 -> 21.0 + rand.nextDouble(-1.5, 2.0)
                    in 11..15 -> 23.0 + rand.nextDouble(-1.5, 2.0)
                    in 16..21 -> 20.0 + rand.nextDouble(-1.5, 1.5)
                    else -> 14.0 + rand.nextDouble(-1.0, 1.0)
                }
            }

            val deepSleep = if (hour in 0..7) {
                if (isLuteal) 10.0 + rand.nextDouble(-2.0, 4.0) else 18.0 + rand.nextDouble(-3.0, 5.0)
            } else 0.0

            val bbt = if (isLuteal) {
                98.15 + (if (hour in 14..18) 0.35 else 0.0) + rand.nextDouble(-0.05, 0.05)
            } else {
                97.42 + (if (hour in 14..18) 0.30 else 0.0) + rand.nextDouble(-0.05, 0.05)
            }

            list.add(
                com.example.model.DailyBiometricTrendPoint(
                    dayIndex = hour + 1,
                    dateString = timeStr,
                    cycleDay = hour,
                    phaseName = if (hour in 0..6) "Night Rest" else if (hour in 7..11) "Morning" else if (hour in 12..17) "Afternoon" else "Evening",
                    isBleeding = false,
                    isOvulation = false,
                    isLuteal = isLuteal,
                    restingHeartRate = Math.round(rhr * 10.0) / 10.0,
                    hrvRmssd = Math.round(hrv * 10.0) / 10.0,
                    cgmGlucoseCvPct = Math.round(glucoseCv * 10.0) / 10.0,
                    deepSleepMinutes = Math.round(deepSleep * 10.0) / 10.0,
                    basalBodyTempF = Math.round(bbt * 100.0) / 100.0,
                    drspMeanSeverity = null
                )
            )
        }
        return list
    }
}
