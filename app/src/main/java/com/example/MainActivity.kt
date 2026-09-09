package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.HomavalesTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.PmddVitalsViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PmddVitalsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomavalesTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

data class NavItem(val screen: AppScreen, val label: String, val icon: ImageVector)

@Composable
fun MainAppContent(viewModel: PmddVitalsViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    val navItems = listOf(
        NavItem(AppScreen.HOME, "Home", Icons.Default.Home),
        NavItem(AppScreen.TRACK, "Track", Icons.Default.EditNote),
        NavItem(AppScreen.RESULTS, "Results", Icons.Default.Assessment),
        NavItem(AppScreen.EXPORT, "Export", Icons.Default.Description),
        NavItem(AppScreen.GUIDE, "Guide", Icons.Default.MenuBook)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                navItems.forEach { item ->
                    val isSelected = currentScreen == item.screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateTo(item.screen) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_${item.label.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.HOME -> LandingHomeScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            AppScreen.TRACK -> DrspDailyLogScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            AppScreen.RESULTS -> CycleSummaryScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            AppScreen.EXPORT -> ClinicalExportScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            AppScreen.GUIDE -> ClinicalGuideScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
        }
    }
}
