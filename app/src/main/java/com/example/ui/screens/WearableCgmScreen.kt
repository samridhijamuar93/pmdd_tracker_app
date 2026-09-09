package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WearableHealthSyncManager
import com.example.model.BiometricCycleCorrelation
import com.example.model.CgmReadingPoint
import com.example.model.DailyBiometricTrendPoint
import com.example.model.DailyLogEntity
import com.example.model.DeviceConnectionInfo
import com.example.model.WearableDeviceType
import com.example.viewmodel.PmddViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WearableCgmScreen(
    viewModel: PmddViewModel,
    modifier: Modifier = Modifier
) {
    val activeLog by viewModel.activeDailyLog.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val isSyncing by viewModel.isSyncingDevices.collectAsState()
    val cgmTrace by viewModel.cgmIntradayTrace.collectAsState()
    val correlation by viewModel.biometricCorrelation.collectAsState()
    val selectedWearable by viewModel.selectedWearable.collectAsState()
    val selectedCgm by viewModel.selectedCgm.collectAsState()
    val allLogs by viewModel.allLogs.collectAsState()
    
    // Time Range selector state (24 Hours, 7 Days, 14 Days, 30 Days)
    var selectedTimeRange by remember { mutableStateOf(BiomarkerTimeRange.DAYS_30) }
    
    val activeBiometricSeries = remember(allLogs, selectedTimeRange) {
        when (selectedTimeRange) {
            BiomarkerTimeRange.HOURS_24 -> WearableHealthSyncManager.get24HourIntradaySeries(isLuteal = true)
            BiomarkerTimeRange.DAYS_7 -> WearableHealthSyncManager.getBiometricSeriesForRange(allLogs, 7)
            BiomarkerTimeRange.DAYS_14 -> WearableHealthSyncManager.getBiometricSeriesForRange(allLogs, 14)
            BiomarkerTimeRange.DAYS_30 -> WearableHealthSyncManager.get30DayBiometricSeries(allLogs)
        }
    }

    var showDeviceDialog by remember { mutableStateOf(false) }

    // Fallbacks if active log hasn't synced yet
    val displayRhr = activeLog.restingHeartRate ?: 68
    val displayHrv = activeLog.hrvRmssd ?: 44.0
    val displaySleep = activeLog.sleepDurationHours ?: 7.2
    val displayDeepSleep = activeLog.deepSleepMinutes ?: 55
    val displayBbt = activeLog.basalBodyTempF ?: 97.8
    val displaySteps = activeLog.stepCount ?: 7850

    if (showDeviceDialog) {
        DeviceManagerDialog(
            devices = connectedDevices,
            onToggleConnection = { viewModel.toggleDeviceConnection(it) },
            onDismiss = { showDeviceDialog = false }
        )
    }

    val displayCycleDay = activeLog.cycleDay ?: if (activeLog.isBleeding) 1 else 26
    val currentPhaseTitle = when {
        activeLog.isBleeding -> "Menses / Onset"
        displayCycleDay in 1..5 -> "Menses Phase"
        displayCycleDay in 6..13 -> "Follicular Phase"
        displayCycleDay in 14..15 -> "Ovulation Window"
        displayCycleDay in 16..20 -> "Early Luteal Phase"
        else -> "Late Luteal Phase (PMDD Window)"
    }

    var viewModeCarousel by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("wearables_cgm_screen")
    ) {
        // TOP SECTION: CURRENT CYCLE PHASE BAR
        item {
            Spacer(modifier = Modifier.height(14.dp))
            CurrentCyclePhaseHomeCard(
                phaseTitle = currentPhaseTitle,
                cycleDay = displayCycleDay,
                isBleeding = activeLog.isBleeding,
                isSyncing = isSyncing,
                onSyncClick = { viewModel.syncAllDevices() },
                onStepDay = { step -> viewModel.stepDay(step) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // TIME-RANGE SWITCHER & VIEW MODE TOGGLE
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time Horizon Switcher (24 Hours, 7 Days, 14 Days, 30 Days)
                TimeRangeSelectorRow(
                    selectedRange = selectedTimeRange,
                    onRangeSelected = { selectedTimeRange = it },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Toggle between Graph Carousel and Table View
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        IconButton(
                            onClick = { viewModeCarousel = true },
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (viewModeCarousel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewCarousel,
                                contentDescription = "Graph Carousel View",
                                tint = if (viewModeCarousel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModeCarousel = false },
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (!viewModeCarousel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = "Table View",
                                tint = if (!viewModeCarousel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (viewModeCarousel) {
                BiomarkerComparisonCarousel(
                    correlation = correlation,
                    seriesData = activeBiometricSeries,
                    selectedTimeRange = selectedTimeRange
                )
            } else {
                BiomarkerComparisonTable(correlation = correlation)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Luteal Phase Insulin Resistance Clinical Insight Callout
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Hormone Link",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Luteal Insulin Resistance & PMDD Cravings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Elevated luteal progesterone transiently reduces peripheral insulin sensitivity, driving sharp post-prandial glycemic excursions and reactive hypoglycemia. These dips trigger acute carbohydrate cravings (DRSP Item 14) and irritability.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // SECTION: APPLE WATCH & PIXEL WATCH BIOMETRICS SUMMARY CARDS
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MonitorHeart,
                    contentDescription = "Wearables",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pixel Watch & Apple Watch Biomarkers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 4 Grid Cards for Wearable Vitals
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Resting Heart Rate Card
                BiometricVitalCard(
                    title = "Resting Heart Rate",
                    value = "$displayRhr bpm",
                    trendText = "Luteal elevation (+7 bpm)",
                    icon = Icons.Default.Favorite,
                    accentColor = Color(0xFFE91E63),
                    modifier = Modifier.weight(1f)
                )

                // Heart Rate Variability (HRV) Card
                BiometricVitalCard(
                    title = "Heart Rate Var. (HRV)",
                    value = "${displayHrv.toInt()} ms",
                    trendText = "Vagal drop (-38% in luteal)",
                    icon = Icons.Default.Timeline,
                    accentColor = Color(0xFF7C4DFF),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sleep Architecture Card
                BiometricVitalCard(
                    title = "Sleep & Deep Stage",
                    value = "${displaySleep}h (${displayDeepSleep}m deep)",
                    trendText = "82% sleep efficiency",
                    icon = Icons.Default.Bedtime,
                    accentColor = Color(0xFF3F51B5),
                    modifier = Modifier.weight(1f)
                )

                // Basal Body Temp Card
                BiometricVitalCard(
                    title = "Basal Body Temp",
                    value = "$displayBbt °F",
                    trendText = "Biphasic luteal shift (+0.6°F)",
                    icon = Icons.Default.Thermostat,
                    accentColor = Color(0xFFFF7043),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Step Count & Activity Row
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "Steps",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Daily Steps & Movement",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$displaySteps steps • 38 active minutes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (displaySteps / 10000f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .width(80.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

// -------------------------------------------------------------
// CURRENT CYCLE PHASE HOME CARD
// -------------------------------------------------------------

@Composable
fun CurrentCyclePhaseHomeCard(
    phaseTitle: String,
    cycleDay: Int,
    isBleeding: Boolean,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onStepDay: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "home_sync_spin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "home_sync_rotation"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("current_cycle_phase_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Top Row: Phase Name & Live Sync Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CURRENT CYCLE PHASE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = phaseTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sync Action Button
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isSyncing) { onSyncClick() }
                        .testTag("sync_all_devices_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .then(if (isSyncing) Modifier.rotate(rotationAngle) else Modifier)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isSyncing) "Syncing" else "Sync",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day Indicator & Stepper Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "DAY $cycleDay OF 28",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onStepDay(-1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Day",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = if (cycleDay == 26) "Today" else "Day $cycleDay",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onStepDay(1) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Day",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4-Phase Progress Bar
            val safeCycleDay = cycleDay.coerceIn(1, 28)
            val currentFraction = (safeCycleDay - 1).toFloat() / 27f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                // Segmented Backgrounds
                Row(modifier = Modifier.fillMaxSize()) {
                    // Menses: Days 1-5 (5/28 ~ 18%)
                    Box(
                        modifier = Modifier
                            .weight(5f)
                            .fillMaxSize()
                            .background(Color(0xFFEF5350).copy(alpha = 0.35f))
                    )
                    // Follicular: Days 6-13 (8/28 ~ 28%)
                    Box(
                        modifier = Modifier
                            .weight(8f)
                            .fillMaxSize()
                            .background(Color(0xFF26A69A).copy(alpha = 0.35f))
                    )
                    // Ovulation: Days 14-15 (2/28 ~ 7%)
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .fillMaxSize()
                            .background(Color(0xFFAB47BC).copy(alpha = 0.35f))
                    )
                    // Luteal: Days 16-28 (13/28 ~ 47%)
                    Box(
                        modifier = Modifier
                            .weight(13f)
                            .fillMaxSize()
                            .background(Color(0xFFEC407A).copy(alpha = 0.35f))
                    )
                }

                // Active Progress Fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(currentFraction.coerceIn(0.04f, 1f))
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFEF5350),
                                    Color(0xFF26A69A),
                                    Color(0xFFAB47BC),
                                    MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Phase Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val isMensesActive = safeCycleDay in 1..5 || isBleeding
                val isFollicularActive = safeCycleDay in 6..13 && !isBleeding
                val isOvulationActive = safeCycleDay in 14..15
                val isLutealActive = safeCycleDay in 16..28

                Text(
                    text = "Menses (1-5)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = if (isMensesActive) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isMensesActive) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Follicular (6-13)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = if (isFollicularActive) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isFollicularActive) Color(0xFF00695C) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ovulation (14)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = if (isOvulationActive) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isOvulationActive) Color(0xFF6A1B9A) else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Luteal (15-28)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = if (isLutealActive) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isLutealActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clinical Phase Correlation Note
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (safeCycleDay >= 16) Color(0xFFE91E63) else Color(0xFF00897B),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            safeCycleDay in 1..5 || isBleeding -> "Menstrual Onset: Rapid symptom clearance & follicular baseline reset."
                            safeCycleDay in 6..13 -> "Follicular Window: High HRV vagal tone & optimal glycemic stability."
                            safeCycleDay in 14..15 -> "Pre-Ovulatory Window: Estrogen surge before progesterone transition."
                            else -> "Late Luteal Window: Progesterone drop, elevated RHR & increased glycemic variability."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// BIOMARKER TIME RANGE DEFINITION & SELECTOR
// -------------------------------------------------------------

enum class BiomarkerTimeRange(val label: String, val shortLabel: String) {
    HOURS_24("24 Hours", "24h"),
    DAYS_7("7 Days", "7d"),
    DAYS_14("14 Days", "14d"),
    DAYS_30("30 Days", "30d")
}

@Composable
fun TimeRangeSelectorRow(
    selectedRange: BiomarkerTimeRange,
    onRangeSelected: (BiomarkerTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BiomarkerTimeRange.entries.forEach { range ->
                val isSelected = range == selectedRange
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onRangeSelected(range) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = range.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BiomarkerComparisonCarousel(
    correlation: BiometricCycleCorrelation,
    seriesData: List<DailyBiometricTrendPoint>,
    selectedTimeRange: BiomarkerTimeRange
) {
    val coroutineScope = rememberCoroutineScope()
    val tabTitles = remember {
        listOf(
            "Overview",
            "Resting HR",
            "HRV Vagal Tone",
            "CGM Glucose %CV",
            "Deep Sleep",
            "Basal Temp"
        )
    }

    val pagerState = rememberPagerState(pageCount = { 6 })

    Column(modifier = Modifier.fillMaxWidth()) {
        // Quick Jump Category Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(tabTitles) { index, title ->
                val isSelected = pagerState.currentPage == index
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    label = {
                        Text(
                            text = title,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Pager for Biomarker Graphs
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("biomarker_graphs_carousel")
        ) { page ->
            when (page) {
                0 -> OverviewBiomarkerSlide(correlation = correlation, seriesData = seriesData, selectedTimeRange = selectedTimeRange)
                1 -> RhrBiomarkerSlide(correlation = correlation, seriesData = seriesData, selectedTimeRange = selectedTimeRange)
                2 -> HrvBiomarkerSlide(correlation = correlation, seriesData = seriesData, selectedTimeRange = selectedTimeRange)
                3 -> CgmVariabilityBiomarkerSlide(correlation = correlation, seriesData = seriesData, selectedTimeRange = selectedTimeRange)
                4 -> DeepSleepBiomarkerSlide(correlation = correlation, seriesData = seriesData, selectedTimeRange = selectedTimeRange)
                5 -> BbtBiomarkerSlide(correlation = correlation, seriesData = seriesData, selectedTimeRange = selectedTimeRange)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Carousel Bottom Controls (Prev, Indicators, Next)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (pagerState.currentPage > 0) {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                },
                enabled = pagerState.currentPage > 0
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Graph")
            }

            // Indicator Dots & Label
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(6) { index ->
                    val isCurrent = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (isCurrent) 8.dp else 6.dp)
                            .background(
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${pagerState.currentPage + 1} / 6",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = {
                    if (pagerState.currentPage < 5) {
                        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                enabled = pagerState.currentPage < 5
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Graph")
            }
        }
    }
}

@Composable
private fun OverviewBiomarkerSlide(
    correlation: BiometricCycleCorrelation,
    seriesData: List<DailyBiometricTrendPoint>,
    selectedTimeRange: BiomarkerTimeRange
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Equalizer, contentDescription = "Overview", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("${selectedTimeRange.label} Multi-Biomarker Series", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            when (selectedTimeRange) {
                                BiomarkerTimeRange.HOURS_24 -> "Continuous intraday 24-hour trace"
                                else -> "Continuous daily telemetry over ${selectedTimeRange.label.lowercase()}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        selectedTimeRange.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Multi-Biomarker Synchronized Dots Time Series Chart
            OverviewMultiBiomarker30DayChart(
                points = seriesData,
                selectedTimeRange = selectedTimeRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = correlation.clinicalSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun RhrBiomarkerSlide(
    correlation: BiometricCycleCorrelation,
    seriesData: List<DailyBiometricTrendPoint>,
    selectedTimeRange: BiomarkerTimeRange
) {
    BiomarkerSlideCard(
        title = "Resting Heart Rate (RHR)",
        subtitle = "Cardiovascular & adrenergic tone (${selectedTimeRange.label})",
        icon = Icons.Default.Favorite,
        accentColor = Color(0xFFE91E63),
        follicularText = "${correlation.follicularRhrMean} bpm",
        lutealText = "${correlation.lutealRhrMean} bpm",
        deltaText = "+${correlation.rhrLutealSurgePct}% Surge",
        isAdverseShift = true,
        normalRangeText = "Normal resting range: 60–75 bpm",
        graphContent = {
            ThirtyDayBiometricDotsTimeSeriesChart(
                points = seriesData,
                selectedTimeRange = selectedTimeRange,
                valueExtractor = { it.restingHeartRate },
                unit = "bpm",
                yMin = 55.0,
                yMax = 88.0,
                normalRangeMin = 60.0,
                normalRangeMax = 75.0,
                normalRangeLabel = "Optimal Zone (60-75 bpm)",
                lineColor = Color(0xFFE91E63),
                gradientColors = listOf(Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFFE91E63)),
                formatValue = { "${it.roundToInt()} bpm" },
                modifier = Modifier.fillMaxWidth()
            )
        },
        mechanismExplanation = "Progesterone metabolites stimulate central noradrenergic tone and reset baroreflex sensitivity, producing an elevation in resting pulse during the luteal window. In PMDD, this adrenergic shift exacerbates physical anxiety and palpitation sensations."
    )
}

@Composable
private fun HrvBiomarkerSlide(
    correlation: BiometricCycleCorrelation,
    seriesData: List<DailyBiometricTrendPoint>,
    selectedTimeRange: BiomarkerTimeRange
) {
    BiomarkerSlideCard(
        title = "Heart Rate Variability (HRV rMSSD)",
        subtitle = "Parasympathetic vagal tone (${selectedTimeRange.label})",
        icon = Icons.Default.Timeline,
        accentColor = Color(0xFF7C4DFF),
        follicularText = "${correlation.follicularHrvMean} ms",
        lutealText = "${correlation.lutealHrvMean} ms",
        deltaText = "-${correlation.hrvLutealDropPct}% Vagal Drop",
        isAdverseShift = true,
        normalRangeText = "Target >45 ms (higher is more resilient)",
        graphContent = {
            ThirtyDayBiometricDotsTimeSeriesChart(
                points = seriesData,
                selectedTimeRange = selectedTimeRange,
                valueExtractor = { it.hrvRmssd },
                unit = "ms",
                yMin = 15.0,
                yMax = 75.0,
                normalRangeMin = 45.0,
                normalRangeMax = 75.0,
                normalRangeLabel = "Resilience Zone (>45 ms)",
                lineColor = Color(0xFF7C4DFF),
                gradientColors = listOf(Color(0xFF7C4DFF), Color(0xFF536DFE), Color(0xFFFF5252)),
                formatValue = { String.format("%.1f ms", it) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        mechanismExplanation = "A substantial drop in rMSSD signals parasympathetic withdrawal and diminished fronto-amygdalar inhibitory control. This objective biomarker directly mirrors the subjective irritability, anger, and emotional volatility captured on DRSP items."
    )
}

@Composable
private fun CgmVariabilityBiomarkerSlide(
    correlation: BiometricCycleCorrelation,
    seriesData: List<DailyBiometricTrendPoint>,
    selectedTimeRange: BiomarkerTimeRange
) {
    BiomarkerSlideCard(
        title = "CGM Glycemic Variability (%CV)",
        subtitle = "Continuous glucose fluctuations (${selectedTimeRange.label})",
        icon = Icons.Default.WaterDrop,
        accentColor = Color(0xFF00897B),
        follicularText = "${correlation.follicularGlucoseCvMean}% CV",
        lutealText = "${correlation.lutealGlucoseCvMean}% CV",
        deltaText = "+${correlation.glucoseCvIncreasePct}% Instability",
        isAdverseShift = true,
        normalRangeText = "Clinical target: %CV < 33%",
        graphContent = {
            ThirtyDayBiometricDotsTimeSeriesChart(
                points = seriesData,
                selectedTimeRange = selectedTimeRange,
                valueExtractor = { it.cgmGlucoseCvPct },
                unit = "% CV",
                yMin = 12.0,
                yMax = 44.0,
                normalRangeMin = 12.0,
                normalRangeMax = 33.0,
                normalRangeLabel = "Clinical Target (<33% CV)",
                lineColor = Color(0xFF00897B),
                gradientColors = listOf(Color(0xFF00897B), Color(0xFF26A69A), Color(0xFFFF7043)),
                formatValue = { String.format("%.1f%%", it) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        mechanismExplanation = "Progesterone induces transient peripheral insulin resistance during the luteal phase. Higher glycemic excursions and post-prandial reactive hypoglycemic troughs trigger intense cravings for simple carbohydrates (DRSP Item 14) and rapid mood crashes."
    )
}

@Composable
private fun DeepSleepBiomarkerSlide(
    correlation: BiometricCycleCorrelation,
    seriesData: List<DailyBiometricTrendPoint>,
    selectedTimeRange: BiomarkerTimeRange
) {
    BiomarkerSlideCard(
        title = "Deep Sleep Architecture",
        subtitle = "Restorative slow-wave sleep (${selectedTimeRange.label})",
        icon = Icons.Default.Bedtime,
        accentColor = Color(0xFF3F51B5),
        follicularText = "${correlation.follicularDeepSleepMinMean.toInt()} min",
        lutealText = "${correlation.lutealDeepSleepMinMean.toInt()} min",
        deltaText = "-${correlation.deepSleepDropPct}% Loss",
        isAdverseShift = true,
        normalRangeText = "Target: 60–90 min/night",
        graphContent = {
            ThirtyDayBiometricDotsTimeSeriesChart(
                points = seriesData,
                selectedTimeRange = selectedTimeRange,
                valueExtractor = { it.deepSleepMinutes },
                unit = "min",
                yMin = 15.0,
                yMax = 100.0,
                normalRangeMin = 60.0,
                normalRangeMax = 95.0,
                normalRangeLabel = "Target (60-90 min)",
                lineColor = Color(0xFF3F51B5),
                gradientColors = listOf(Color(0xFF3F51B5), Color(0xFF5C6BC0), Color(0xFFE91E63)),
                formatValue = { "${it.roundToInt()} min" },
                modifier = Modifier.fillMaxWidth()
            )
        },
        mechanismExplanation = "Altered GABA-A receptor plasticity in response to fluctuating allopregnanolone levels disrupts delta-wave generation during N3 slow-wave sleep. Patients experience non-restorative sleep, frequent micro-arousals, and premenstrual daytime exhaustion."
    )
}

@Composable
private fun BbtBiomarkerSlide(
    correlation: BiometricCycleCorrelation,
    seriesData: List<DailyBiometricTrendPoint>,
    selectedTimeRange: BiomarkerTimeRange
) {
    BiomarkerSlideCard(
        title = "Basal Body Temperature (BBT)",
        subtitle = "Progesterone thermal shift (${selectedTimeRange.label})",
        icon = Icons.Default.Thermostat,
        accentColor = Color(0xFFFF7043),
        follicularText = "${correlation.follicularBbtMean} °F",
        lutealText = "${correlation.lutealBbtMean} °F",
        deltaText = "+0.76°F Biphasic Shift",
        isAdverseShift = false,
        normalRangeText = "Typical shift: +0.5°F to +0.9°F post-ovulation",
        graphContent = {
            ThirtyDayBiometricDotsTimeSeriesChart(
                points = seriesData,
                selectedTimeRange = selectedTimeRange,
                valueExtractor = { it.basalBodyTempF },
                unit = "°F",
                yMin = 96.8,
                yMax = 98.8,
                normalRangeMin = 97.4,
                normalRangeMax = 98.4,
                normalRangeLabel = "Coverline (97.8°F)",
                lineColor = Color(0xFFFF7043),
                gradientColors = listOf(Color(0xFF42A5F5), Color(0xFFFFB74D), Color(0xFFFF7043)),
                formatValue = { String.format("%.2f°F", it) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        mechanismExplanation = "Ovulation triggers corpus luteum progesterone secretion, elevating hypothalamic thermal setpoint. In PMDD assessment, this thermal shift verifies that luteal symptoms align strictly with the ovulatory cycle rather than continuous generalized mood disorders."
    )
}

@Composable
private fun BiomarkerSlideCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    follicularText: String,
    lutealText: String,
    deltaText: String,
    isAdverseShift: Boolean,
    normalRangeText: String,
    graphContent: @Composable () -> Unit,
    mechanismExplanation: String
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Surface(
                    color = if (isAdverseShift) Color(0xFFD32F2F).copy(alpha = 0.12f) else Color(0xFF2E7D32).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = deltaText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isAdverseShift) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Values comparison pill banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("FOLLICULAR PHASE", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(follicularText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    }
                    Text("vs", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(horizontalAlignment = Alignment.End) {
                        Text("LUTEAL PHASE", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(lutealText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE91E63))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 30-Day Dots Time Series Graph
            graphContent()

            Spacer(modifier = Modifier.height(8.dp))
            Text(normalRangeText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // PMDD Physiological Mechanism Card
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Mechanism",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mechanismExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DYNAMIC BIOMETRIC DOTS TIME-SERIES CHARTS (24h / 7d / 14d / 30d)
// -------------------------------------------------------------

@Composable
private fun ThirtyDayBiometricDotsTimeSeriesChart(
    points: List<DailyBiometricTrendPoint>,
    selectedTimeRange: BiomarkerTimeRange = BiomarkerTimeRange.DAYS_30,
    valueExtractor: (DailyBiometricTrendPoint) -> Double,
    unit: String,
    yMin: Double,
    yMax: Double,
    normalRangeMin: Double? = null,
    normalRangeMax: Double? = null,
    normalRangeLabel: String? = null,
    lineColor: Color,
    gradientColors: List<Color>,
    formatValue: (Double) -> String = { String.format("%.1f", it) },
    modifier: Modifier = Modifier
) {
    val safePoints = remember(points) { points }
    var selectedIndex by remember(safePoints.size) { 
        mutableIntStateOf(if (safePoints.size > 20) 23 else (safePoints.size - 1).coerceAtLeast(0)) 
    }
    val activePoint = safePoints.getOrNull(selectedIndex.coerceIn(0, (safePoints.size - 1).coerceAtLeast(0)))

    Column(modifier = modifier) {
        // Interactive Selected Day Inspector Pill
        if (activePoint != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = when {
                                activePoint.isBleeding -> Color(0xFFEF5350)
                                activePoint.isOvulation -> Color(0xFFAB47BC)
                                activePoint.isLuteal -> Color(0xFFEC407A)
                                else -> Color(0xFF1E88E5)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (selectedTimeRange == BiomarkerTimeRange.HOURS_24) activePoint.dateString else "Day ${activePoint.cycleDay}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedTimeRange == BiomarkerTimeRange.HOURS_24) "${activePoint.phaseName} Intraday" else "${activePoint.phaseName} (${activePoint.dateString})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = formatValue(valueExtractor(activePoint)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = lineColor
                    )
                }
            }
        }

        // Time Series Dots & Waveform Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFAFAFD))
                .pointerInput(safePoints) {
                    detectTapGestures { offset ->
                        val paddingStart = 24.dp.toPx()
                        val paddingEnd = 16.dp.toPx()
                        val chartW = size.width - paddingStart - paddingEnd
                        if (chartW > 0 && safePoints.isNotEmpty()) {
                            val stepX = chartW / (safePoints.size - 1).coerceAtLeast(1)
                            val relativeX = (offset.x - paddingStart).coerceIn(0f, chartW)
                            val closestIndex = (relativeX / stepX).roundToInt().coerceIn(0, safePoints.size - 1)
                            selectedIndex = closestIndex
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (safePoints.isEmpty()) return@Canvas

                val paddingStart = 28.dp.toPx()
                val paddingEnd = 16.dp.toPx()
                val paddingTop = 16.dp.toPx()
                val paddingBottom = 22.dp.toPx()

                val chartW = size.width - paddingStart - paddingEnd
                val chartH = size.height - paddingTop - paddingBottom
                val numPoints = safePoints.size
                val stepX = chartW / (numPoints - 1).coerceAtLeast(1)

                // 1. Shaded Menstrual Cycle Phases Background Bands (when full cycle or multiple days)
                if (selectedTimeRange == BiomarkerTimeRange.DAYS_30) {
                    // Days 1..5: Menses
                    val mensesEndIdx = (5 - 1).coerceIn(0, numPoints - 1)
                    val mensesW = mensesEndIdx * stepX
                    drawRect(
                        color = Color(0xFFEF5350).copy(alpha = 0.08f),
                        topLeft = Offset(paddingStart, paddingTop),
                        size = Size(mensesW, chartH)
                    )

                    // Days 6..13: Follicular
                    val folStartIdx = 5
                    val folEndIdx = 13
                    val folX = paddingStart + (folStartIdx - 1) * stepX
                    val folW = (folEndIdx - folStartIdx) * stepX
                    drawRect(
                        color = Color(0xFF42A5F5).copy(alpha = 0.06f),
                        topLeft = Offset(folX, paddingTop),
                        size = Size(folW, chartH)
                    )

                    // Days 15..28: Luteal / PMDD Window
                    val lutStartIdx = 14
                    val lutEndIdx = 28.coerceAtMost(numPoints)
                    val lutX = paddingStart + (lutStartIdx - 1) * stepX
                    val lutW = (lutEndIdx - lutStartIdx) * stepX
                    drawRect(
                        color = Color(0xFFEC407A).copy(alpha = 0.09f),
                        topLeft = Offset(lutX, paddingTop),
                        size = Size(lutW, chartH)
                    )

                    // Ovulation Marker (Day 14)
                    val ovIdx = 13 // 0-indexed for Day 14
                    val ovX = paddingStart + ovIdx * stepX
                    drawLine(
                        color = Color(0xFFAB47BC).copy(alpha = 0.45f),
                        start = Offset(ovX, paddingTop),
                        end = Offset(ovX, paddingTop + chartH),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                } else if (selectedTimeRange == BiomarkerTimeRange.HOURS_24) {
                    // Shaded overnight band (00:00 - 07:00 approx)
                    val sleepW = (7f / 24f) * chartW
                    drawRect(
                        color = Color(0xFF3F51B5).copy(alpha = 0.07f),
                        topLeft = Offset(paddingStart, paddingTop),
                        size = Size(sleepW, chartH)
                    )
                } else {
                    // For 7d & 14d: highlight luteal points
                    safePoints.forEachIndexed { idx, pt ->
                        if (pt.isLuteal) {
                            val xLeft = paddingStart + (idx - 0.5f).coerceAtLeast(0f) * stepX
                            val xW = stepX
                            drawRect(
                                color = Color(0xFFEC407A).copy(alpha = 0.06f),
                                topLeft = Offset(xLeft, paddingTop),
                                size = Size(xW, chartH)
                            )
                        }
                    }
                }

                // 2. Normal Reference Range Band (if present)
                if (normalRangeMin != null && normalRangeMax != null) {
                    val normTop = paddingTop + chartH - (((normalRangeMax - yMin) / (yMax - yMin)) * chartH).toFloat()
                    val normBottom = paddingTop + chartH - (((normalRangeMin - yMin) / (yMax - yMin)) * chartH).toFloat()
                    drawRect(
                        color = Color(0xFF4CAF50).copy(alpha = 0.08f),
                        topLeft = Offset(paddingStart, normTop),
                        size = Size(chartW, (normBottom - normTop).coerceAtLeast(1f))
                    )
                    drawLine(
                        color = Color(0xFF4CAF50).copy(alpha = 0.35f),
                        start = Offset(paddingStart, normTop),
                        end = Offset(paddingStart + chartW, normTop),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }

                // 3. Calculate Coordinates for all data points
                val pointCoords = safePoints.mapIndexed { idx, pt ->
                    val v = valueExtractor(pt).coerceIn(yMin, yMax)
                    val x = paddingStart + idx * stepX
                    val y = paddingTop + chartH - (((v - yMin) / (yMax - yMin)) * chartH).toFloat()
                    Offset(x, y)
                }

                // 4. Draw Gradient Fill Under Curve
                val fillPath = Path().apply {
                    if (pointCoords.isNotEmpty()) {
                        moveTo(pointCoords.first().x, paddingTop + chartH)
                        lineTo(pointCoords.first().x, pointCoords.first().y)
                        for (i in 0 until pointCoords.size - 1) {
                            val p0 = pointCoords[i]
                            val p1 = pointCoords[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                        lineTo(pointCoords.last().x, paddingTop + chartH)
                        close()
                    }
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.22f), Color.Transparent),
                        startY = paddingTop,
                        endY = paddingTop + chartH
                    )
                )

                // 5. Draw Continuous Time-Series Curve Line
                val linePath = Path().apply {
                    if (pointCoords.isNotEmpty()) {
                        moveTo(pointCoords.first().x, pointCoords.first().y)
                        for (i in 0 until pointCoords.size - 1) {
                            val p0 = pointCoords[i]
                            val p1 = pointCoords[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }
                }
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(gradientColors),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // 6. Draw EVERY SINGLE DATA DOT
                pointCoords.forEachIndexed { idx, coord ->
                    val pt = safePoints[idx]
                    val isSelected = idx == selectedIndex

                    val dotColor = when {
                        pt.isBleeding -> Color(0xFFEF5350)
                        pt.isOvulation -> Color(0xFFAB47BC)
                        pt.isLuteal -> Color(0xFFEC407A)
                        else -> Color(0xFF1E88E5)
                    }

                    if (isSelected) {
                        // Selected dot outer halo
                        drawCircle(
                            color = dotColor.copy(alpha = 0.25f),
                            radius = 9.dp.toPx(),
                            center = coord
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = coord
                        )
                        drawCircle(
                            color = dotColor,
                            radius = 4.5.dp.toPx(),
                            center = coord
                        )
                    } else {
                        // Standard dot
                        drawCircle(
                            color = Color.White,
                            radius = 3.5.dp.toPx(),
                            center = coord
                        )
                        drawCircle(
                            color = dotColor,
                            radius = 2.5.dp.toPx(),
                            center = coord
                        )
                    }
                }
            }
        }

        // Timeline Phase Legend & Horizon Marker Axis
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (selectedTimeRange) {
                BiomarkerTimeRange.HOURS_24 -> {
                    Text("00:00 (Sleep)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("06:00 (Waking)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("12:00 (Midday)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("18:00 (Evening)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("23:00", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BiomarkerTimeRange.DAYS_7 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFF1E88E5), CircleShape))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Day 1 (7 Days Ago)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFFEC407A), CircleShape))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Day 7 (Today)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                BiomarkerTimeRange.DAYS_14 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFF1E88E5), CircleShape))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("14 Days Ago (Mid-Cycle)", style = MaterialTheme.typography.labelSmall, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFFEC407A), CircleShape))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Today (Luteal PMDD Window)", style = MaterialTheme.typography.labelSmall, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                BiomarkerTimeRange.DAYS_30 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFFEF5350), CircleShape))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("D1-5 Menses", style = MaterialTheme.typography.labelSmall, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFF1E88E5), CircleShape))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("D6-13 Follicular", style = MaterialTheme.typography.labelSmall, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFFAB47BC), CircleShape))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("D14 Ovulation", style = MaterialTheme.typography.labelSmall, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).background(Color(0xFFEC407A), CircleShape))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("D15-28 Luteal", style = MaterialTheme.typography.labelSmall, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewMultiBiomarker30DayChart(
    points: List<DailyBiometricTrendPoint>,
    selectedTimeRange: BiomarkerTimeRange = BiomarkerTimeRange.DAYS_30,
    modifier: Modifier = Modifier
) {
    val safePoints = remember(points) { points }
    var selectedIndex by remember(safePoints.size) { 
        mutableIntStateOf(if (safePoints.size > 20) 23 else (safePoints.size - 1).coerceAtLeast(0)) 
    }
    val activePoint = safePoints.getOrNull(selectedIndex.coerceIn(0, (safePoints.size - 1).coerceAtLeast(0)))

    Column(modifier = modifier) {
        if (activePoint != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedTimeRange == BiomarkerTimeRange.HOURS_24) "${activePoint.dateString} • ${activePoint.phaseName}" else "Day ${activePoint.cycleDay} • ${activePoint.phaseName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "RHR: ${activePoint.restingHeartRate.roundToInt()} bpm",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE91E63)
                        )
                        Text(
                            text = "HRV: ${String.format("%.0f", activePoint.hrvRmssd)} ms",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7C4DFF)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFAFAFD))
                .pointerInput(safePoints) {
                    detectTapGestures { offset ->
                        val paddingStart = 24.dp.toPx()
                        val paddingEnd = 16.dp.toPx()
                        val chartW = size.width - paddingStart - paddingEnd
                        if (chartW > 0 && safePoints.isNotEmpty()) {
                            val stepX = chartW / (safePoints.size - 1).coerceAtLeast(1)
                            val relativeX = (offset.x - paddingStart).coerceIn(0f, chartW)
                            val closestIndex = (relativeX / stepX).roundToInt().coerceIn(0, safePoints.size - 1)
                            selectedIndex = closestIndex
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (safePoints.isEmpty()) return@Canvas

                val paddingStart = 28.dp.toPx()
                val paddingEnd = 16.dp.toPx()
                val paddingTop = 12.dp.toPx()
                val paddingBottom = 16.dp.toPx()

                val chartW = size.width - paddingStart - paddingEnd
                val chartH = size.height - paddingTop - paddingBottom
                val numPoints = safePoints.size
                val stepX = chartW / (numPoints - 1).coerceAtLeast(1)

                // Luteal Phase Highlight Band
                if (selectedTimeRange == BiomarkerTimeRange.DAYS_30 && numPoints >= 28) {
                    val lutStartIdx = 14
                    val lutEndIdx = 28.coerceAtMost(numPoints)
                    val lutX = paddingStart + (lutStartIdx - 1) * stepX
                    val lutW = (lutEndIdx - lutStartIdx) * stepX
                    drawRect(
                        color = Color(0xFFEC407A).copy(alpha = 0.08f),
                        topLeft = Offset(lutX, paddingTop),
                        size = Size(lutW, chartH)
                    )
                }

                // RHR normalized path (55 to 88 bpm)
                val rhrCoords = safePoints.mapIndexed { idx, pt ->
                    val v = pt.restingHeartRate.coerceIn(55.0, 88.0)
                    val x = paddingStart + idx * stepX
                    val y = paddingTop + chartH - (((v - 55.0) / (88.0 - 55.0)) * chartH).toFloat()
                    Offset(x, y)
                }

                // HRV normalized path (15 to 75 ms)
                val hrvCoords = safePoints.mapIndexed { idx, pt ->
                    val v = pt.hrvRmssd.coerceIn(15.0, 75.0)
                    val x = paddingStart + idx * stepX
                    val y = paddingTop + chartH - (((v - 15.0) / (75.0 - 15.0)) * chartH).toFloat()
                    Offset(x, y)
                }

                // Draw RHR Line (Pink)
                val rhrPath = Path().apply {
                    moveTo(rhrCoords.first().x, rhrCoords.first().y)
                    for (i in 0 until rhrCoords.size - 1) {
                        val p0 = rhrCoords[i]
                        val p1 = rhrCoords[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }
                drawPath(rhrPath, Color(0xFFE91E63), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                // Draw HRV Line (Purple)
                val hrvPath = Path().apply {
                    moveTo(hrvCoords.first().x, hrvCoords.first().y)
                    for (i in 0 until hrvCoords.size - 1) {
                        val p0 = hrvCoords[i]
                        val p1 = hrvCoords[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }
                drawPath(hrvPath, Color(0xFF7C4DFF), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

                // Draw daily dots for both series
                rhrCoords.forEachIndexed { idx, coord ->
                    val isSel = idx == selectedIndex
                    drawCircle(Color.White, radius = if (isSel) 4.5.dp.toPx() else 2.5.dp.toPx(), center = coord)
                    drawCircle(Color(0xFFE91E63), radius = if (isSel) 3.5.dp.toPx() else 1.8.dp.toPx(), center = coord)
                }

                hrvCoords.forEachIndexed { idx, coord ->
                    val isSel = idx == selectedIndex
                    drawCircle(Color.White, radius = if (isSel) 4.5.dp.toPx() else 2.5.dp.toPx(), center = coord)
                    drawCircle(Color(0xFF7C4DFF), radius = if (isSel) 3.5.dp.toPx() else 1.8.dp.toPx(), center = coord)
                }
            }
        }

        // Legend row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFE91E63), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("RHR (Surges in Luteal)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF7C4DFF), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("HRV (Drops in Luteal)", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
            }
        }
    }
}


@Composable
private fun BiomarkerComparisonTable(correlation: BiometricCycleCorrelation) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Wearable & Glycemic Phase Shifts Table",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Synchronized across 2 consecutive C-PASS assessment cycles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            CorrelationMetricRow(
                metric = "Resting Heart Rate (RHR)",
                follicular = "${correlation.follicularRhrMean} bpm",
                luteal = "${correlation.lutealRhrMean} bpm",
                delta = "+${correlation.rhrLutealSurgePct}% surge",
                isPositiveShift = false
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            CorrelationMetricRow(
                metric = "Heart Rate Variability (HRV)",
                follicular = "${correlation.follicularHrvMean} ms",
                luteal = "${correlation.lutealHrvMean} ms",
                delta = "-${correlation.hrvLutealDropPct}% drop",
                isPositiveShift = false
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            CorrelationMetricRow(
                metric = "Deep Sleep Architecture",
                follicular = "${correlation.follicularDeepSleepMinMean.toInt()} min",
                luteal = "${correlation.lutealDeepSleepMinMean.toInt()} min",
                delta = "-${correlation.deepSleepDropPct}% loss",
                isPositiveShift = false
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            CorrelationMetricRow(
                metric = "CGM Glucose Variability (%CV)",
                follicular = "${correlation.follicularGlucoseCvMean}%",
                luteal = "${correlation.lutealGlucoseCvMean}%",
                delta = "+${correlation.glucoseCvIncreasePct}% swings",
                isPositiveShift = false
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            CorrelationMetricRow(
                metric = "Basal Body Temperature",
                follicular = "${correlation.follicularBbtMean} °F",
                luteal = "${correlation.lutealBbtMean} °F",
                delta = "+0.76°F shift",
                isPositiveShift = true
            )

            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = correlation.clinicalSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun CgmMetricMiniItem(
    title: String,
    value: String,
    subtitle: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun BiometricVitalCard(
    title: String,
    value: String,
    trendText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = accentColor.copy(alpha = 0.14f),
                    shape = CircleShape,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = trendText,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun CorrelationMetricRow(
    metric: String,
    follicular: String,
    luteal: String,
    delta: String,
    isPositiveShift: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Text(
                text = metric,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            modifier = Modifier.weight(1.5f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Follicular", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Text(text = follicular, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Luteal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                Text(text = luteal, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
            Surface(
                color = if (isPositiveShift) Color(0xFF2E7D32).copy(alpha = 0.12f) else Color(0xFFD32F2F).copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = delta,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositiveShift) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun CgmCanvasChart(
    points: List<CgmReadingPoint>,
    onPointSelected: (CgmReadingPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetMin = 70.0
    val targetMax = 140.0
    val yMin = 50.0
    val yMax = 220.0

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7FAFA))
            .pointerInput(points) {
                detectTapGestures { offset ->
                    if (points.isNotEmpty()) {
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val index = ((points.size - 1) * fraction).toInt().coerceIn(0, points.size - 1)
                        onPointSelected(points[index])
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        // Target Zone (70 to 140 mg/dL) Shaded Band
        val targetTopY = height - ((targetMax - yMin) / (yMax - yMin) * height).toFloat()
        val targetBottomY = height - ((targetMin - yMin) / (yMax - yMin) * height).toFloat()
        val bandHeight = targetBottomY - targetTopY

        drawRect(
            color = Color(0xFF00897B).copy(alpha = 0.12f),
            topLeft = Offset(0f, targetTopY),
            size = Size(width, bandHeight)
        )

        // Target line markers
        drawLine(
            color = Color(0xFF00897B).copy(alpha = 0.35f),
            start = Offset(0f, targetTopY),
            end = Offset(width, targetTopY),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0xFF00897B).copy(alpha = 0.35f),
            start = Offset(0f, targetBottomY),
            end = Offset(width, targetBottomY),
            strokeWidth = 1.dp.toPx()
        )

        // Time Grid Vertical Lines (00:00, 06:00, 12:00, 18:00, 24:00)
        for (i in 0..4) {
            val gridX = width * (i / 4f)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(gridX, 0f),
                end = Offset(gridX, height),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (points.size < 2) return@Canvas

        // Draw CGM Trace Path
        val path = Path()
        val stepX = width / (points.size - 1)

        points.forEachIndexed { i, pt ->
            val x = i * stepX
            val yFraction = ((pt.glucoseMgDl - yMin) / (yMax - yMin)).coerceIn(0.0, 1.0)
            val y = height - (yFraction * height).toFloat()

            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = Color(0xFF00897B),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw last point dot
        val lastPoint = points.last()
        val lastX = width
        val lastYFraction = ((lastPoint.glucoseMgDl - yMin) / (yMax - yMin)).coerceIn(0.0, 1.0)
        val lastY = height - (lastYFraction * height).toFloat()

        drawCircle(
            color = Color(0xFF00897B),
            radius = 5.dp.toPx(),
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = Color.White,
            radius = 2.5.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}

@Composable
private fun DeviceManagerDialog(
    devices: List<DeviceConnectionInfo>,
    onToggleConnection: (WearableDeviceType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Connected Wearables & CGM", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Manage active data bridges for Apple HealthKit, Android Health Connect, and CGM Cloud APIs:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = device.modelName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${device.deviceType.platform} • Battery: ${device.batteryPercent}% • ${device.lastSyncFormatted}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = device.isConnected,
                            onCheckedChange = { onToggleConnection(device.deviceType) }
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
