package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class ConditionChartType {
    PMDD,
    PMS,
    PME,
    MDD,
    GAD
}

data class DifferentialDiagnosisItem(
    val badgeTitle: String,
    val fullName: String,
    val chartType: ConditionChartType,
    val icon: ImageVector,
    val badgeBgColor: @Composable () -> Color,
    val badgeTextColor: @Composable () -> Color,
    val chartColor: Color,
    val cyclicalPattern: String,
    val symptomProfile: String,
    val keyDistinction: String,
    val follicularClearanceText: String,
    val follicularClearancePassed: Boolean,
    val severityLevel: String,
    val peakScoreLabel: String,
    val baselineScoreLabel: String
)

@Composable
fun CpassGuideScreen(
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val carouselListState = rememberLazyListState()
    var selectedDiffIdx by remember { mutableIntStateOf(0) }

    LaunchedEffect(carouselListState.firstVisibleItemIndex) {
        selectedDiffIdx = carouselListState.firstVisibleItemIndex.coerceIn(0, 4)
    }

    val differentialItems = remember {
        listOf(
            DifferentialDiagnosisItem(
                badgeTitle = "PMDD",
                fullName = "Premenstrual Dysphoric Disorder",
                chartType = ConditionChartType.PMDD,
                icon = Icons.Default.Psychology,
                badgeBgColor = { MaterialTheme.colorScheme.primary },
                badgeTextColor = { MaterialTheme.colorScheme.onPrimary },
                chartColor = Color(0xFFD32F2F),
                cyclicalPattern = "Strict On/Off Cycle",
                symptomProfile = "Severe affective surge (rage, despair, panic) in luteal phase with marked functional impairment.",
                keyDistinction = "100% full follicular remission (Days +4..+10). Symptoms switch on post-ovulation and cease at menses.",
                follicularClearanceText = "Full Remission (<= 2)",
                follicularClearancePassed = true,
                severityLevel = "Severe & Cyclical",
                peakScoreLabel = "5-6 / 6 (Severe)",
                baselineScoreLabel = "1-2 / 6 (Clear)"
            ),
            DifferentialDiagnosisItem(
                badgeTitle = "PMS",
                fullName = "Premenstrual Syndrome",
                chartType = ConditionChartType.PMS,
                icon = Icons.Default.Timeline,
                badgeBgColor = { MaterialTheme.colorScheme.secondary },
                badgeTextColor = { MaterialTheme.colorScheme.onSecondary },
                chartColor = Color(0xFF0288D1),
                cyclicalPattern = "Mild Cyclical Swell",
                symptomProfile = "Predominantly physical discomfort (bloating, fatigue, mild mood shifts) without disabling distress.",
                keyDistinction = "Does NOT reach clinical DRSP impairment threshold (>= 4). Daily life and work remain intact.",
                follicularClearanceText = "Full Remission (<= 2)",
                follicularClearancePassed = true,
                severityLevel = "Mild to Moderate",
                peakScoreLabel = "2-3 / 6 (Mild)",
                baselineScoreLabel = "1 / 6 (Clear)"
            ),
            DifferentialDiagnosisItem(
                badgeTitle = "PME",
                fullName = "Premenstrual Exacerbation",
                chartType = ConditionChartType.PME,
                icon = Icons.Default.Warning,
                badgeBgColor = { MaterialTheme.colorScheme.tertiary },
                badgeTextColor = { MaterialTheme.colorScheme.onTertiary },
                chartColor = Color(0xFFF57C00),
                cyclicalPattern = "High Baseline + Surge",
                symptomProfile = "Underlying disorder (MDD, GAD, PTSD, Bipolar) that noticeably spikes 7-10 days premenstrually.",
                keyDistinction = "Fails follicular remission. Baseline remains chronically elevated (DRSP > 3) throughout the month.",
                follicularClearanceText = "Fails Clearance (>= 4)",
                follicularClearancePassed = false,
                severityLevel = "Continuous + Spike",
                peakScoreLabel = "5-6 / 6 (Spike)",
                baselineScoreLabel = "3-4 / 6 (Elevated)"
            ),
            DifferentialDiagnosisItem(
                badgeTitle = "MDD",
                fullName = "Major Depressive Disorder",
                chartType = ConditionChartType.MDD,
                icon = Icons.Default.Schedule,
                badgeBgColor = { MaterialTheme.colorScheme.error },
                badgeTextColor = { MaterialTheme.colorScheme.onError },
                chartColor = Color(0xFF7B1FA2),
                cyclicalPattern = "Continuous Acyclic",
                symptomProfile = "Pervasive low mood, anhedonia, fatigue, and worthlessness persisting across weeks/months.",
                keyDistinction = "No correlation with menstrual phases. Depressive symptoms do not reliably lift at menses onset.",
                follicularClearanceText = "No Remission",
                follicularClearancePassed = false,
                severityLevel = "Chronic Episode",
                peakScoreLabel = "4-5 / 6 (Sustained)",
                baselineScoreLabel = "4-5 / 6 (Continuous)"
            ),
            DifferentialDiagnosisItem(
                badgeTitle = "GAD",
                fullName = "Generalized Anxiety Disorder",
                chartType = ConditionChartType.GAD,
                icon = Icons.Default.Analytics,
                badgeBgColor = { MaterialTheme.colorScheme.outline },
                badgeTextColor = { MaterialTheme.colorScheme.surface },
                chartColor = Color(0xFF455A64),
                cyclicalPattern = "Stress-Driven Jitter",
                symptomProfile = "Uncontrollable worry, muscle tension, restlessness, and insomnia across diverse life triggers.",
                keyDistinction = "Anxiety fluctuates with external stressors rather than biological ovulation and luteal hormone drops.",
                follicularClearanceText = "No Cycle Link",
                follicularClearancePassed = false,
                severityLevel = "Persistent Worry",
                peakScoreLabel = "4-5 / 6 (Stress peaks)",
                baselineScoreLabel = "3-4 / 6 (Ongoing)"
            )
        )
    }

    val safeDiffIdx = selectedDiffIdx.coerceIn(0, differentialItems.lastIndex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("cpass_guide_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(14.dp))

            // Clean Header: "Your Guide" / "Compare 5 Conditions"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "YOUR GUIDE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Differential Diagnosis",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = "${safeDiffIdx + 1} of ${differentialItems.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Enlarged Differential Diagnosis Horizontal Carousel
            LazyRow(
                state = carouselListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("differential_diagnosis_carousel"),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
            ) {
                itemsIndexed(differentialItems) { index, item ->
                    val isSelected = index == safeDiffIdx
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .width(345.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .clickable { selectedDiffIdx = index }
                            .testTag("differential_card_${item.badgeTitle.lowercase()}")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Top Row: Title, Icon, Badge
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
                                        color = item.badgeBgColor(),
                                        shape = CircleShape,
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = null,
                                                tint = item.badgeTextColor(),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.fullName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = item.severityLevel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = item.badgeBgColor(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = item.badgeTitle,
                                        color = item.badgeTextColor(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // --- VISUAL SYMPTOM TRAJECTORY CHART ---
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                ConditionMiniChart(
                                    chartType = item.chartType,
                                    chartColor = item.chartColor,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Visual Key Indicator Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "PEAK SEVERITY",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = item.peakScoreLabel,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = item.chartColor
                                        )
                                    }
                                }

                                Surface(
                                    color = if (item.follicularClearancePassed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "FOLLICULAR PHASE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = item.follicularClearanceText,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.follicularClearancePassed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Key Distinction Banner
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CLINICAL DIFFERENTIATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.keyDistinction,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Carousel Navigation Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                differentialItems.forEachIndexed { dotIdx, _ ->
                    val isCurrent = dotIdx == safeDiffIdx
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isCurrent) 22.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            .clickable {
                                selectedDiffIdx = dotIdx
                                coroutineScope.launch {
                                    carouselListState.animateScrollToItem(dotIdx)
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Essential Clinical Insights Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Key Biological Takeaway",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "PMDD is not caused by abnormal hormone levels. Patients have normal progesterone and estrogen levels, but suffer an abnormal neurobiological sensitivity in GABA-A receptors when allopregnanolone levels drop.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

/**
 * Custom Canvas Chart illustrating the 28-day symptom trajectory curve
 * with marked cycle phases (Follicular, Ovulation, Luteal, Menses).
 */
@Composable
fun ConditionMiniChart(
    chartType: ConditionChartType,
    chartColor: Color,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val bottomPadding = 24.dp.toPx()
        val topPadding = 12.dp.toPx()
        val startPadding = 12.dp.toPx()
        val endPadding = 12.dp.toPx()

        val plotWidth = width - startPadding - endPadding
        val plotHeight = height - topPadding - bottomPadding

        // Threshold cutoff line (y = 4 / severe threshold)
        val cutoffY = topPadding + plotHeight * (1f - (4f - 1f) / 5f)
        drawLine(
            color = Color.Gray.copy(alpha = 0.35f),
            start = Offset(startPadding, cutoffY),
            end = Offset(width - endPadding, cutoffY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        )

        // Ovulation vertical marker line (Day 14, at 50% width)
        val ovulX = startPadding + plotWidth * 0.48f
        drawLine(
            color = gridColor,
            start = Offset(ovulX, topPadding),
            end = Offset(ovulX, height - bottomPadding),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
        )

        // Luteal zone shaded background (Days 15..28)
        val lutealStartX = ovulX
        val lutealWidth = (width - endPadding) - lutealStartX
        drawRect(
            color = chartColor.copy(alpha = 0.06f),
            topLeft = Offset(lutealStartX, topPadding),
            size = androidx.compose.ui.geometry.Size(lutealWidth, plotHeight)
        )

        // Generate smooth curve points based on condition
        val points = mutableListOf<Offset>()
        val numPoints = 28
        for (day in 1..numPoints) {
            val normX = (day - 1).toFloat() / (numPoints - 1).toFloat()
            val x = startPadding + normX * plotWidth

            val score: Float = when (chartType) {
                ConditionChartType.PMDD -> {
                    // Days 1..13: baseline 1.1; Days 14..27: steep surge to 5.8; Day 28: sudden plunge
                    if (day <= 13) {
                        1.1f
                    } else if (day in 14..18) {
                        1.1f + (day - 13) * 0.7f
                    } else if (day in 19..26) {
                        5.8f
                    } else if (day == 27) {
                        5.0f
                    } else {
                        1.2f // Plunge at menses
                    }
                }
                ConditionChartType.PMS -> {
                    // Days 1..14: baseline 1.0; Days 15..26: mild bump to 2.6; Day 28: baseline 1.0
                    if (day <= 14) {
                        1.0f
                    } else if (day in 15..25) {
                        1.0f + 1.6f * kotlin.math.sin(((day - 14).toFloat() / 12f) * Math.PI.toFloat())
                    } else {
                        1.0f
                    }
                }
                ConditionChartType.PME -> {
                    // Days 1..13: high baseline 3.4; Days 14..26: surge to 5.7; Day 28: returns only to 3.4
                    if (day <= 13) {
                        3.4f
                    } else if (day in 14..26) {
                        3.4f + 2.3f * kotlin.math.sin(((day - 13).toFloat() / 13f) * Math.PI.toFloat())
                    } else {
                        3.4f
                    }
                }
                ConditionChartType.MDD -> {
                    // Continuous sustained elevation around 4.8 with slight natural waviness
                    4.8f + 0.3f * kotlin.math.sin((day.toFloat() / 3f))
                }
                ConditionChartType.GAD -> {
                    // Jittery stress fluctuations between 3.2 and 4.9 across entire month
                    val jitter = if (day % 3 == 0) 0.8f else if (day % 2 == 0) -0.5f else 0.2f
                    (3.8f + jitter).coerceIn(1f, 6f)
                }
            }

            val clampedScore = score.coerceIn(1f, 6f)
            val normY = (clampedScore - 1f) / 5f
            val y = topPadding + plotHeight * (1f - normY)
            points.add(Offset(x, y))
        }

        // Draw Filled Gradient under curve
        val fillPath = Path().apply {
            moveTo(startPadding, topPadding + plotHeight)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(width - endPadding, topPadding + plotHeight)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    chartColor.copy(alpha = 0.35f),
                    chartColor.copy(alpha = 0.05f)
                ),
                startY = topPadding,
                endY = topPadding + plotHeight
            )
        )

        // Draw Line
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) / 2f
                val midY = (prev.y + curr.y) / 2f
                quadraticTo(prev.x, prev.y, midX, midY)
            }
            lineTo(points.last().x, points.last().y)
        }

        drawPath(
            path = linePath,
            color = chartColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Key Data Points
        val peakPoint = points.maxByOrNull { topPadding + plotHeight - it.y }
        if (peakPoint != null) {
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = peakPoint
            )
            drawCircle(
                color = chartColor,
                radius = 3.5.dp.toPx(),
                center = peakPoint
            )
        }

        // Bottom Phase Guide Line
        val baselineY = height - bottomPadding
        drawLine(
            color = axisColor,
            start = Offset(startPadding, baselineY),
            end = Offset(width - endPadding, baselineY),
            strokeWidth = 1.dp.toPx()
        )
    }
}
