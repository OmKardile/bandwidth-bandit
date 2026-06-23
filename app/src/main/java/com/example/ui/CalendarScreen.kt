package com.example.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.ImageView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UsageEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: UsageViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPermissionGranted by viewModel.isUsagePermissionGranted.collectAsStateWithLifecycle()
    val isSideloaded by viewModel.isSideloaded.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isAppUsageLoading by viewModel.isAppUsageLoading.collectAsStateWithLifecycle()

    val selectedMonthStr by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedDateStr by viewModel.selectedDate.collectAsStateWithLifecycle()
    val monthlyUsages by viewModel.monthlyUsages.collectAsStateWithLifecycle()
    val selectedUsageEntity by viewModel.selectedDateUsage.collectAsStateWithLifecycle()
    
    // Core data flows for multi-tabs
    val appsUsageList by viewModel.appUsageList.collectAsStateWithLifecycle()
    val weeklyAppUsageList by viewModel.weeklyAppUsageList.collectAsStateWithLifecycle()
    val monthlyAppUsageList by viewModel.monthlyAppUsageList.collectAsStateWithLifecycle()
    val last7DaysUsage by viewModel.last7DaysUsage.collectAsStateWithLifecycle()
    val allUsages by viewModel.allUsages.collectAsStateWithLifecycle()

    // Active screen selection tab ("Today", "7 Days", "30 Days", "Calendar Grid", "History")
    var activeTab by remember { mutableStateOf("Today") }
    val tabsList = listOf("Today", "7 Days", "30 Days", "Calendar Grid", "History")
    var isTroubleshootingExpanded by remember { mutableStateOf(false) }

    // Pure Kotlin local Month Grid calculation
    val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val parsedDate = remember(selectedMonthStr) {
        try {
            sdfMonth.parse(selectedMonthStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    val cal = remember(parsedDate) {
        Calendar.getInstance().apply { time = parsedDate }
    }

    val monthName = remember(cal) {
        cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
    }
    val year = cal.get(Calendar.YEAR)

    // First day of month for weekoffsets
    val firstDayCal = remember(cal) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, cal.get(Calendar.YEAR))
            set(Calendar.MONTH, cal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val daysInMonth = firstDayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val startDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
    val gridOffset = startDayOfWeek - 1

    val weekDays = listOf("S", "M", "T", "W", "T", "F", "S")

    val formattedReadableDate = remember(selectedDateStr) {
        try {
            val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputSdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
            val date = inputSdf.parse(selectedDateStr)
            if (date != null) outputSdf.format(date) else selectedDateStr
        } catch (e: Exception) {
            selectedDateStr
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Header Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NETWORK CONSUMPTION",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Usage History Explorer",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (isSyncing || isAppUsageLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(
                        onClick = {
                            viewModel.checkPermissionState()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // GlassWire-styled Pill/Pills scrollable tab row
        item {
            ScrollableTabRow(
                selectedTabIndex = tabsList.indexOf(activeTab).coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {},
                indicator = {}
            ) {
                tabsList.forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { activeTab = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Permission Alert is show first if NOT granted
        if (!isPermissionGranted) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().testTag("permission_notice_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Dynamic glowing icon box
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Permission Required",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Text(
                            text = "NETWORK USAGE STATISTICS",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.5.sp),
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "To track real-time historical usage and audit network speeds cleanly on an app-by-app basis, Android requires the system Usage Access permission.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Button(
                            onClick = { viewModel.openUsageAccessSettings(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(
                                text = "Grant Usage Access",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                            )
                        }

                        // Collapsible troubleshooting panel specifically for unblocking greyed-out toggles in Android 13/14+
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTroubleshootingExpanded = !isTroubleshootingExpanded },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Troubleshoot",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Is \"Permit usage access\" greyed out?",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    Text(
                                        text = if (isTroubleshootingExpanded) "▲ Hide" else "▼ Show Fix",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                AnimatedVisibility(visible = isTroubleshootingExpanded) {
                                    Column(
                                        modifier = Modifier.padding(top = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "On Android 13 and 14+, sideloaded Apps have security restrictions active by default. Sentry security blocks toggles that use sensitive APIs unless manually unlocked.",
                                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = "⚠️ MANDATORY PRIMING STEP: You MUST click \"Grant Usage Access\" above, find our App, and tap the greyed toggle first. Once Android shows the \"Restricted setting\" prompt, dismiss it and return here! (If you don't do this first, Android hides the unlock option entirely).",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error,
                                                    lineHeight = 16.sp
                                                )
                                            )
                                        }

                                        Text(
                                            text = "After doing the priming step, tap \"Open App Info\" below, and unlock using your device instructions:",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )

                                        // Device guides
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(
                                                "📱 Pixel / Motorola / Stock Android" to "Tap the three-dot menu (⋮) at the top-right corner, select 'Allow restricted settings', then verify your PIN/Pattern/Fingerprint.",
                                                "📱 Samsung Galaxy (One UI)" to "Scroll to the absolute bottom of the App Info page. Tap the 'Allow restricted settings' button. If missing, look under 'More Permissions' or tap the (⋮) menu.",
                                                "📱 OnePlus (OxygenOS)" to "Tap the (⋮) menu in the top-right, choose 'Allow restricted settings', and authenticate your security lock.",
                                                "🔄 MIUI (Xiaomi/Poco)" to "Scroll down the settings list and enable the 'Allow restricted settings' switch directly on the App's detailed settings card."
                                            ).forEach { (device, instruction) ->
                                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                                    Text(
                                                        text = device,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = instruction,
                                                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = { viewModel.openAppInfoSettings(context) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "Open App Info Directly",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Render sections based on selected tab
        when (activeTab) {
            "Today" -> {
                // TODAY INSIGHT PIE/CIRCLE CARD AND APP LIST BREAKDOWN
                item {
                    val logs = selectedUsageEntity
                    val wifiBytes = (logs?.wifiRx ?: 0L) + (logs?.wifiTx ?: 0L)
                    val mobileBytes = (logs?.mobileRx ?: 0L) + (logs?.mobileTx ?: 0L)
                    val grandTotal = wifiBytes + mobileBytes

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TODAY'S STATISTICS",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Live Sync",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                            // Glasswire main wheel display
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                                    .padding(8.dp)
                                    .border(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val formattedTotal = formatLargeBytes(grandTotal)
                                    val parts = formattedTotal.split(" ")
                                    Text(
                                        text = parts.firstOrNull() ?: "0",
                                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = parts.getOrNull(1) ?: "B",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Mobile Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatLargeBytes(mobileBytes), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFF5722))
                                    Text("↓${formatLargeBytes(logs?.mobileRx ?: 0L)}  ↑${formatLargeBytes(logs?.mobileTx ?: 0L)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                }

                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Wi-Fi Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatLargeBytes(wifiBytes), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2196F3))
                                    Text("↓${formatLargeBytes(logs?.wifiRx ?: 0L)}  ↑${formatLargeBytes(logs?.wifiTx ?: 0L)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "App Data Breakdown (Today)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (appsUsageList.isEmpty() && isPermissionGranted) {
                    item {
                        EmptyStateCard()
                    }
                } else {
                    val maxBytes = appsUsageList.firstOrNull()?.totalBytes ?: 1L
                    items(appsUsageList, key = { it.packageName }) { item ->
                        AppUsageRowItem(appItem = item, maxBytes = maxBytes)
                    }
                }
            }

            "7 Days" -> {
                // WEEKLY STATS STACKED BAR CHART AND APP LIST BREAKDOWN
                item {
                    GlassWireBarChart(dailyUsages = last7DaysUsage)
                }

                // Overall totals block
                item {
                    val wSum = weeklyAppUsageList.sumOf { it.wifiRx + it.wifiTx }
                    val mSum = weeklyAppUsageList.sumOf { it.mobileRx + it.mobileTx }
                    val grandSum = wSum + mSum

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Weekly Mobile", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF5722))
                                Text(formatLargeBytes(mSum), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                            }
                            Box(modifier = Modifier.width(1.dp).height(30.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Weekly Wi-Fi", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3))
                                Text(formatLargeBytes(wSum), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                            }
                            Box(modifier = Modifier.width(1.dp).height(30.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Weekly Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(formatLargeBytes(grandSum), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "App Data Breakdown (This Week)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                if (weeklyAppUsageList.isEmpty() && isPermissionGranted) {
                    item {
                        EmptyStateCard()
                    }
                } else {
                    val maxBytes = weeklyAppUsageList.firstOrNull()?.totalBytes ?: 1L
                    items(weeklyAppUsageList, key = { it.packageName }) { item ->
                        AppUsageRowItem(appItem = item, maxBytes = maxBytes)
                    }
                }
            }

            "30 Days" -> {
                // MONTHLY OVERVIEW BLOCKS AND APP SUMMARY LIST
                val wSum = monthlyAppUsageList.sumOf { it.wifiRx + it.wifiTx }
                val mSum = monthlyAppUsageList.sumOf { it.mobileRx + it.mobileTx }
                val grandSum = wSum + mSum

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "MONTHELY COMSUMPTION SUMMARY",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Grand Total Logged", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatLargeBytes(grandSum), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = MaterialTheme.colorScheme.primary)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("30D", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF2196F3), CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Wi-Fi:", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(formatLargeBytes(wSum), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF5722), CircleShape))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mobile Data:", style = MaterialTheme.typography.bodyMedium)
                                }
                                Text(formatLargeBytes(mSum), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            val appsCount = monthlyAppUsageList.size
                            Text(
                                text = "$appsCount apps consumed networks bandwidth within this month timeframe.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "App Data Breakdown (This Month)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }

                if (monthlyAppUsageList.isEmpty() && isPermissionGranted) {
                    item {
                        EmptyStateCard()
                    }
                } else {
                    val maxBytes = monthlyAppUsageList.firstOrNull()?.totalBytes ?: 1L
                    items(monthlyAppUsageList, key = { it.packageName }) { item ->
                        AppUsageRowItem(appItem = item, maxBytes = maxBytes)
                    }
                }
            }

            "Calendar Grid" -> {
                // INTERACTIVE HEATMAP CALENDAR SYSTEM WITH SELECT DAY SPECIFIC CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { viewModel.changeMonth(-1) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Prev Month", tint = MaterialTheme.colorScheme.primary)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$monthName $year",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { viewModel.changeMonth(1) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Grid itself
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                weekDays.forEach { dayName ->
                                    Text(
                                        text = dayName,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                            val totalCells = daysInMonth + gridOffset
                            val totalRows = (totalCells + 6) / 7

                            for (row in 0 until totalRows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (col in 0 until 7) {
                                        val cellIndex = row * 7 + col
                                        val dayNumber = cellIndex - gridOffset + 1

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (dayNumber in 1..daysInMonth) {
                                                val currentDayDateStr = String.format(Locale.US, "%s-%02d", selectedMonthStr, dayNumber)
                                                val logEntry = monthlyUsages.find { it.date == currentDayDateStr }
                                                val isSelected = currentDayDateStr == selectedDateStr

                                                val totalBytes = (logEntry?.wifiRx ?: 0L) + (logEntry?.wifiTx ?: 0L) +
                                                        (logEntry?.mobileRx ?: 0L) + (logEntry?.mobileTx ?: 0L)

                                                val heatmapColor = when {
                                                    totalBytes <= 0L -> Color.Transparent
                                                    totalBytes < 104857600L -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                                    totalBytes < 1073741824L -> Color(0xFF2196F3).copy(alpha = 0.25f)
                                                    else -> Color(0xFFFF5722).copy(alpha = 0.35f)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                        .background(heatmapColor)
                                                        .clickable { viewModel.selectDate(currentDayDateStr) }
                                                        .testTag("calendar_day_$dayNumber")
                                                        .then(
                                                            if (isSelected) {
                                                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                            } else Modifier
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(
                                                            text = dayNumber.toString(),
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                                                            ),
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (totalBytes > 0L) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .background(
                                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                        shape = CircleShape
                                                                    )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Show Selected Date Card details
                item {
                    val logs = selectedUsageEntity
                    val wifiBytes = (logs?.wifiRx ?: 0L) + (logs?.wifiTx ?: 0L)
                    val mobileBytes = (logs?.mobileRx ?: 0L) + (logs?.mobileTx ?: 0L)
                    val gdTotal = wifiBytes + mobileBytes

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_breakdown_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SELECTED DATE DETAILS",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = formattedReadableDate,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Wi-Fi Data", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2196F3))
                                    Text(formatLargeBytes(wifiBytes), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                }
                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Mobile Data", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF5722))
                                    Text(formatLargeBytes(mobileBytes), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                }
                                Box(modifier = Modifier.width(1.dp).height(30.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Day Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(formatLargeBytes(gdTotal), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                                }
                            }
                            
                            Button(
                                onClick = {
                                    activeTab = "Today" // Click loads Today and dynamically updates view
                                },
                                modifier = Modifier.fillMaxWidth().height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Inspect day apps details on Today tab", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            "History" -> {
                // SPREADSHEEET TABLE LIST PRE-DAYS ENTRIES (Screenshot 4)
                item {
                    val weekS = allUsages.take(7).sumOf { it.wifiRx + it.wifiTx + it.mobileRx + it.mobileTx }
                    val monthS = allUsages.take(30).sumOf { it.wifiRx + it.wifiTx + it.mobileRx + it.mobileTx }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "HISTORICAL LOG STATISTICS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Last 7 Days total recorded:", style = MaterialTheme.typography.bodyMedium)
                                Text(formatLargeBytes(weekS), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Last 30 Days total recorded:", style = MaterialTheme.typography.bodyMedium)
                                Text(formatLargeBytes(monthS), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                // Table Header Column Labels
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("DATE", modifier = Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("MOBILE", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFFF5722))
                            Text("WI-FI", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2196F3))
                            Text("TOTAL", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (allUsages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = "No logged historical data entries present.",
                                modifier = Modifier.padding(24.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(allUsages, key = { it.date }) { logEntry ->
                        val mobile = logEntry.mobileRx + logEntry.mobileTx
                        val wifi = logEntry.wifiRx + logEntry.wifiTx
                        val total = mobile + wifi

                        val parsedDateStr = remember(logEntry.date) {
                            try {
                                val inSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val outSdf = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                                val d = inSdf.parse(logEntry.date)
                                if (d != null) outSdf.format(d) else logEntry.date
                            } catch (e: Exception) {
                                logEntry.date
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = parsedDateStr,
                                    modifier = Modifier.weight(1.3f),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatLargeBytes(mobile),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFFFF5722).copy(alpha = 0.85f)
                                )
                                Text(
                                    text = formatLargeBytes(wifi),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = Color(0xFF2196F3).copy(alpha = 0.85f)
                                )
                                Text(
                                    text = formatLargeBytes(total),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassWireBarChart(
    dailyUsages: List<UsageEntity>,
    modifier: Modifier = Modifier
) {
    if (dailyUsages.isEmpty()) return
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())
    
    val maxBytes = remember(dailyUsages) {
        val maxVal = dailyUsages.maxOfOrNull { (it.mobileRx + it.mobileTx + it.wifiRx + it.wifiTx) } ?: 0L
        if (maxVal <= 0L) 100 * 1024 * 1024L else maxVal // minimum scale to lock layout proportions limits
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "7-DAY ACTIVITY PROGRESS (STACKED)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyUsages.forEach { usage ->
                    val totalMobile = usage.mobileRx + usage.mobileTx
                    val totalWifi = usage.wifiRx + usage.wifiTx
                    val grandTotal = totalMobile + totalWifi
                    
                    val parsed = try { sdfDate.parse(usage.date) } catch(e: Exception) { null }
                    val label = if (parsed != null) sdfDay.format(parsed) else ""
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                .width(12.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Rounded track background outline 
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            )
                            
                            // Stack proportional Wi-Fi (blue) / Mobile data (coral) indicator columns represent
                            if (grandTotal > 0L) {
                                val totalProportion = (grandTotal.toFloat() / maxBytes).coerceIn(0.01f, 1f)
                                val wifiProportion = if (grandTotal <= 0L) 0f else (totalWifi.toFloat() / grandTotal)
                                val mobileProportion = if (grandTotal <= 0L) 0f else (totalMobile.toFloat() / grandTotal)

                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight(totalProportion)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                ) {
                                    if (wifiProportion > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(wifiProportion)
                                                .fillMaxWidth()
                                                .background(Color(0xFF2196F3))
                                        )
                                    }
                                    if (mobileProportion > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(mobileProportion)
                                                .fillMaxWidth()
                                                .background(Color(0xFFFF5722))
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatLargeBytes(grandTotal).split(" ").firstOrNull() ?: "0",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF2196F3), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Wi-Fi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF5722), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mobile Data", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AppUsageRowItem(
    appItem: AppUsageItem,
    maxBytes: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                update = { iv ->
                    try {
                        val icon = iv.context.packageManager.getApplicationIcon(appItem.packageName)
                        iv.setImageDrawable(icon)
                    } catch (e: Exception) {
                        iv.setImageResource(android.R.drawable.sym_def_app_icon)
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = appItem.appLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatLargeBytes(appItem.totalBytes),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val fraction = if (maxBytes <= 0L) 0f else (appItem.totalBytes.toFloat() / maxBytes)
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mobile: ${formatLargeBytes(appItem.mobileRx + appItem.mobileTx)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFFFF5722).copy(alpha = 0.85f)
                    )
                    Text(
                        text = "Wi-Fi: ${formatLargeBytes(appItem.wifiRx + appItem.wifiTx)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFF2196F3).copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
            Text(
                text = "No recorded app network activity for this date range.",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
            Text(
                text = "System network usage sync runs in real-time. Check another date or tab category filters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
