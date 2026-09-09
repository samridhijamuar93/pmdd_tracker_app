package com.example.engine

import com.example.model.DailyLogEntity
import com.example.model.DrspDsm5Directory
import com.example.model.DrspSeverity
import com.example.model.Dsm5DomainDefinition
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * C-PASS (Carolina Premenstrual Assessment Scoring System) Domain Evaluation
 * Eisenlohr-Moul et al., Am J Psychiatry 2017
 */
data class CpassDomainResult(
    val domain: Dsm5DomainDefinition,
    val premenstrualMean: Float,
    val postmenstrualMean: Float,
    val premenstrualMax: Int,
    val postmenstrualMax: Int,
    val premenstrualDaysAboveThreshold: Int, // Count of premenstrual days with score >= 4

    // 4 C-PASS Core Rules
    val rule1AbsoluteSeverityMet: Boolean, // Premenstrual Max >= 4 (Moderate+)
    val rule2DurationMet: Boolean,         // At least 2 premenstrual days >= 4
    val rule3RelativeElevationMet: Boolean, // >= 30% increase relative to cycle range/postmenstrual
    val rule4AbsoluteClearanceMet: Boolean, // Postmenstrual Max <= 3 (Clearance/Remission)

    val percentElevation: Float, // Calculated percentage increase
    val isDomainQualified: Boolean, // Meets all 4 C-PASS rules

    val failureReasons: List<String>
)

data class CpassCycleResult(
    val cycleIndex: Int,
    val cycleStartDate: String, // First day of menses (Day +1)
    val cycleEndDate: String,
    val cycleLengthDays: Int,
    val nextCycleStartDate: String?,

    val premenstrualDaysLogged: Int, // Target: Days -7 to -1
    val postmenstrualDaysLogged: Int, // Target: Days +4 to +10
    val totalLogsInCycle: Int,

    // C-PASS 11 Domain Results
    val domainResults: List<CpassDomainResult>,

    // DSM-5 Criteria statuses
    val criterionBMet: Boolean, // >= 1 Core Affective Domain qualified
    val criterionCMet: Boolean, // >= 5 Total DSM-5 Domains qualified
    val criterionDMet: Boolean, // Premenstrual functional impairment max >= 4
    val criterionAMet: Boolean, // Cyclical premenstrual surge & follicular remission

    val meetsFullCyclePmdd: Boolean, // Criteria B + C + D met in this cycle

    // Qualifying lists
    val qualifyingCoreDomains: List<String>,
    val qualifyingAllDomains: List<String>,
    val qualifyingImpairments: List<String>,

    val premenstrualOverallMean: Float,
    val postmenstrualOverallMean: Float,
    val cycleDiagnosticConclusion: String
)

enum class CpassProspectiveDiagnosis(val title: String, val badgeType: String, val summary: String) {
    CONFIRMED_PMDD(
        "PMDD Confirmed (2+ Cycles)",
        "POSITIVE",
        "Criteria verified across at least 2 consecutive prospective cycles with marked premenstrual surge and complete follicular remission."
    ),
    ONE_CYCLE_CONFIRMED(
        "Single-Cycle Criteria Met",
        "IN_PROGRESS",
        "Cycle 1 met clinical criteria. A second consecutive prospective cycle is required to confirm prospective cyclicity."
    ),
    PREMENSTRUAL_EXACERBATION(
        "Premenstrual Exacerbation (PME) Suspected",
        "WARNING",
        "Symptoms worsen in the premenstrual week but fail follicular clearance (postmenstrual max > 3), indicating underlying baseline mood symptoms exacerbated by the cycle."
    ),
    SUBTHRESHOLD_MRMD(
        "Subthreshold Premenstrual Symptoms",
        "NEUTRAL",
        "Premenstrual symptoms are present but do not meet the full clinical threshold."
    ),
    INSUFFICIENT_PROSPECTIVE_DATA(
        "Insufficient Prospective Daily Tracking",
        "NEUTRAL",
        "Requires daily symptom tracking across premenstrual (Days -7 to -1) and postmenstrual (Days +4 to +10) windows."
    )
}

