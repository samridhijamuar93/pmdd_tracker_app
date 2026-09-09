package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Standard Daily Record of Severity of Problems (DRSP) 1-6 Likert Scale:
 * 1 = Not at all (None)
 * 2 = Minimal (Slightly noticeable)
 * 3 = Mild (Noticeable, but easily tolerated)
 * 4 = Moderate (Substantial distress, meets clinical severity threshold)
 * 5 = Severe (Marked distress / impairment)
 * 6 = Extreme (Incapacitating / disabling)
 */
enum class DrspSeverity(val score: Int, val label: String, val shortLabel: String) {
    NOT_AT_ALL(1, "Not at all (1)", "None (1)"),
    MINIMAL(2, "Minimal (2)", "Min (2)"),
    MILD(3, "Mild (3)", "Mild (3)"),
    MODERATE(4, "Moderate (4)", "Mod (4)"),
    SEVERE(5, "Severe (5)", "Sev (5)"),
    EXTREME(6, "Extreme (6)", "Ext (6)");

    companion object {
        fun fromScore(score: Int): DrspSeverity = entries.find { it.score == score } ?: NOT_AT_ALL
        const val CPASS_SEVERITY_THRESHOLD = 4 // DRSP score >= 4 is required for C-PASS moderate/severe
        const val CPASS_CLEARANCE_MAX = 3 // DRSP score <= 3 in follicular window is required for C-PASS clearance
    }
}

enum class PeriodFlow(val label: String, val isBleeding: Boolean) {
    NONE("No Bleeding", false),
    SPOTTING("Spotting", true),
    LIGHT("Light Flow", true),
    MEDIUM("Medium Flow", true),
    HEAVY("Heavy Flow", true)
}

enum class DsmCategory(val title: String, val isCore: Boolean) {
    CORE_AFFECTIVE("Criterion B: Core Affective Domains", true),
    ADDITIONAL_SYMPTOMS("Criterion C: Additional Cognitive & Somatic Domains", false),
    FUNCTIONAL_IMPAIRMENT("Criterion D: Functional Impairment", false)
}

/**
 * 11 DSM-5 PMDD Domains mapped to the 21 DRSP symptom items
 */
data class Dsm5DomainDefinition(
    val domainId: Int, // 1 to 11
    val code: String,
    val title: String,
    val criterion: String,
    val isCoreAffective: Boolean,
    val drspItemNumbers: List<Int>,
    val drspItemDescriptions: List<String>
)

object DrspDsm5Directory {

