package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrspDsm5Directory
import com.example.ui.components.getDrspScoreColor
import com.example.viewmodel.PmddViewModel

enum class TrendTimeframe(val title: String, val shortLabel: String) {
    CURRENT_CYCLE("Current Cycle", "Cycle"),
    LUTEAL_14D("14-Day Luteal Window", "14 Days"),
    WEEK_7D("Past 7 Days", "7 Days"),
    ALL_ENTRIES("All Logged Cycles", "All Time")
}

@Composable
fun CycleTrendsScreen(
    viewModel: PmddViewModel,
    modifier: Modifier = Modifier
) {
    val allLogs by viewModel.allLogs.collectAsState()
    val sortedLogs = remember(allLogs) { allLogs.sortedBy { it.dateString } }

    var selectedDomainId by remember { mutableStateOf(0) } // 0 = Overall Mean, 1..11 = Specific DSM Domain
    var selectedTimeframe by remember { mutableStateOf(TrendTimeframe.CURRENT_CYCLE) }
    var isEntriesExpanded by remember { mutableStateOf(false) }
    var visibleEntriesCount by remember { mutableIntStateOf(5) }

    // Filter logs dynamically based on the selected timeframe rather than locking to 30 days
    val filteredLogs = remember(sortedLogs, selectedTimeframe) {
        if (sortedLogs.isEmpty()) emptyList()
        else when (selectedTimeframe) {
            TrendTimeframe.CURRENT_CYCLE -> {
                // Find latest bleeding day to anchor start of current cycle
                val lastBleedIdx = sortedLogs.indexOfLast { it.isBleeding }
                if (lastBleedIdx >= 0) {
                    // Include from the bleeding onset or up to 28 days
                    sortedLogs.subList(lastBleedIdx.coerceAtLeast(0), sortedLogs.size)
                } else {
                    sortedLogs.takeLast(14)
                }
            }
            TrendTimeframe.LUTEAL_14D -> sortedLogs.takeLast(14)
            TrendTimeframe.WEEK_7D -> sortedLogs.takeLast(7)
            TrendTimeframe.ALL_ENTRIES -> sortedLogs
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("cycle_trends_screen")
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "DRSP SEVERITY TRAJECTORY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "Tracking luteal surge (Days -7..-1) and follicular remission (Days +4..+10)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Timeframe Range Selector Chips (Cycle, 14D, 7D, All Time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrendTimeframe.values().forEach { timeframe ->
                    FilterChip(
                        selected = selectedTimeframe == timeframe,
                        onClick = { selectedTimeframe = timeframe },
                        label = {
                            Text(
                                text = timeframe.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selectedTimeframe == timeframe) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("timeframe_chip_${timeframe.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Domain Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedDomainId == 0,
                        onClick = { selectedDomainId = 0 },
                        label = { Text("Overall Mean", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("trend_chip_overall")
                    )
                }
                items(DrspDsm5Directory.DOMAINS, key = { it.domainId }) { domain ->
                    FilterChip(
                        selected = selectedDomainId == domain.domainId,
                        onClick = { selectedDomainId = domain.domainId },
                        label = { Text(domain.title, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("trend_chip_${domain.code}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Canvas Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outlineVariant),
                    width = 1.dp
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = if (selectedDomainId == 0) "Daily Severity Trajectory" else DrspDsm5Directory.DOMAINS.find { it.domainId == selectedDomainId }?.title ?: "",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${selectedTimeframe.title} • ${filteredLogs.size} days charted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Score >= 4 = Threshold",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No data points in this timeframe. Save daily DRSP logs or load demo data.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Plot Trend Curve using filtered timeframe logs
                        val scores = filteredLogs.map { log ->
                            if (selectedDomainId == 0) log.dailyMeanSeverity else log.getDomainScore(selectedDomainId).toFloat()
                        }
                        val bleedingPoints = filteredLogs.map { it.isBleeding }

                        val primaryColor = MaterialTheme.colorScheme.primary
                        val gridColor = MaterialTheme.colorScheme.outlineVariant
                        val thresholdColor = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        val bleedingMarkerColor = MaterialTheme.colorScheme.error

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .testTag("trend_curve_canvas")
                        ) {
                            val w = size.width
                            val h = size.height
                            val maxScore = 6f
                            val minScore = 1f

                            // Draw horizontal grid lines (1 to 6)
                            for (s in 1..6) {
                                val y = h - ((s - minScore) / (maxScore - minScore)) * h
                                drawLine(
                                    color = if (s == 4) thresholdColor else gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = if (s == 4) 2.dp.toPx() else 1.dp.toPx()
                                )
                            }

                            if (scores.size > 1) {
                                val stepX = w / (scores.size - 1)
                                val path = Path()

                                scores.forEachIndexed { i, score ->
                                    val x = i * stepX
                                    val y = h - ((score.coerceIn(1f, 6f) - minScore) / (maxScore - minScore)) * h
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                                    // Draw Bleeding marker at bottom
                                    if (bleedingPoints[i]) {
                                        drawCircle(
                                            color = bleedingMarkerColor,
                                            radius = 4.dp.toPx(),
                                            center = Offset(x, h - 6.dp.toPx())
                                        )
                                    }

                                    // Draw Point circle
                                    drawCircle(
                                        color = if (score >= 4f) thresholdColor else primaryColor,
                                        radius = 3.5.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = primaryColor,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )
                            } else if (scores.size == 1) {
                                val y = h - ((scores[0].coerceIn(1f, 6f) - minScore) / (maxScore - minScore)) * h
                                drawCircle(
                                    color = primaryColor,
                                    radius = 5.dp.toPx(),
                                    center = Offset(w / 2, y)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = filteredLogs.firstOrNull()?.dateString ?: "Start",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bleeding Days", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = filteredLogs.lastOrNull()?.dateString ?: "Latest",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Daily Logs History Section - Collapsible with pagination
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.outlineVariant)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isEntriesExpanded = !isEntriesExpanded }
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "DAILY DRSP ENTRIES (${sortedLogs.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isEntriesExpanded) "Showing up to $visibleEntriesCount entries" else "Tap to expand and view past logs",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    viewModel.navigateTo(com.example.viewmodel.AppScreen.LOG_DRSP)
                                }
                            ) {
                                Text(
                                    text = "+ Log",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (isEntriesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isEntriesExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { isEntriesExpanded = !isEntriesExpanded }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isEntriesExpanded) {
            val reversedLogs = sortedLogs.reversed()
            val entriesToShow = reversedLogs.take(visibleEntriesCount)

            items(entriesToShow, key = { it.dateString }) { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            viewModel.editLogForDate(log.dateString)
                        }
                        .testTag("recent_log_entry_${log.dateString}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(MaterialTheme.colorScheme.outlineVariant)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = log.dateString,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (log.cycleDay != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Day ${log.cycleDay}",
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (log.isBleeding) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = log.getFlow().label,
                                            color = MaterialTheme.colorScheme.onError,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Affective Max: ${log.core4DomainScores.maxOrNull() ?: 1}/6 • Somatic Max: ${log.all11DomainScores.maxOrNull() ?: 1}/6 • Impairment: ${log.maxImpairmentScore}/6",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (log.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Notes: ${log.notes}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = getDrspScoreColor((log.dailyMeanSeverity + 0.5f).toInt().coerceIn(1, 6)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = String.format("%.1f", log.dailyMeanSeverity),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    viewModel.editLogForDate(log.dateString)
                                }
                            ) {
                                Text(
                                    text = "Edit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // See More / Show Less pagination buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (visibleEntriesCount < sortedLogs.size) {
                        OutlinedButton(
                            onClick = { visibleEntriesCount += 5 },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("see_more_entries_btn")
                        ) {
                            Text("See More (+5 Entries)", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (visibleEntriesCount > 5) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { visibleEntriesCount = 5 },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reset to 5")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