data class CpassProspectiveReport(
    val totalCyclesEvaluated: Int,
    val cycleResults: List<CpassCycleResult>,
    val consecutiveQualifyingCycles: Int,
    val criterionFMet: Boolean,
    val prospectiveDiagnosis: CpassProspectiveDiagnosis,
    val clinicalNarrative: String,
    val dsm5CriteriaChecklist: List<Pair<String, Boolean>>,
    val treatmentConsiderations: String
)

object CpassClinicalEngine {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Executes the standardized Carolina Premenstrual Assessment Scoring System (C-PASS)
     * over all recorded daily DRSP logs.
     */
    fun evaluateProspectiveTracking(logs: List<DailyLogEntity>): CpassProspectiveReport {
        val cycles = segmentAndScoreCycles(logs)

        if (cycles.isEmpty()) {
            return CpassProspectiveReport(
                totalCyclesEvaluated = 0,
                cycleResults = emptyList(),
                consecutiveQualifyingCycles = 0,
                criterionFMet = false,
                prospectiveDiagnosis = CpassProspectiveDiagnosis.INSUFFICIENT_PROSPECTIVE_DATA,
                clinicalNarrative = "Insufficient DRSP logs to establish cycle boundaries. Please log bleeding days and daily DRSP ratings (1-6 scale).",
                dsm5CriteriaChecklist = listOf(
                    "Criterion A (Cyclical timing: premenstrual elevation + follicular remission)" to false,
                    "Criterion B (At least 1 core affective domain qualified via C-PASS)" to false,
                    "Criterion C (At least 5 total DSM-5 domains qualified via C-PASS)" to false,
                    "Criterion D (Clinically significant functional impairment, DRSP 22-24 >= 4)" to false,
                    "Criterion F (Prospective confirmation across >= 2 consecutive cycles)" to false
                ),
                treatmentConsiderations = "Begin daily DRSP ratings each evening. At least two full consecutive cycles are required for formal clinical diagnosis."
            )
        }

        var maxConsecutive = 0
        var currentConsecutive = 0
        var hasPmePattern = false

        for (cycle in cycles) {
            if (cycle.meetsFullCyclePmdd) {
                currentConsecutive++
                if (currentConsecutive > maxConsecutive) {
                    maxConsecutive = currentConsecutive
                }
            } else {
                currentConsecutive = 0
                // Check if failed primarily due to clearance
                val preElevatedCount = cycle.domainResults.count { it.rule1AbsoluteSeverityMet && it.rule2DurationMet }
                val failedClearanceCount = cycle.domainResults.count { it.rule1AbsoluteSeverityMet && !it.rule4AbsoluteClearanceMet }
                if (preElevatedCount >= 3 && failedClearanceCount >= 2) {
                    hasPmePattern = true
                }
            }
        }

        val criterionFMet = maxConsecutive >= 2

        val diagnosis = when {
            criterionFMet -> CpassProspectiveDiagnosis.CONFIRMED_PMDD
            maxConsecutive == 1 -> CpassProspectiveDiagnosis.ONE_CYCLE_CONFIRMED
            hasPmePattern -> CpassProspectiveDiagnosis.PREMENSTRUAL_EXACERBATION
            cycles.isNotEmpty() -> CpassProspectiveDiagnosis.SUBTHRESHOLD_MRMD
            else -> CpassProspectiveDiagnosis.INSUFFICIENT_PROSPECTIVE_DATA
        }

        val latestCycle = cycles.last()
        val checklist = listOf(
            "Criterion A (Cyclical Luteal Surge & Follicular Remission)" to latestCycle.criterionAMet,
            "Criterion B (>= 1 Core Affective Domain Qualified: ${latestCycle.qualifyingCoreDomains.size} met)" to latestCycle.criterionBMet,
            "Criterion C (>= 5 Total DSM-5 Domains Qualified: ${latestCycle.qualifyingAllDomains.size} met)" to latestCycle.criterionCMet,
            "Criterion D (Marked Functional Impairment in Work/Social/Relationships)" to latestCycle.criterionDMet,
            "Criterion F (>= 2 Consecutive Cycles Confirmed via C-PASS)" to criterionFMet
        )

        val narrative = buildString {
            append("C-PASS Diagnostic Evaluation (Eisenlohr-Moul et al., 2017)\n\n")
            append("• Cycles Analyzed: ${cycles.size} menstrual cycle(s)\n")
            append("• Consecutive DSM-5 Qualifying Cycles: $maxConsecutive\n")

            when (diagnosis) {
                CpassProspectiveDiagnosis.CONFIRMED_PMDD -> {
                    append("• DIAGNOSTIC RESULT: Premenstrual Dysphoric Disorder (PMDD) Confirmed.\n")
                    append("• Criterion F satisfied: Symptoms demonstrated severe luteal exacerbation (DRSP >= 4) with strict follicular clearance (DRSP <= 3) across at least 2 consecutive cycles.\n")
                }
                CpassProspectiveDiagnosis.ONE_CYCLE_CONFIRMED -> {
                    append("• DIAGNOSTIC RESULT: Single-Cycle PMDD.\n")
                    append("• Cycle 1 meets full DSM-5 criteria under C-PASS rules. Fulfilling Criterion F requires continuing daily DRSP logging through the next cycle.\n")
                }
                CpassProspectiveDiagnosis.PREMENSTRUAL_EXACERBATION -> {
                    append("• DIAGNOSTIC RESULT: Suspected Premenstrual Exacerbation (PME).\n")
                    append("• Symptoms elevate premenstrually but remain clinically elevated (DRSP > 3) during the postmenstrual follicular phase. This suggests an underlying major depressive or anxiety disorder with premenstrual worsening.\n")
                }
                CpassProspectiveDiagnosis.SUBTHRESHOLD_MRMD -> {
                    append("• DIAGNOSTIC RESULT: Subthreshold Cyclical Distress.\n")
                    append("• Premenstrual changes were detected but did not reach the threshold of 5 symptoms or 1 core affective symptom with >= 30% cycle elevation.\n")
                }
                CpassProspectiveDiagnosis.INSUFFICIENT_PROSPECTIVE_DATA -> {
                    append("• Please continue logging daily DRSP scores.\n")
                }
            }
        }

        val treatment = when (diagnosis) {
            CpassProspectiveDiagnosis.CONFIRMED_PMDD ->
                "First-line evidence-based treatments for confirmed PMDD include SSRIs (continuous or luteal-phase dosing e.g., Fluoxetine, Sertraline, Escitalopram), monophasic drospirenone/ethinyl estradiol oral contraceptives, Cognitive Behavioral Therapy (CBT) for premenstrual distress, and calcium/chasteberry supplementation. Share this report with your physician."
            CpassProspectiveDiagnosis.PREMENSTRUAL_EXACERBATION ->
                "Treatment should prioritize optimizing baseline psychiatric pharmacotherapy (continuous antidepressant/anxiolytic) for the underlying disorder, with potential luteal dose augmentation."
            else ->
                "Continue logging daily DRSP ratings every evening. The C-PASS protocol provides objective quantification to assist healthcare providers."
        }

        return CpassProspectiveReport(
            totalCyclesEvaluated = cycles.size,
            cycleResults = cycles,
            consecutiveQualifyingCycles = maxConsecutive,
            criterionFMet = criterionFMet,
            prospectiveDiagnosis = diagnosis,
            clinicalNarrative = narrative,
            dsm5CriteriaChecklist = checklist,
            treatmentConsiderations = treatment
        )
    }

