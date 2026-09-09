package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.engine.CpassProspectiveReport
import com.example.model.DailyLogEntity
import com.example.model.DrspDsm5Directory
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ClinicalPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 36f // 0.5 inch margin

    /**
     * Generates a standard multi-page clinical PDF report containing:
     * 1. Clinical Evaluation Executive Summary & DSM-5 Diagnostic Criteria Results
     * 2. Carolina Premenstrual Assessment Scoring System (C-PASS) 11-Domain Mathematical Analysis
     * 3. Standard Daily Record of Severity of Problems (DRSP) 21-Item Symptom Matrix
     * 4. Evidence-Based Clinical Treatment Considerations
     * 5. Clinician Review & Signature Block
     */
    fun generateClinicalReportPdf(
        context: Context,
        report: CpassProspectiveReport,
        dailyLogs: List<DailyLogEntity>
    ): File {
        val pdfDocument = PdfDocument()

        // Page 1: Clinical Summary, DSM-5 Criteria Checklist, C-PASS Domain Analysis, & Treatment Considerations
        val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        drawPage1(page1.canvas, report)
        pdfDocument.finishPage(page1)

        // Page 2: Standard Daily DRSP 21-Item Matrix & Clinician Attestation Block
        val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        drawPage2(page2.canvas, report, dailyLogs)
        pdfDocument.finishPage(page2)

        // Save to reports folder in cache/files
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val dateStamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val pdfFile = File(reportsDir, "PMDD_Clinical_DRSP_Report_$dateStamp.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    private fun drawPage1(canvas: Canvas, report: CpassProspectiveReport) {
        var y = MARGIN

        val titlePaint = Paint().apply {
            color = Color.rgb(30, 27, 75) // Deep Navy Indigo
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = Color.rgb(79, 70, 229) // Primary Purple
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(31, 41, 55) // Dark Gray
            textSize = 8f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val boldBodyPaint = Paint().apply {
            color = Color.rgb(17, 24, 39)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val mutedPaint = Paint().apply {
            color = Color.rgb(107, 114, 128)
            textSize = 7.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(229, 231, 235)
            strokeWidth = 1f
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(243, 244, 246)
        }

        // Top Header Banner
        canvas.drawText("CLINICAL EVALUATION REPORT", MARGIN, y + 10f, subTitlePaint)
        y += 22f
        canvas.drawText("Premenstrual Dysphoric Disorder (PMDD) Diagnostic Summary", MARGIN, y + 4f, titlePaint)

        // ICD-10 & Tool Badges on the right
        val badgePaint = Paint().apply {
            color = Color.rgb(238, 242, 255)
        }
        val badgeBorder = Paint().apply {
            color = Color.rgb(99, 102, 241)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val badgeTextPaint = Paint().apply {
            color = Color.rgb(67, 56, 202)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        canvas.drawRoundRect(RectF(PAGE_WIDTH - MARGIN - 90f, MARGIN - 4f, PAGE_WIDTH - MARGIN, MARGIN + 14f), 4f, 4f, badgePaint)
        canvas.drawRoundRect(RectF(PAGE_WIDTH - MARGIN - 90f, MARGIN - 4f, PAGE_WIDTH - MARGIN, MARGIN + 14f), 4f, 4f, badgeBorder)
        canvas.drawText("ICD-10: F32.81", PAGE_WIDTH - MARGIN - 80f, MARGIN + 9f, badgeTextPaint)

        y += 18f
        canvas.drawText(
            "Evaluation Protocol: Daily Record of Severity of Problems (DRSP) & Carolina Premenstrual Assessment Scoring System (C-PASS)",
            MARGIN,
            y,
            mutedPaint
        )
        y += 10f
        canvas.drawText("Report Date: ${LocalDate.now()}  •  Instrument Version: Standard DSM-5 21-Item DRSP Matrix", MARGIN, y, mutedPaint)

        y += 12f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 12f

        // Diagnostic Formulation Box
        val diagBgPaint = Paint().apply {
            color = when (report.prospectiveDiagnosis.badgeType) {
                "POSITIVE" -> Color.rgb(236, 253, 245) // Soft green
                "WARNING" -> Color.rgb(254, 243, 199) // Soft amber
                else -> Color.rgb(243, 244, 246)
            }
        }
        val diagBorderPaint = Paint().apply {
            color = when (report.prospectiveDiagnosis.badgeType) {
                "POSITIVE" -> Color.rgb(16, 185, 129)
                "WARNING" -> Color.rgb(245, 158, 11)
                else -> Color.rgb(156, 163, 175)
            }
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val diagTitleColor = when (report.prospectiveDiagnosis.badgeType) {
            "POSITIVE" -> Color.rgb(6, 95, 70)
            "WARNING" -> Color.rgb(146, 64, 14)
            else -> Color.rgb(31, 41, 55)
        }
        val diagTitlePaint = Paint().apply {
            color = diagTitleColor
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val diagBoxTop = y
        val diagBoxHeight = 52f
        canvas.drawRoundRect(RectF(MARGIN, diagBoxTop, PAGE_WIDTH - MARGIN, diagBoxTop + diagBoxHeight), 6f, 6f, diagBgPaint)
        canvas.drawRoundRect(RectF(MARGIN, diagBoxTop, PAGE_WIDTH - MARGIN, diagBoxTop + diagBoxHeight), 6f, 6f, diagBorderPaint)

        canvas.drawText("PROSPECTIVE DIAGNOSTIC CONCLUSION:", MARGIN + 10f, diagBoxTop + 14f, subTitlePaint)
        canvas.drawText(report.prospectiveDiagnosis.title, MARGIN + 10f, diagBoxTop + 28f, diagTitlePaint)
        canvas.drawText(
            "Cycles Evaluated: ${report.totalCyclesEvaluated.coerceAtLeast(1)}   |   Consecutive Qualifying Cycles: ${report.consecutiveQualifyingCycles}   |   Criterion F Met: ${if (report.criterionFMet) "YES (>=2 Cycles)" else "NO"}",
            MARGIN + 10f,
            diagBoxTop + 42f,
            boldBodyPaint
        )

        y = diagBoxTop + diagBoxHeight + 14f

        // SECTION 1: DSM-5 Diagnostic Criteria Checklist
        canvas.drawText("1. DSM-5 DIAGNOSTIC CRITERIA COMPLIANCE STATUS", MARGIN, y, boldBodyPaint)
        y += 8f

        for ((criterionText, isMet) in report.dsm5CriteriaChecklist) {
            val statusColor = if (isMet) Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68)
            val checkPaint = Paint().apply {
                color = statusColor
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val statusLabel = if (isMet) "[MET / VERIFIED]" else "[NOT MET / INSUFFICIENT]"

            canvas.drawText(if (isMet) "✓" else "✗", MARGIN + 4f, y + 8f, checkPaint)
            canvas.drawText(criterionText, MARGIN + 18f, y + 8f, bodyPaint)
            canvas.drawText(statusLabel, PAGE_WIDTH - MARGIN - 120f, y + 8f, checkPaint)
            y += 12f
        }

        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 14f

        // SECTION 2: C-PASS 11-Domain Mathematical Evaluation Table
        canvas.drawText("2. C-PASS 11-DOMAIN MATHEMATICAL SCORING ANALYSIS (CYCLE 1 & 2)", MARGIN, y, boldBodyPaint)
        y += 8f

        // Table Header
        val colWidths = floatArrayOf(150f, 65f, 65f, 55f, 55f, 55f, 78f)
        val headers = arrayOf("DSM-5 Domain", "Premen Mean", "Postmen Mean", "% Elev", "Rule 1-2", "Clearance", "Status")
        val tableLeft = MARGIN
        val tableRight = PAGE_WIDTH - MARGIN

        canvas.drawRect(RectF(tableLeft, y, tableRight, y + 14f), headerBgPaint)
        canvas.drawRect(RectF(tableLeft, y, tableRight, y + 14f), linePaint.apply { style = Paint.Style.STROKE })

        var curX = tableLeft + 4f
        for (i in headers.indices) {
            canvas.drawText(headers[i], curX, y + 10f, boldBodyPaint.apply { textSize = 6.5f })
            curX += colWidths[i]
        }
        y += 14f

        val firstCycle = report.cycleResults.firstOrNull()
        if (firstCycle != null) {
            for (domainResult in firstCycle.domainResults) {
                val rowHeight = 12f
                val isCore = domainResult.domain.isCoreAffective
                val qual = domainResult.isDomainQualified

                val domainName = "${domainResult.domain.domainId}. ${domainResult.domain.title}"
                val preStr = String.format("%.2f (Max %d)", domainResult.premenstrualMean, domainResult.premenstrualMax)
                val postStr = String.format("%.2f (Max %d)", domainResult.postmenstrualMean, domainResult.postmenstrualMax)
                val elevStr = String.format("+%.0f%%", domainResult.percentElevation)
                val r12Str = if (domainResult.rule1AbsoluteSeverityMet && domainResult.rule2DurationMet) "Pass" else "Fail"
                val clearStr = if (domainResult.rule4AbsoluteClearanceMet) "Pass (<=3)" else "Elevated"
                val statusStr = if (qual) "QUALIFIED" else "Non-Qual"

                val rowPaint = if (qual) boldBodyPaint else bodyPaint
                val qualPaint = Paint().apply {
                    color = if (qual) Color.rgb(16, 185, 129) else Color.rgb(156, 163, 175)
                    textSize = 6.5f
                    typeface = if (qual) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                    isAntiAlias = true
                }

                canvas.drawLine(tableLeft, y + rowHeight, tableRight, y + rowHeight, linePaint)

                var rowX = tableLeft + 4f
                canvas.drawText(domainName.take(28), rowX, y + 9f, (if (isCore) boldBodyPaint else bodyPaint).apply { textSize = 6.5f })
                rowX += colWidths[0]
                canvas.drawText(preStr, rowX, y + 9f, rowPaint.apply { textSize = 6.5f })
                rowX += colWidths[1]
                canvas.drawText(postStr, rowX, y + 9f, bodyPaint.apply { textSize = 6.5f })
                rowX += colWidths[2]
                canvas.drawText(elevStr, rowX, y + 9f, rowPaint.apply { textSize = 6.5f })
                rowX += colWidths[3]
                canvas.drawText(r12Str, rowX, y + 9f, bodyPaint.apply { textSize = 6.5f })
                rowX += colWidths[4]
                canvas.drawText(clearStr, rowX, y + 9f, bodyPaint.apply { textSize = 6.5f })
                rowX += colWidths[5]
                canvas.drawText(statusStr, rowX, y + 9f, qualPaint)

                y += rowHeight
            }
        } else {
            canvas.drawText("No segmented cycle available for tabular breakdown. Minimum 1 cycle required.", tableLeft + 10f, y + 10f, mutedPaint)
            y += 16f
        }

        y += 10f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 12f

        // SECTION 3: Evidence-Based Clinical Considerations
        canvas.drawText("3. EVIDENCE-BASED CLINICAL CONSIDERATIONS & GUIDELINE RECOMMENDATIONS", MARGIN, y, boldBodyPaint)
        y += 8f
        val treatLines = listOf(
            "• First-Line Pharmacotherapy: SSRIs (Fluoxetine 20mg, Sertraline 50-100mg, or Escitalopram 10-20mg). Effective in continuous or luteal-phase-only dosing.",
            "• Hormonal Regulation: Monophasic Drospirenone 3mg / Ethinyl Estradiol 20mcg (24/4 regimen) FDA-approved to suppress ovulation and premenstrual mood surges.",
            "• Psychotherapy & Non-Pharmacological: Cognitive Behavioral Therapy (CBT) specifically formatted for premenstrual distress.",
            "• Adjunct Lifestyle: Aerobic exercise, Calcium 1200mg/day, Vitamin B6, and Chasteberry (Vitex agnus-castus) for mild somatic symptom relief."
        )
        for (line in treatLines) {
            canvas.drawText(line, MARGIN + 4f, y + 6f, bodyPaint.apply { textSize = 7f })
            y += 10f
        }

        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 12f

        // SECTION 4: Clinical Evaluation Roadmap & Diagnostic Confirmation Protocol
        canvas.drawText("4. CLINICAL EVALUATION ROADMAP & DIAGNOSTIC PROTOCOL", MARGIN, y, boldBodyPaint)
        y += 8f

        val roadmapSteps = listOf(
            "Step 1: Prospective Daily Tracking — Daily 1–6 DRSP logging across >=2 full consecutive menstrual cycles.",
            "Step 2: Verify Luteal Onset Surge — >=30% premenstrual severity elevation with absolute score >= 4.0 in DSM-5 domains.",
            "Step 3: Validate Follicular Remission — Score clearance (<= 3.0) during postmenstrual baseline (Days +4 to +10), ruling out PME/MDD.",
            "Step 4: Collaborative Medical Plan — Review report with clinician for individualized pharmacotherapy or hormonal management."
        )

        val stepBoxBg = Paint().apply { color = Color.rgb(249, 250, 251) }
        val stepBoxBorder = Paint().apply {
            color = Color.rgb(229, 231, 235)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val roadmapBoxTop = y
        val roadmapBoxHeight = (roadmapSteps.size * 12f) + 8f
        canvas.drawRoundRect(RectF(MARGIN, roadmapBoxTop, PAGE_WIDTH - MARGIN, roadmapBoxTop + roadmapBoxHeight), 4f, 4f, stepBoxBg)
        canvas.drawRoundRect(RectF(MARGIN, roadmapBoxTop, PAGE_WIDTH - MARGIN, roadmapBoxTop + roadmapBoxHeight), 4f, 4f, stepBoxBorder)

        y = roadmapBoxTop + 10f
        for (step in roadmapSteps) {
            canvas.drawText("• $step", MARGIN + 8f, y, bodyPaint.apply { textSize = 6.8f })
            y += 12f
        }

        // Page 1 Footer
        canvas.drawLine(MARGIN, PAGE_HEIGHT - MARGIN - 14f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN - 14f, linePaint)
        canvas.drawText("Confidential Medical Record  •  Standard DRSP/C-PASS Protocol", MARGIN, PAGE_HEIGHT - MARGIN, mutedPaint)
        canvas.drawText("Page 1 of 2", PAGE_WIDTH - MARGIN - 40f, PAGE_HEIGHT - MARGIN, mutedPaint)
    }

    private fun drawPage2(canvas: Canvas, report: CpassProspectiveReport, dailyLogs: List<DailyLogEntity>) {
        var y = MARGIN

        val titlePaint = Paint().apply {
            color = Color.rgb(30, 27, 75)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val boldBodyPaint = Paint().apply {
            color = Color.rgb(17, 24, 39)
            textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(31, 41, 55)
            textSize = 6.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val mutedPaint = Paint().apply {
            color = Color.rgb(107, 114, 128)
            textSize = 6.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(229, 231, 235)
            strokeWidth = 0.8f
        }

        // Header for Page 2
        canvas.drawText("FULL STANDARDIZED DAILY RECORD OF SEVERITY OF PROBLEMS (DRSP) MATRIX", MARGIN, y + 8f, titlePaint)
        y += 14f
        canvas.drawText(
            "Ratings: 1=Not at all, 2=Minimal, 3=Mild, 4=Moderate, 5=Severe, 6=Extreme. Scores >=4 represent clinical distress threshold (highlighted).",
            MARGIN,
            y + 6f,
            mutedPaint
        )
        y += 14f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 10f

        // Build Daily Matrix Grid
        // Show up to the latest 14-20 days of logs sorted by date
        val sortedLogs = dailyLogs.sortedBy { it.dateString }
        val displayLogs = if (sortedLogs.size > 14) sortedLogs.takeLast(14) else sortedLogs

        val symptomNames = listOf(
            "1. Felt depressed, sad, down",
            "2. Felt hopeless",
            "3. Felt worthless or guilty",
            "4. Felt anxious, tense, on edge",
            "5. Mood swings / suddenly tearful",
            "6. Rejection sensitive",
            "7. Felt angry or irritable",
            "8. Conflicts / arguments",
            "9. Decreased interest (work/school)",
            "10. Social withdrawal",
            "11. Concentration difficulty",
            "12. Tired / lacked energy",
            "13. Appetite increase / overeating",
            "14. Specific food cravings",
            "15. Insomnia / sleep trouble",
            "16. Hypersomnia / excessive sleep",
            "17. Overwhelmed / unable to cope",
            "18. Feeling out of control",
            "19. Breast tenderness / swelling",
            "20. Headaches, muscle/body aches",
            "21. Bloating, weight gain",
            "22. Impairment: Work / Duties",
            "23. Impairment: Relationships",
            "24. Impairment: Social / Hobbies"
        )

        val tableLeft = MARGIN
        val labelColWidth = 145f
        val numDays = displayLogs.size.coerceAtLeast(1)
        val availableWidth = (PAGE_WIDTH - 2 * MARGIN) - labelColWidth
        val dayColWidth = (availableWidth / numDays.coerceAtMost(14)).coerceIn(24f, 32f)

        // Matrix Header: Dates and Cycle Days
        val headerBg = Paint().apply { color = Color.rgb(243, 244, 246) }
        canvas.drawRect(RectF(tableLeft, y, tableLeft + labelColWidth + (numDays * dayColWidth), y + 20f), headerBg)
        canvas.drawText("DRSP Symptom Item", tableLeft + 4f, y + 13f, boldBodyPaint)

        for (d in displayLogs.indices) {
            val log = displayLogs[d]
            val dateLabel = log.dateString.takeLast(5) // MM-DD
            val dayX = tableLeft + labelColWidth + (d * dayColWidth)
            canvas.drawText(dateLabel, dayX + 2f, y + 8f, boldBodyPaint.apply { textSize = 5.5f })
            val phaseLabel = if (log.isBleeding) "Menses" else if ((log.cycleDay ?: 1) in 20..35) "Luteal" else "Follic"
            canvas.drawText(phaseLabel.take(5), dayX + 2f, y + 16f, mutedPaint.apply { textSize = 5f })
        }
        y += 22f

        // Draw each DRSP row
        val severeCellPaint = Paint().apply { color = Color.rgb(254, 226, 226) } // Light red highlight for >=4
        val severeTextPaint = Paint().apply {
            color = Color.rgb(185, 28, 28)
            textSize = 6.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val rowHeight = 11.5f
        for (itemIndex in 1..24) {
            val isImpairment = itemIndex >= 22
            val symptomTitle = symptomNames.getOrNull(itemIndex - 1) ?: "Item $itemIndex"

            canvas.drawLine(tableLeft, y + rowHeight, tableLeft + labelColWidth + (numDays * dayColWidth), y + rowHeight, linePaint)

            canvas.drawText(
                symptomTitle,
                tableLeft + 4f,
                y + 8.5f,
                (if (isImpairment || itemIndex in 1..8) boldBodyPaint else bodyPaint).apply { textSize = 6f }
            )

            for (d in displayLogs.indices) {
                val log = displayLogs[d]
                val score = when (itemIndex) {
                    1 -> log.drsp1_depressed
                    2 -> log.drsp2_hopeless
                    3 -> log.drsp3_worthless
                    4 -> log.drsp4_anxious
                    5 -> log.drsp5_mood_swings
                    6 -> log.drsp6_rejection_sensitive
                    7 -> log.drsp7_angry_irritable
                    8 -> log.drsp8_conflicts
                    9 -> log.drsp9_decreased_interest
                    10 -> log.drsp10_social_withdrawal
                    11 -> log.drsp11_concentration
                    12 -> log.drsp12_tired_low_energy
                    13 -> log.drsp13_appetite_increase
                    14 -> log.drsp14_food_cravings
                    15 -> log.drsp15_insomnia
                    16 -> log.drsp16_hypersomnia
                    17 -> log.drsp17_overwhelmed
                    18 -> log.drsp18_out_of_control
                    19 -> log.drsp19_breast_tenderness
                    20 -> log.drsp20_headaches_body_aches
                    21 -> log.drsp21_bloating_weight
                    22 -> log.drsp22_impairment_work
                    23 -> log.drsp23_impairment_relationships
                    24 -> log.drsp24_impairment_social
                    else -> 1
                }

                val cellX = tableLeft + labelColWidth + (d * dayColWidth)
                if (score >= 4) {
                    canvas.drawRect(RectF(cellX, y + 1f, cellX + dayColWidth - 1f, y + rowHeight - 1f), severeCellPaint)
                    canvas.drawText(score.toString(), cellX + 8f, y + 8.5f, severeTextPaint)
                } else {
                    canvas.drawText(score.toString(), cellX + 8f, y + 8.5f, bodyPaint.apply { textSize = 6.5f })
                }
            }
            y += rowHeight
        }

        y += 14f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 14f

        // SECTION 5: Clinician Attestation & Review Signature Box
        val signBoxTop = y
        val signBoxHeight = 85f
        canvas.drawRoundRect(RectF(MARGIN, signBoxTop, PAGE_WIDTH - MARGIN, signBoxTop + signBoxHeight), 6f, 6f, Paint().apply { color = Color.rgb(249, 250, 251) })
        canvas.drawRoundRect(RectF(MARGIN, signBoxTop, PAGE_WIDTH - MARGIN, signBoxTop + signBoxHeight), 6f, 6f, Paint().apply {
            color = Color.rgb(209, 213, 219)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        })

        canvas.drawText("PHYSICIAN / CLINICIAN REVIEW & TREATMENT PLAN", MARGIN + 10f, signBoxTop + 14f, boldBodyPaint.apply { textSize = 8f })
        canvas.drawText("Clinical Notes & Differential Diagnosis:", MARGIN + 10f, signBoxTop + 28f, mutedPaint)
        canvas.drawLine(MARGIN + 10f, signBoxTop + 44f, PAGE_WIDTH - MARGIN - 10f, signBoxTop + 44f, linePaint)
        canvas.drawLine(MARGIN + 10f, signBoxTop + 58f, PAGE_WIDTH - MARGIN - 10f, signBoxTop + 58f, linePaint)

        canvas.drawText("Provider Signature: ___________________________", MARGIN + 10f, signBoxTop + 74f, boldBodyPaint.apply { textSize = 7.5f })
        canvas.drawText("Date: ____________", MARGIN + 260f, signBoxTop + 74f, boldBodyPaint.apply { textSize = 7.5f })
        canvas.drawText("NPI / License: _________________", PAGE_WIDTH - MARGIN - 160f, signBoxTop + 74f, boldBodyPaint.apply { textSize = 7.5f })

        // Page 2 Footer
        canvas.drawLine(MARGIN, PAGE_HEIGHT - MARGIN - 14f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN - 14f, linePaint)
        canvas.drawText("Confidential Medical Record  •  Standard DRSP/C-PASS Protocol", MARGIN, PAGE_HEIGHT - MARGIN, mutedPaint)
        canvas.drawText("Page 2 of 2", PAGE_WIDTH - MARGIN - 40f, PAGE_HEIGHT - MARGIN, mutedPaint)
    }

    /**
     * Creates an Intent to share the generated PDF report via Android Share Sheet
     */
    fun createSharePdfIntent(context: Context, pdfFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PMDD Clinical DRSP & C-PASS Evaluation Report")
            putExtra(
                Intent.EXTRA_TEXT,
                "Please find attached the prospective daily DRSP symptom log and Carolina Premenstrual Assessment Scoring System (C-PASS) clinical evaluation report."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Creates an Intent to view/open the PDF in a PDF reader app
     */
    fun createViewPdfIntent(context: Context, pdfFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