    val DOMAINS = listOf(
        // Domain 1: Depressed Mood (Criterion B.3)
        Dsm5DomainDefinition(
            domainId = 1,
            code = "depressed_mood",
            title = "Depression & Hopelessness",
            criterion = "Criterion B.3",
            isCoreAffective = true,
            drspItemNumbers = listOf(1, 2, 3),
            drspItemDescriptions = listOf(
                "Felt depressed, sad, down, or blue",
                "Felt hopeless",
                "Felt worthless or guilty"
            )
        ),
        // Domain 2: Anxiety / Tension (Criterion B.4)
        Dsm5DomainDefinition(
            domainId = 2,
            code = "anxiety_tension",
            title = "Anxiety, Tension & On Edge",
            criterion = "Criterion B.4",
            isCoreAffective = true,
            drspItemNumbers = listOf(4),
            drspItemDescriptions = listOf(
                "Felt anxious, tense, keyed up, or on edge"
            )
        ),
        // Domain 3: Affective Lability (Criterion B.1)
        Dsm5DomainDefinition(
            domainId = 3,
            code = "affective_lability",
            title = "Affective Lability & Mood Swings",
            criterion = "Criterion B.1",
            isCoreAffective = true,
            drspItemNumbers = listOf(5, 6),
            drspItemDescriptions = listOf(
                "Had mood swings (suddenly feeling sad, tearful, or sensitive)",
                "Felt sensitive to rejection, feelings easily hurt"
            )
        ),
        // Domain 4: Irritability / Anger (Criterion B.2)
        Dsm5DomainDefinition(
            domainId = 4,
            code = "irritability_anger",
            title = "Irritability, Anger & Conflicts",
            criterion = "Criterion B.2",
            isCoreAffective = true,
            drspItemNumbers = listOf(7, 8),
            drspItemDescriptions = listOf(
                "Felt angry or irritable",
                "Had conflicts or arguments with people"
            )
        ),
        // Domain 5: Decreased Interest (Criterion C.1)
        Dsm5DomainDefinition(
            domainId = 5,
            code = "decreased_interest",
            title = "Decreased Interest (Anhedonia)",
            criterion = "Criterion C.1",
            isCoreAffective = false,
            drspItemNumbers = listOf(9, 10),
            drspItemDescriptions = listOf(
                "Had less interest in usual activities (work, school, hobbies)",
                "Had less interest in social activities / friends"
            )
        ),
        // Domain 6: Concentration Difficulty (Criterion C.2)
        Dsm5DomainDefinition(
            domainId = 6,
            code = "concentration",
            title = "Difficulty Concentrating",
            criterion = "Criterion C.2",
            isCoreAffective = false,
            drspItemNumbers = listOf(11),
            drspItemDescriptions = listOf(
                "Had difficulty concentrating, focusing, or thinking clearly"
            )
        ),
        // Domain 7: Lethargy / Fatigue (Criterion C.3)
        Dsm5DomainDefinition(
            domainId = 7,
            code = "lethargy_fatigue",
            title = "Lethargy & Low Energy",
            criterion = "Criterion C.3",
            isCoreAffective = false,
            drspItemNumbers = listOf(12),
            drspItemDescriptions = listOf(
                "Felt lethargic, tired easily, or lacked energy"
            )
        ),
        // Domain 8: Appetite Change (Criterion C.4)
        Dsm5DomainDefinition(
            domainId = 8,
            code = "appetite_cravings",
            title = "Appetite Change & Food Cravings",
            criterion = "Criterion C.4",
            isCoreAffective = false,
            drspItemNumbers = listOf(13, 14),
            drspItemDescriptions = listOf(
                "Had increased appetite or overeating",
                "Had cravings for specific foods (sweets, carbs, salty)"
            )
        ),
        // Domain 9: Sleep Disturbance (Criterion C.5)
        Dsm5DomainDefinition(
            domainId = 9,
            code = "sleep_problems",
            title = "Hypersomnia or Insomnia",
            criterion = "Criterion C.5",
            isCoreAffective = false,
            drspItemNumbers = listOf(15, 16),
            drspItemDescriptions = listOf(
                "Had trouble sleeping (falling asleep, staying asleep, waking early)",
                "Slept more than usual or had trouble waking up (hypersomnia)"
            )
        ),
        // Domain 10: Overwhelmed (Criterion C.6)
        Dsm5DomainDefinition(
            domainId = 10,
            code = "overwhelmed",
            title = "Overwhelmed / Out of Control",
            criterion = "Criterion C.6",
            isCoreAffective = false,
            drspItemNumbers = listOf(17, 18),
            drspItemDescriptions = listOf(
                "Felt overwhelmed or unable to cope",
                "Felt out of control"
            )
        ),
        // Domain 11: Physical Symptoms (Criterion C.7)
        Dsm5DomainDefinition(
            domainId = 11,
            code = "physical_symptoms",
            title = "Physical & Somatic Symptoms",
            criterion = "Criterion C.7",
            isCoreAffective = false,
            drspItemNumbers = listOf(19, 20, 21),
            drspItemDescriptions = listOf(
                "Breast tenderness, pain, or swelling",
                "Headaches, joint pain, muscle aches, or stiffness",
                "Bloating, feeling full, or weight gain"
            )
        )
    )

    // Functional Impairment items (DRSP items 22, 23, 24)
    val IMPAIRMENT_ITEMS = listOf(
        Pair(22, "Interference with work, school, or home duties"),
        Pair(23, "Interference with relationships with family, friends, or coworkers"),
        Pair(24, "Avoidance of social activities, hobbies, or routine events")
    )
}