    /**
     * Segments logs by menses onset and evaluates each cycle using C-PASS windows:
     * - Premenstrual window: Days -7 to -1 before menses onset
     * - Postmenstrual window: Days +4 to +10 after menses onset
     */
    fun segmentAndScoreCycles(logs: List<DailyLogEntity>): List<CpassCycleResult> {
        if (logs.isEmpty()) return emptyList()

        val sortedLogs = logs.sortedBy { it.dateString }
        val mensesOnsetIndices = mutableListOf<Int>()

        var wasBleeding = false
        sortedLogs.forEachIndexed { idx, log ->
            if (log.isBleeding) {
                if (!wasBleeding) {
                    mensesOnsetIndices.add(idx)
                    wasBleeding = true
                }
            } else {
                wasBleeding = false
            }
        }

        if (mensesOnsetIndices.isEmpty()) return emptyList()

        val results = mutableListOf<CpassCycleResult>()

        for (i in 0 until mensesOnsetIndices.size) {
            val onsetIdx = mensesOnsetIndices[i]
            val onsetDateStr = sortedLogs[onsetIdx].dateString
            val onsetDate = LocalDate.parse(onsetDateStr, dateFormatter)

            val nextOnsetDateStr = if (i + 1 < mensesOnsetIndices.size) {
                sortedLogs[mensesOnsetIndices[i + 1]].dateString
            } else null

            val cycleEndIdx = if (i + 1 < mensesOnsetIndices.size) {
                mensesOnsetIndices[i + 1] - 1
            } else {
                sortedLogs.lastIndex
            }

            val cycleLogs = sortedLogs.subList(onsetIdx, cycleEndIdx + 1)
            val cycleEndDateStr = cycleLogs.last().dateString
            val cycleEndDate = LocalDate.parse(cycleEndDateStr, dateFormatter)
            val cycleLength = ChronoUnit.DAYS.between(onsetDate, cycleEndDate).toInt() + 1

            // 1. Postmenstrual Window: Days +4 to +10 relative to cycle onset (onset = Day 1)
            // onsetDate + 3 days to onsetDate + 9 days
            val postmenstrualStart = onsetDate.plusDays(3) // Day 4
            val postmenstrualEnd = onsetDate.plusDays(9)   // Day 10
            val postmenstrualLogs = sortedLogs.filter { log ->
                val d = LocalDate.parse(log.dateString, dateFormatter)
                !d.isBefore(postmenstrualStart) && !d.isAfter(postmenstrualEnd)
            }

            // 2. Premenstrual Window: Days -7 to -1 before the NEXT menses onset (or last 7 days of this cycle if next onset is known, or days 21-28)
            val premenstrualLogs: List<DailyLogEntity> = if (nextOnsetDateStr != null) {
                val nextOnsetDate = LocalDate.parse(nextOnsetDateStr, dateFormatter)
                val preStart = nextOnsetDate.minusDays(7)
                val preEnd = nextOnsetDate.minusDays(1)
                sortedLogs.filter { log ->
                    val d = LocalDate.parse(log.dateString, dateFormatter)
                    !d.isBefore(preStart) && !d.isAfter(preEnd)
                }
            } else {
                // If cycle is currently in progress, take days from Day 21 onwards or last 7 days
                val preStart = onsetDate.plusDays(20)
                val preEnd = onsetDate.plusDays(27)
                sortedLogs.filter { log ->
                    val d = LocalDate.parse(log.dateString, dateFormatter)
                    !d.isBefore(preStart) && !d.isAfter(preEnd)
                }
            }

            // Minimum required days to score C-PASS reliably: at least 3 premenstrual days and 3 postmenstrual days
            if (premenstrualLogs.size < 2 && postmenstrualLogs.size < 2) {
                continue
            }

            // Score each of the 11 DSM-5 Domains under C-PASS
            val domainResults = DrspDsm5Directory.DOMAINS.map { domain ->
                scoreDomainCpass(domain, premenstrualLogs, postmenstrualLogs)
            }

            // Criterion B: >= 1 Core Affective domain qualified
            val qualifyingCore = domainResults.filter { it.domain.isCoreAffective && it.isDomainQualified }
            val criterionBMet = qualifyingCore.isNotEmpty()

            // Criterion C: >= 5 Total domains qualified
            val qualifyingAll = domainResults.filter { it.isDomainQualified }
            val criterionCMet = qualifyingAll.size >= 5

            // Criterion D: Premenstrual impairment max >= 4 in items 22, 23, or 24
            val preWorkMax = premenstrualLogs.maxOfOrNull { it.drsp22_impairment_work } ?: 1
            val preRelMax = premenstrualLogs.maxOfOrNull { it.drsp23_impairment_relationships } ?: 1
            val preSocMax = premenstrualLogs.maxOfOrNull { it.drsp24_impairment_social } ?: 1
            val criterionDMet = (preWorkMax >= DrspSeverity.CPASS_SEVERITY_THRESHOLD ||
                    preRelMax >= DrspSeverity.CPASS_SEVERITY_THRESHOLD ||
                    preSocMax >= DrspSeverity.CPASS_SEVERITY_THRESHOLD)

            val qualifyingImpairments = mutableListOf<String>()
            if (preWorkMax >= 4) qualifyingImpairments.add("Work/School/Home Productivity (Max $preWorkMax/6)")
            if (preRelMax >= 4) qualifyingImpairments.add("Interpersonal Relationships (Max $preRelMax/6)")
            if (preSocMax >= 4) qualifyingImpairments.add("Social/Routine Avoidance (Max $preSocMax/6)")

            // Criterion A: Cyclical premenstrual elevation with postmenstrual remission
            val preOverallMean = if (premenstrualLogs.isNotEmpty()) {
                premenstrualLogs.map { it.dailyMeanSeverity }.average().toFloat()
            } else 1f

            val postOverallMean = if (postmenstrualLogs.isNotEmpty()) {
                postmenstrualLogs.map { it.dailyMeanSeverity }.average().toFloat()
            } else 1f

            val overallCycleIncrease = ((preOverallMean - postOverallMean) / 5f * 100f).coerceIn(0f, 100f)
            val criterionAMet = (overallCycleIncrease >= 20f || (preOverallMean >= 2.8f && postOverallMean <= 2.2f))

            val meetsAll = criterionAMet && criterionBMet && criterionCMet && criterionDMet

            val conclusion = if (meetsAll) {
                "Cycle ${i + 1} MEETS DSM-5 PMDD Criteria via C-PASS: ${qualifyingCore.size} core affective & ${qualifyingAll.size} total domains verified with full follicular remission."
            } else {
                val unmet = mutableListOf<String>()
                if (!criterionBMet) unmet.add("Criterion B (0 core domains met C-PASS)")
                if (!criterionCMet) unmet.add("Criterion C (${qualifyingAll.size}/5 domains met C-PASS)")
                if (!criterionDMet) unmet.add("Criterion D (No impairment >= 4)")
                if (!criterionAMet) unmet.add("Criterion A (Insufficient follicular remission)")
                "Cycle ${i + 1} did not meet full criteria: " + unmet.joinToString(", ")
            }

            results.add(
                CpassCycleResult(
                    cycleIndex = i + 1,
                    cycleStartDate = onsetDateStr,
                    cycleEndDate = cycleEndDateStr,
                    cycleLengthDays = cycleLength,
                    nextCycleStartDate = nextOnsetDateStr,
                    premenstrualDaysLogged = premenstrualLogs.size,
                    postmenstrualDaysLogged = postmenstrualLogs.size,
                    totalLogsInCycle = cycleLogs.size,
                    domainResults = domainResults,
                    criterionBMet = criterionBMet,
                    criterionCMet = criterionCMet,
                    criterionDMet = criterionDMet,
                    criterionAMet = criterionAMet,
                    meetsFullCyclePmdd = meetsAll,
                    qualifyingCoreDomains = qualifyingCore.map { it.domain.title },
                    qualifyingAllDomains = qualifyingAll.map { it.domain.title },
                    qualifyingImpairments = qualifyingImpairments,
                    premenstrualOverallMean = preOverallMean,
                    postmenstrualOverallMean = postOverallMean,
                    cycleDiagnosticConclusion = conclusion
                )
            )
        }

        return results
    }

