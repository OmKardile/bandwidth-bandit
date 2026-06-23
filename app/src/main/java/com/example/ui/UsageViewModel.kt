package com.example.ui

import android.app.Application
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SpeedState
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.UsageEntity
import com.example.data.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class AppUsageItem(
    val packageName: String,
    val appLabel: String,
    val wifiRx: Long,
    val wifiTx: Long,
    val mobileRx: Long,
    val mobileTx: Long,
    val totalBytes: Long
)

class UsageViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = UsageRepository(database.usageDao())
    val prefs = PreferencesManager(application)

    // Current selected month for calendar (format "yyyy-MM")
    private val _selectedMonth = MutableStateFlow(SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    // Current selected date for detailed calendar card (format "yyyy-MM-dd")
    private val _selectedDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // Preferences states loaded into compose
    val speedUnit = MutableStateFlow(prefs.speedUnit)
    val dailyAlertLimitMb = MutableStateFlow(prefs.dailyAlertLimitMb)
    val appTheme = MutableStateFlow(prefs.appTheme)
    val colorPalette = MutableStateFlow(prefs.colorPalette)
    val notifFontColor = MutableStateFlow(prefs.notifFontColor)
    val isDynamicNotifColor = MutableStateFlow(prefs.isDynamicNotifColor)
    val isServiceEnabled = MutableStateFlow(prefs.isServiceEnabled)

    // Reactive flow for data of the selected month
    val monthlyUsages: StateFlow<List<UsageEntity>> = _selectedMonth
        .flatMapLatest { month ->
            repository.getUsagesByMonth(month)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive flow for usage of the selected date
    val selectedDateUsage: StateFlow<UsageEntity?> = _selectedDate
        .flatMapLatest { date ->
            repository.getUsageByDateFlow(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val last7DaysUsage: StateFlow<List<UsageEntity>> = repository.allUsages
        .map { list ->
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dates = (0 until 7).map { i ->
                val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                sdf.format(c.time)
            }.reversed()
            dates.map { d ->
                list.find { it.date == d } ?: UsageEntity(date = d)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allUsages: StateFlow<List<UsageEntity>> = repository.allUsages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Usage permissions & granular lists
    val isUsagePermissionGranted = MutableStateFlow(false)
    val isSideloaded = MutableStateFlow(false)
    val appUsageList = MutableStateFlow<List<AppUsageItem>>(emptyList())
    val weeklyAppUsageList = MutableStateFlow<List<AppUsageItem>>(emptyList())
    val monthlyAppUsageList = MutableStateFlow<List<AppUsageItem>>(emptyList())
    val isAppUsageLoading = MutableStateFlow(false)
    val isSyncing = MutableStateFlow(false)

    init {
        checkIfSideloaded()
        checkPermissionState()
    }

    fun checkIfSideloaded() {
        val ctx = getApplication<Application>()
        val pm = ctx.packageManager
        try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(ctx.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(ctx.packageName)
            }
            // Sideloaded installations are common through packageinstaller, adb/shell, or have null installer
            val sideloaded = installer == null || installer.isEmpty() ||
                             installer.contains("packageinstaller") ||
                             installer == "com.android.shell"
            isSideloaded.value = sideloaded
            Log.d("UsagePermission", "App install source installer: $installer, isSideloaded: $sideloaded")
        } catch (e: Exception) {
            isSideloaded.value = true
            Log.w("UsagePermission", "Error checking sideload source, defaulting to true: ${e.message}")
        }
    }

    fun checkPermissionState() {
        val ctx = getApplication<Application>()
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
        }
        
        val granted = mode == android.app.AppOpsManager.MODE_ALLOWED
        isUsagePermissionGranted.value = granted
        
        if (granted) {
            Log.i("UsagePermission", "Usage Access Permission is successfuly GRANTED (mode: MODE_ALLOWED). Syncing stats...")
            syncHistoricalUsage()
            loadAppUsageForSelectedDate()
            loadWeeklyAndMonthlyAppUsage()
        } else {
            Log.w("UsagePermission", "--- USAGE PERMISSION CHECK: NOT GRANTED ---")
            Log.w("UsagePermission", "Package Name: ${ctx.packageName}")
            Log.w("UsagePermission", "AppOps Code Mode: $mode (Expected: MODE_ALLOWED = 0)")
            Log.w("UsagePermission", "Android OS Version: Build.VERSION.SDK_INT = ${Build.VERSION.SDK_INT}")
            Log.w("UsagePermission", "Is Sideloaded App: ${isSideloaded.value}")
            
            // Verify if declared in AndroidManifest
            try {
                val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS)
                val requestedPermissions = packageInfo.requestedPermissions
                val isDeclared = requestedPermissions?.contains("android.permission.PACKAGE_USAGE_STATS") == true
                Log.w("UsagePermission", "Is PACKAGE_USAGE_STATS declared in App Manifest: $isDeclared")
                if (!isDeclared) {
                    Log.w("UsagePermission", "android.permission.PACKAGE_USAGE_STATS is missing or rejected in the AndroidManifest.xml!")
                }
            } catch (e: Exception) {
                Log.w("UsagePermission", "Could not check manifest declarations: ${e.message}")
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isSideloaded.value) {
                Log.w("UsagePermission", "ALERT: Sideloaded app running on Android 13+. Android will block the permission toggle by default via Restricted Settings!")
                Log.w("UsagePermission", "User MUST enable Restricted Settings first by visiting App Info screen -> top right 3 dots (⋮) -> Allow restricted settings.")
            }
            Log.w("UsagePermission", "------------------------------------")
        }
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            try {
                data = Uri.fromParts("package", context.packageName, null)
            } catch (e: Exception) {
                Log.w("UsagePermission", "Cannot set package data for usage access settings: ${e.message}")
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (ex: Exception) {
                Log.e("UsagePermission", "Failed to open usage access settings", ex)
            }
        }
    }

    fun openAppInfoSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:" + context.packageName)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (ex: Exception) {
                Log.e("UsagePermission", "Failed to open settings fallback", ex)
            }
        }
    }

    private fun queryTotalUsageForTimeframe(nsm: NetworkStatsManager, type: Int, startTime: Long, endTime: Long): Pair<Long, Long> {
        var rx = 0L
        var tx = 0L
        try {
            val stats = nsm.querySummary(type, null, startTime, endTime)
            if (stats != null) {
                val bucket = android.app.usage.NetworkStats.Bucket()
                while (stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    rx += bucket.rxBytes
                    tx += bucket.txBytes
                }
                stats.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Pair(rx, tx)
    }

    fun syncHistoricalUsage() {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            try {
                val ctx = getApplication<Application>()
                val nsm = ctx.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
                if (nsm != null) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    
                    // Sync the last 30 days
                    for (i in 0 until 30) {
                        val checkCal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -i)
                        }
                        val formattedDate = sdf.format(checkCal.time)
                        
                        // Calculate start & end millis for this day
                        checkCal.set(Calendar.HOUR_OF_DAY, 0)
                        checkCal.set(Calendar.MINUTE, 0)
                        checkCal.set(Calendar.SECOND, 0)
                        checkCal.set(Calendar.MILLISECOND, 0)
                        val start = checkCal.timeInMillis
                        
                        checkCal.set(Calendar.HOUR_OF_DAY, 23)
                        checkCal.set(Calendar.MINUTE, 59)
                        checkCal.set(Calendar.SECOND, 59)
                        checkCal.set(Calendar.MILLISECOND, 999)
                        val end = checkCal.timeInMillis
                        
                        // Query wifi robustly
                        val (wRx, wTx) = queryTotalUsageForTimeframe(nsm, ConnectivityManager.TYPE_WIFI, start, end)

                        // Query mobile robustly
                        val (mRx, mTx) = queryTotalUsageForTimeframe(nsm, ConnectivityManager.TYPE_MOBILE, start, end)

                        if (wRx > 0L || wTx > 0L || mRx > 0L || mTx > 0L) {
                            val entity = UsageEntity(
                                date = formattedDate,
                                wifiRx = wRx,
                                wifiTx = wTx,
                                mobileRx = mRx,
                                mobileTx = mTx
                            )
                            repository.insertOrUpdateUsage(entity)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun loadWeeklyAndMonthlyAppUsage() {
        if (!isUsagePermissionGranted.value) return
        viewModelScope.launch {
            isAppUsageLoading.value = true
            try {
                val ctx = getApplication<Application>()
                
                // Weekly: last 7 days
                val cal = Calendar.getInstance()
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                val weeklyStats = withContext(Dispatchers.IO) {
                    queryAppUsageForTimeframe(ctx, start, end)
                }
                weeklyAppUsageList.value = weeklyStats
                
                // Monthly: last 30 days
                val cal2 = Calendar.getInstance()
                val end2 = cal2.timeInMillis
                cal2.add(Calendar.DAY_OF_YEAR, -30)
                cal2.set(Calendar.HOUR_OF_DAY, 0)
                cal2.set(Calendar.MINUTE, 0)
                cal2.set(Calendar.SECOND, 0)
                val start2 = cal2.timeInMillis
                val monthlyStats = withContext(Dispatchers.IO) {
                    queryAppUsageForTimeframe(ctx, start2, end2)
                }
                monthlyAppUsageList.value = monthlyStats
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isAppUsageLoading.value = false
            }
        }
    }

    fun loadAppUsageForSelectedDate() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            if (!isUsagePermissionGranted.value) {
                appUsageList.value = emptyList()
                return@launch
            }
            
            val dateStr = _selectedDate.value
            val times = getStartAndEndMillis(dateStr)
            
            // Query lists in helper IO dispatcher
            val stats = withContext(Dispatchers.IO) {
                queryAppUsageForTimeframe(ctx, times.first, times.second)
            }
            appUsageList.value = stats
        }
    }

    private fun getStartAndEndMillis(dateStr: String): Pair<Long, Long> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = try {
            sdf.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTime = calendar.timeInMillis
        
        return Pair(startTime, endTime)
    }

    private fun queryAppUsageForTimeframe(context: Context, startTime: Long, endTime: Long): List<AppUsageItem> {
        val resultList = mutableMapOf<Int, AppUsageTemp>()
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager ?: return emptyList()
        
        // 1. Wifi stats query
        try {
            val statsWifi = nsm.querySummary(ConnectivityManager.TYPE_WIFI, null, startTime, endTime)
            val bucket = android.app.usage.NetworkStats.Bucket()
            while (statsWifi.hasNextBucket()) {
                statsWifi.getNextBucket(bucket)
                val uid = bucket.uid
                val rx = bucket.rxBytes
                val tx = bucket.txBytes
                val existing = resultList.getOrPut(uid) { AppUsageTemp(uid) }
                existing.wifiRx += rx
                existing.wifiTx += tx
            }
            statsWifi.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 2. Mobile stats query
        try {
            val statsMobile = nsm.querySummary(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime)
            val bucket = android.app.usage.NetworkStats.Bucket()
            while (statsMobile.hasNextBucket()) {
                statsMobile.getNextBucket(bucket)
                val uid = bucket.uid
                val rx = bucket.rxBytes
                val tx = bucket.txBytes
                val existing = resultList.getOrPut(uid) { AppUsageTemp(uid) }
                existing.mobileRx += rx
                existing.mobileTx += tx
            }
            statsMobile.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Resolve info from Package Manager
        val pm = context.packageManager
        val appsMap = mutableMapOf<Int, Pair<String, String>>()
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                val uid = app.uid
                val pkg = app.packageName
                val label = pm.getApplicationLabel(app).toString()
                appsMap[uid] = Pair(pkg, label)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return resultList.values.map { temp ->
            val appInfo = appsMap[temp.uid]
            val (pkg, label) = if (appInfo != null) {
                appInfo
            } else {
                when (temp.uid) {
                    android.app.usage.NetworkStats.Bucket.UID_TETHERING -> Pair("tethering", "Tethering / Hotspot")
                    android.app.usage.NetworkStats.Bucket.UID_REMOVED -> Pair("removed", "Removed apps")
                    1000 -> Pair("android.system", "OS & System Services")
                    else -> Pair("unknown.uid.${temp.uid}", "Process UID ${temp.uid}")
                }
            }
            
            AppUsageItem(
                packageName = pkg,
                appLabel = label,
                wifiRx = temp.wifiRx,
                wifiTx = temp.wifiTx,
                mobileRx = temp.mobileRx,
                mobileTx = temp.mobileTx,
                totalBytes = temp.wifiRx + temp.wifiTx + temp.mobileRx + temp.mobileTx
            )
        }.filter { it.totalBytes > 0 }
         .sortedByDescending { it.totalBytes }
    }

    fun changeMonth(offset: Int) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        try {
            val date = sdf.parse(_selectedMonth.value) ?: return
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.MONTH, offset)
            }
            _selectedMonth.value = sdf.format(cal.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        loadAppUsageForSelectedDate()
    }

    // Settings modifiers
    fun setSpeedUnit(unit: String) {
        prefs.speedUnit = unit
        speedUnit.value = unit
    }

    fun setDailyAlertLimit(limitMb: Long) {
        prefs.dailyAlertLimitMb = limitMb
        dailyAlertLimitMb.value = limitMb
    }

    fun setAppTheme(theme: String) {
        prefs.appTheme = theme
        appTheme.value = theme
    }

    fun setColorPalette(palette: String) {
        prefs.colorPalette = palette
        colorPalette.value = palette
    }

    fun setNotifColor(colorHex: String) {
        prefs.notifFontColor = colorHex
        notifFontColor.value = colorHex
    }

    fun setDynamicNotifColor(enabled: Boolean) {
        prefs.isDynamicNotifColor = enabled
        isDynamicNotifColor.value = enabled
    }

    fun setServiceEnabled(enabled: Boolean) {
        prefs.isServiceEnabled = enabled
        isServiceEnabled.value = enabled
    }

    fun clearHistory() {
        viewModelScope.launch {
            database.clearAllTables()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.insertOrUpdateUsage(UsageEntity(date = todayStr))
            SpeedState.todayMobileUsage.value = 0L
            SpeedState.todayWifiUsage.value = 0L
            loadAppUsageForSelectedDate()
        }
    }
}

private class AppUsageTemp(val uid: Int) {
    var wifiRx: Long = 0L
    var wifiTx: Long = 0L
    var mobileRx: Long = 0L
    var mobileTx: Long = 0L
}
