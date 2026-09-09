package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DrspDailyLog
import com.example.viewmodel.PmddVitalsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DrspDailyLogScreen(
    viewModel: PmddVitalsViewModel,
    modifier: Modifier = Modifier
) {
    val allLogs by viewModel.allLogs.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var activeLog by remember(selectedDate, allLogs) {
        mutableStateOf(
            allLogs.find { it.date == selectedDate } ?: DrspDailyLog(date = selectedDate, cycleDay = 26)
        )
    }

    val flows = listOf("None", "Spotting", "Light", "Medium", "Heavy")

    val drspCategories = listOf(
        "Depression & Hopelessness" to (activeLog.depressedMood to { v: Int -> activeLog = activeLog.copy(depressedMood = v) }),
        "Anxiety & Tension" to (activeLog.anxietyTension to { v: Int -> activeLog = activeLog.copy(anxietyTension = v) }),
        "Mood Swings & Tearfulness" to (activeLog.moodSwings to { v: Int -> activeLog = activeLog.copy(moodSwings = v) }),
        "Anger & Irritability" to (activeLog.angerIrritability to { v: Int -> activeLog = activeLog.copy(angerIrritability = v) }),
        "Decreased Interest" to (activeLog.decreasedInterest to { v: Int -> activeLog = activeLog.copy(decreasedInterest = v) }),
        "Fatigue & Lethargy" to (activeLog.lethargyFatigue to { v: Int -> activeLog = activeLog.copy(lethargyFatigue = v) }),
        "Food Cravings & Appetite" to (activeLog.increasedAppetiteCravings to { v: Int -> activeLog = activeLog.copy(increasedAppetiteCravings = v) }),
        "Breast Tenderness & Bloating" to (activeLog.physicalBreastTenderness to { v: Int -> activeLog = activeLog.copy(physicalBreastTenderness = v) })
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Text(
                text = "Daily DRSP Symptom Log",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Daily Record of Severity of Problems (DSM-5 Standard)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Date Strip Selector
        item {
            val dates = remember {
                (0..6).map { LocalDate.now().minusDays(it.toLong()) }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dates) { dateObj ->
                    val dateStr = dateObj.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val isSelected = dateStr == selectedDate
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewModel.setSelectedDate(dateStr) }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = dateObj.dayOfWeek.name.take(3),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateObj.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Menstrual Bleeding / Flow
        item {
            Text(
                text = "MENSTRUAL BLEEDING / FLOW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.0.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(flows) { flow ->
                    val isSelected = activeLog.bleedingFlow == flow
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                activeLog = activeLog.copy(
                                    bleedingFlow = flow,
                                    isBleeding = flow != "None"
                                )
                                viewModel.saveLog(activeLog)
                            }
                    ) {
                        Text(
                            text = flow,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Symptom Sliders
        items(drspCategories) { (title, pair) ->
            val (currentScore, onScoreChange) = pair
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$currentScore / 6",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentScore >= 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (1..6).forEach { score ->
                            val isSelected = currentScore == score
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onScoreChange(score)
                                        viewModel.saveLog(activeLog)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = score.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = { viewModel.saveLog(activeLog) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Save Daily DRSP Log", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