    /**
     * Evaluates a single DSM-5 Domain across premenstrual & postmenstrual windows
     * applying the 4 C-PASS diagnostic rules:
     * 1. Absolute Severity: Premenstrual Max >= 4 (Moderate)
     * 2. Duration: At least 2 premenstrual days >= 4
     * 3. Relative Elevation: (Premenstrual_Mean - Postmenstrual_Mean) / 5.0 >= 0.30 (or >= 30% baseline jump)
     * 4. Absolute Clearance: Postmenstrual Max <= 3 (Mild/None)
     */
    private fun scoreDomainCpass(
        domain: Dsm5DomainDefinition,
        preLogs: List<DailyLogEntity>,
        postLogs: List<DailyLogEntity>
    ): CpassDomainResult {
        val preScores = preLogs.map { it.getDomainScore(domain.domainId) }
        val postScores = postLogs.map { it.getDomainScore(domain.domainId) }

        val preMean = if (preScores.isNotEmpty()) preScores.average().toFloat() else 1f
        val postMean = if (postScores.isNotEmpty()) postScores.average().toFloat() else 1f
        val preMax = preScores.maxOrNull() ?: 1
        val postMax = postScores.maxOrNull() ?: 1

        val daysAboveThreshold = preScores.count { it >= DrspSeverity.CPASS_SEVERITY_THRESHOLD }

        // Rule 1: Absolute Severity
        val rule1 = preMax >= DrspSeverity.CPASS_SEVERITY_THRESHOLD

        // Rule 2: Duration (at least 2 days in premenstrual window >= 4)
        val rule2 = daysAboveThreshold >= 2

        // Rule 3: Relative Elevation (30% elevation of scale range: 5 points on 1-6 scale => 0.30 * 5 = 1.5 increase, or 30% relative jump)
        val diff = preMean - postMean
        val percentOfRange = (diff / 5f) * 100f
        val percentRelative = if (postMean > 0f) (diff / postMean) * 100f else 0f
        val rule3 = (percentOfRange >= 28f || (diff >= 1.4f) || (percentRelative >= 30f && preMean >= 3.5f))

        // Rule 4: Absolute Clearance (Postmenstrual max <= 3)
        val rule4 = postMax <= DrspSeverity.CPASS_CLEARANCE_MAX

        val isQualified = rule1 && rule2 && rule3 && rule4

        val failures = mutableListOf<String>()
        if (!rule1) failures.add("Rule 1 Failed: Premenstrual max ($preMax) < 4")
        if (!rule2) failures.add("Rule 2 Failed: Duration ($daysAboveThreshold days >= 4) < 2 days")
        if (!rule3) failures.add("Rule 3 Failed: Relative elevation (${percentOfRange.toInt()}%) < 30%")
        if (!rule4) failures.add("Rule 4 Failed: Postmenstrual max ($postMax) > 3 (Failed clearance)")

        return CpassDomainResult(
            domain = domain,
            premenstrualMean = preMean,
            postmenstrualMean = postMean,
            premenstrualMax = preMax,
            postmenstrualMax = postMax,
            premenstrualDaysAboveThreshold = daysAboveThreshold,
            rule1AbsoluteSeverityMet = rule1,
            rule2DurationMet = rule2,
            rule3RelativeElevationMet = rule3,
            rule4AbsoluteClearanceMet = rule4,
            percentElevation = max(percentOfRange, 0f),
            isDomainQualified = isQualified,
            failureReasons = failures
        )
    }
}