@Entity(tableName = "daily_drsp_logs")
data class DailyLogEntity(
    @PrimaryKey val dateString: String, // Format: YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val periodFlow: String = PeriodFlow.NONE.name,
    val cycleDay: Int? = null,

    // DRSP 21 Items (1 to 6 scale, default 1 = Not at all)
    val drsp1_depressed: Int = 1,
    val drsp2_hopeless: Int = 1,
    val drsp3_worthless: Int = 1,
    val drsp4_anxious: Int = 1,
    val drsp5_mood_swings: Int = 1,
    val drsp6_rejection_sensitive: Int = 1,
    val drsp7_angry_irritable: Int = 1,
    val drsp8_conflicts: Int = 1,
    val drsp9_decreased_interest: Int = 1,
    val drsp10_social_withdrawal: Int = 1,
    val drsp11_concentration: Int = 1,
    val drsp12_tired_low_energy: Int = 1,
    val drsp13_appetite_increase: Int = 1,
    val drsp14_food_cravings: Int = 1,
    val drsp15_insomnia: Int = 1,
    val drsp16_hypersomnia: Int = 1,
    val drsp17_overwhelmed: Int = 1,
    val drsp18_out_of_control: Int = 1,
    val drsp19_breast_tenderness: Int = 1,
    val drsp20_headaches_body_aches: Int = 1,
    val drsp21_bloating_weight: Int = 1,

    // DRSP Impairment Items (22, 23, 24)
    val drsp22_impairment_work: Int = 1,
    val drsp23_impairment_relationships: Int = 1,
    val drsp24_impairment_social: Int = 1,

    // Context notes & medication
    val notes: String = "",
    val tookMedication: Boolean = false,
    val medicationDetails: String = "",
    val somaticTags: String = "",

    // Apple Watch / Pixel Watch Wearable Telemetry
    val restingHeartRate: Int? = null,
    val hrvRmssd: Double? = null,
    val sleepDurationHours: Double? = null,
    val deepSleepMinutes: Int? = null,
    val remSleepMinutes: Int? = null,
    val basalBodyTempF: Double? = null,
    val stepCount: Int? = null,

    // Continuous Glucose Monitor (CGM) Telemetry
    val cgmCurrentGlucose: Double? = null,
    val cgmMeanGlucose: Double? = null,
    val cgmTimeInRangePct: Double? = null,
    val cgmGlucoseCvPct: Double? = null,
    val wearableSource: String = ""
) {
    fun getFlow(): PeriodFlow = try {
        PeriodFlow.valueOf(periodFlow)
    } catch (e: Exception) {
        PeriodFlow.NONE
    }

    val isBleeding: Boolean
        get() = getFlow().isBleeding

    /**
     * Maps the 21 DRSP items to the 11 DSM-5 domains by taking the maximum score of items in each domain
     * (Standard C-PASS scoring uses the item-level maximum within each DSM-5 domain for daily severity)
     */
    fun getDomainScore(domainId: Int): Int {
        return when (domainId) {
            1 -> maxOf(drsp1_depressed, drsp2_hopeless, drsp3_worthless)
            2 -> drsp4_anxious
            3 -> maxOf(drsp5_mood_swings, drsp6_rejection_sensitive)
            4 -> maxOf(drsp7_angry_irritable, drsp8_conflicts)
            5 -> maxOf(drsp9_decreased_interest, drsp10_social_withdrawal)
            6 -> drsp11_concentration
            7 -> drsp12_tired_low_energy
            8 -> maxOf(drsp13_appetite_increase, drsp14_food_cravings)
            9 -> maxOf(drsp15_insomnia, drsp16_hypersomnia)
            10 -> maxOf(drsp17_overwhelmed, drsp18_out_of_control)
            11 -> maxOf(drsp19_breast_tenderness, drsp20_headaches_body_aches, drsp21_bloating_weight)
            else -> 1
        }
    }

    val all11DomainScores: List<Int>
        get() = (1..11).map { getDomainScore(it) }

    val core4DomainScores: List<Int>
        get() = (1..4).map { getDomainScore(it) }

    val impairmentScores: List<Int>
        get() = listOf(drsp22_impairment_work, drsp23_impairment_relationships, drsp24_impairment_social)

    val maxImpairmentScore: Int
        get() = impairmentScores.maxOrNull() ?: 1

    val dailyMeanSeverity: Float
        get() = all11DomainScores.average().toFloat()

    val totalDrsp21Sum: Int
        get() = drsp1_depressed + drsp2_hopeless + drsp3_worthless + drsp4_anxious +
                drsp5_mood_swings + drsp6_rejection_sensitive + drsp7_angry_irritable +
                drsp8_conflicts + drsp9_decreased_interest + drsp10_social_withdrawal +
                drsp11_concentration + drsp12_tired_low_energy + drsp13_appetite_increase +
                drsp14_food_cravings + drsp15_insomnia + drsp16_hypersomnia +
                drsp17_overwhelmed + drsp18_out_of_control + drsp19_breast_tenderness +
                drsp20_headaches_body_aches + drsp21_bloating_weight
}
