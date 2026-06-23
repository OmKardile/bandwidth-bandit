package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.DashboardScreen
import com.example.ui.CalendarScreen
import com.example.ui.SpeedTestScreen
import com.example.ui.SettingsScreen
import com.example.ui.UsageViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: UsageViewModel

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeedMonitorServiceIfNeeded()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.checkPermissionState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[UsageViewModel::class.java]

        // Handle startup permission and service initialization
        checkNotificationPermissionAndStart()

        setContent {
            val appThemeOpt by viewModel.appTheme.collectAsStateWithLifecycle()
            val colorPaletteOpt by viewModel.colorPalette.collectAsStateWithLifecycle()
            
            // Map custom theme selector state to system configurations
            val isDark = when (appThemeOpt) {
                "Dark" -> true
                "Light" -> false
                else -> isSystemInDarkTheme() // "System"
            }

            MyApplicationTheme(darkTheme = isDark, paletteName = colorPaletteOpt, dynamicColor = false) {
                var selectedTab by remember { mutableStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Glassmorphic Premium Cyber Navigation Deck
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("main_navigation_bar"),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            tonalElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val navTabs = listOf(
                                    Triple(0, Icons.Default.Home, "Dashboard"),
                                    Triple(1, Icons.Default.DateRange, "Calendar"),
                                    Triple(2, Icons.Default.PlayArrow, "Speed Test"),
                                    Triple(3, Icons.Default.Settings, "Settings")
                                )

                                navTabs.forEach { (index, icon, title) ->
                                    val isTabSelected = selectedTab == index
                                    val tabTag = when (index) {
                                        0 -> "nav_item_dashboard"
                                        1 -> "nav_item_calendar"
                                        2 -> "nav_item_speedtest"
                                        else -> "nav_item_settings"
                                    }

                                    val bgAnimColor by animateColorAsState(
                                        targetValue = if (isTabSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "nav_bg"
                                    )
                                    val textAnimColor by animateColorAsState(
                                        targetValue = if (isTabSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "nav_txt"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .weight(if (isTabSelected) 1.5f else 1.0f)
                                            .testTag(tabTag)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(bgAnimColor)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = { selectedTab = index }
                                            )
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.animateContentSize()
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = title,
                                                tint = textAnimColor,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            if (isTabSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = title,
                                                    color = textAnimColor,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Black,
                                                        letterSpacing = 0.2.sp
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val contentModifier = Modifier.padding(innerPadding)
                    when (selectedTab) {
                        0 -> DashboardScreen(viewModel = viewModel, modifier = contentModifier)
                        1 -> CalendarScreen(viewModel = viewModel, modifier = contentModifier)
                        2 -> SpeedTestScreen(viewModel = viewModel, modifier = contentModifier)
                        3 -> SettingsScreen(viewModel = viewModel, modifier = contentModifier)
                    }
                }
            }
        }
    }

    private fun checkNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            if (status == PackageManager.PERMISSION_GRANTED) {
                startSpeedMonitorServiceIfNeeded()
            } else {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            startSpeedMonitorServiceIfNeeded()
        }
    }

    private fun startSpeedMonitorServiceIfNeeded() {
        if (viewModel.isServiceEnabled.value) {
            val intent = Intent(this, SpeedMonitorService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
