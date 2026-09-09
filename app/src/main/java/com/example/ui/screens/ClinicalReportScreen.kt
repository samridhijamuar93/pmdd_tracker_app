package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ClinicalPdfGenerator
import com.example.viewmodel.PmddViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun ClinicalReportScreen(
    viewModel: PmddViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val report by viewModel.cpassReport.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val treatmentCarouselListState = rememberLazyListState()
    var showTelehealthDialog by remember { mutableStateOf(false) }

    if (showTelehealthDialog) {
        TelehealthConsultDialog(
            report = report.prospectiveDiagnosis,
            viewModel = viewModel,
            onDismiss = { showTelehealthDialog = false }
        )
    }

    fun exportAndSharePdf() {
        try {
            val pdfFile = ClinicalPdfGenerator.generateClinicalReportPdf(context, report, allLogs)
            val shareIntent = ClinicalPdfGenerator.createSharePdfIntent(context, pdfFile)
            context.startActivity(Intent.createChooser(shareIntent, "Share Clinical DRSP Report with Doctor"))
            viewModel.showNotification("Generated official clinical DRSP PDF")
        } catch (e: Exception) {
            viewModel.showNotification("Error sharing PDF: ${e.localizedMessage}", isError = true)
        }
    }

    fun downloadAndOpenPdf() {
        try {
            val pdfFile = ClinicalPdfGenerator.generateClinicalReportPdf(context, report, allLogs)
            val viewIntent = ClinicalPdfGenerator.createViewPdfIntent(context, pdfFile)
            context.startActivity(Intent.createChooser(viewIntent, "Open Clinical DRSP Report"))
            viewModel.showNotification("Clinical PDF saved: ${pdfFile.name}")
        } catch (e: Exception) {
            viewModel.showNotification("Error downloading PDF: ${e.localizedMessage}", isError = true)
        }
    }

    fun shareTextSummary() {
        val summaryText = buildString {
            appendLine("CLINICAL EVALUATION REPORT: PMDD PROSPECTIVE SCORING")
            appendLine("ICD-10 Diagnostic Code: F32.81 Premenstrual Dysphoric Disorder")
            appendLine("Instrument: Daily Record of Severity of Problems (DRSP) & C-PASS Algorithm")
            appendLine("Date: ${LocalDate.now()}")
            appendLine("--------------------------------------------------")
            appendLine("DIAGNOSTIC CONCLUSION: ${report.prospectiveDiagnosis.title}")
            appendLine(report.prospectiveDiagnosis.summary)
            appendLine("Cycles Evaluated: ${report.totalCyclesEvaluated} (Consecutive Qualifying: ${report.consecutiveQualifyingCycles})")
            appendLine("\nDSM-5 DIAGNOSTIC CRITERIA STATUS:")
            report.dsm5CriteriaChecklist.forEach { (criterion, isMet) ->
                appendLine("[${if (isMet) "✓ MET" else "✗ NOT MET"}] $criterion")
            }
            appendLine("\nEVIDENCE-BASED CLINICAL CONSIDERATIONS:")
            appendLine(report.treatmentConsiderations)
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "PMDD Clinical DRSP & C-PASS Summary")
            putExtra(Intent.EXTRA_TEXT, summaryText)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Clinical Summary"))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("clinical_report_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Action Card: Download & Share Official PDF
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Standard Clinical Report (PDF)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Includes DSM-5 checklist, C-PASS table & full 21 DRSP chart",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { exportAndSharePdf() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("share_pdf_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share PDF", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { downloadAndOpenPdf() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("download_pdf_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { shareTextSummary() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("share_text_btn")
                        ) {
                            Icon(Icons.Default.TextSnippet, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start Telehealth Consultation Button
            Button(
                onClick = { showTelehealthDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("open_telehealth_dialog_btn")
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Telehealth Consultation", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            data class TreatmentCardModel(
                val title: String,
                val categoryBadge: String,
                val icon: androidx.compose.ui.graphics.vector.ImageVector,
                val description: String,
                val clinicalCategory: String
            )

            val treatmentCardList = when (report.prospectiveDiagnosis) {
                com.example.engine.CpassProspectiveDiagnosis.CONFIRMED_PMDD,
                com.example.engine.CpassProspectiveDiagnosis.ONE_CYCLE_CONFIRMED -> listOf(
                    TreatmentCardModel(
                        title = "First-Line Pharmacotherapy (SSRIs)",
                        categoryBadge = "FIRST-LINE",
                        icon = Icons.Default.Medication,
                        description = "Luteal-phase dosing (from ovulation until menses onset) or continuous SSRIs (Fluoxetine, Sertraline, Escitalopram) demonstrate rapid onset for affective PMDD symptoms without multi-week delay.",
                        clinicalCategory = "Guideline Tier 1"
                    ),
                    TreatmentCardModel(
                        title = "Hormonal Ovulation Suppression",
                        categoryBadge = "HORMONAL",
                        icon = Icons.Default.MedicalServices,
                        description = "Combined oral contraceptives containing drospirenone (3 mg) and ethinyl estradiol (20 mcg) with a shortened 4-day or extended hormone-free interval to eliminate cyclical endocrine surges.",
                        clinicalCategory = "Endocrine Suppression"
                    ),
                    TreatmentCardModel(
                        title = "Targeted Psychotherapy (CBT)",
                        categoryBadge = "PSYCHOTHERAPY",
                        icon = Icons.Default.Psychology,
                        description = "Cognitive Behavioral Therapy specifically structured for premenstrual distress to build symptom coping mechanisms, emotional regulation, and cognitive reframing techniques.",
                        clinicalCategory = "Psychological Interventions"
                    ),
                    TreatmentCardModel(
                        title = "Nutritional & Complementary Modalities",
                        categoryBadge = "NUTRITION & LIFESTYLE",
                        icon = Icons.Default.Spa,
                        description = "Calcium carbonate (1200 mg/day), Vitamin B6 (50–100 mg/day), Chasteberry (Vitex agnus-castus), regular aerobic physical activity, and sleep rhythm stabilization.",
                        clinicalCategory = "Supportive & Lifestyle"
                    )
                )
                com.example.engine.CpassProspectiveDiagnosis.PREMENSTRUAL_EXACERBATION -> listOf(
                    TreatmentCardModel(
                        title = "Underlying Condition Optimization",
                        categoryBadge = "PRIMARY RX",
                        icon = Icons.Default.Psychology,
                        description = "Optimize primary psychiatric pharmacotherapy (continuous antidepressant, mood stabilizer, or anxiolytic) for the baseline disorder (MDD, GAD, PTSD, Bipolar).",
                        clinicalCategory = "Primary Disorder Control"
                    ),
                    TreatmentCardModel(
                        title = "Luteal Phase Dose Augmentation",
                        categoryBadge = "AUGMENTATION",
                        icon = Icons.Default.Medication,
                        description = "Consult with prescribing physician regarding temporary premenstrual dosage adjustments of current medications during the symptomatic luteal window.",
                        clinicalCategory = "Premenstrual Titration"
                    ),
                    TreatmentCardModel(
                        title = "Behavioral & Coping Support",
                        categoryBadge = "COPING & THERAPY",
                        icon = Icons.Default.Lightbulb,
                        description = "Targeted cognitive psychotherapy focusing on distress tolerance, symptom tracking, and distinguishing baseline illness fluctuations from menstrual spikes.",
                        clinicalCategory = "Distress Tolerance"
                    )
                )
                else -> listOf(
                    TreatmentCardModel(
                        title = "Prospective Daily Charting",
                        categoryBadge = "TRACKING",
                        icon = Icons.Default.FactCheck,
                        description = "Continue logging DRSP daily ratings every evening across 2 consecutive cycles to establish definitive luteal surge and follicular clearance patterns.",
                        clinicalCategory = "Diagnostic Charting"
                    ),
                    TreatmentCardModel(
                        title = "Lifestyle & Sleep Optimization",
                        categoryBadge = "LIFESTYLE",
                        icon = Icons.Default.Spa,
                        description = "Maintain consistent circadian sleep rhythms, moderate aerobic exercise, and balanced complex-carbohydrate nutrition to support baseline neuroendocrine health.",
                        clinicalCategory = "Circadian Rhythm"
                    ),
                    TreatmentCardModel(
                        title = "Physician Consultation",
                        categoryBadge = "CLINICAL REVIEW",
                        icon = Icons.Default.MedicalServices,
                        description = "Review daily charting trends and C-PASS diagnostic matrix with your OB/GYN or primary healthcare provider for personalized guidance and support.",
                        clinicalCategory = "Collaborative Care"
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section Title & Carousel Navigation Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Treatment & Care Planning",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Carousel navigation arrows
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                val prevIndex = (treatmentCarouselListState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                                coroutineScope.launch {
                                    treatmentCarouselListState.animateScrollToItem(prevIndex)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Option",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable {
                                val nextIndex = (treatmentCarouselListState.firstVisibleItemIndex + 1).coerceAtMost(treatmentCardList.size - 1)
                                coroutineScope.launch {
                                    treatmentCarouselListState.animateScrollToItem(nextIndex)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Option",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Horizontal Carousel
            LazyRow(
                state = treatmentCarouselListState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("treatment_options_carousel")
            ) {
                itemsIndexed(treatmentCardList) { index, cardItem ->
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillParentMaxWidth(0.90f)
                            .testTag("treatment_carousel_card_$index")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = cardItem.icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "OPTION ${index + 1} OF ${treatmentCardList.size}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.8.sp
                                        )
                                        Text(
                                            text = cardItem.clinicalCategory,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = cardItem.categoryBadge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.6.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = cardItem.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = cardItem.description,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Carousel Dots Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                treatmentCardList.forEachIndexed { index, _ ->
                    val isSelected = treatmentCarouselListState.firstVisibleItemIndex == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(7.dp)
                            .width(if (isSelected) 22.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                            .clickable {
                                coroutineScope.launch {
                                    treatmentCarouselListState.animateScrollToItem(index)
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Dedicated Card: Physician Sharing & Confidentiality Note
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Physician Sharing & Confidentiality Note",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Clinical Sharing: Please share this report with your OB/GYN, Psychiatrist, or healthcare provider. Diagnostic confirmation and treatment decisions must be made in consultation with a licensed clinician.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Confidentiality: All daily symptom logs, C-PASS metrics, and reports are computed and stored privately on your device. No personal health information (PHI) is shared without your direct authorization.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
