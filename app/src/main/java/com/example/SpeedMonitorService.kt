package com.example

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Html
import androidx.core.app.NotificationCompat
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.data.UsageEntity
import com.example.data.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object SpeedState {
    val downloadSpeed = MutableStateFlow(0L) // bytes/sec
    val uploadSpeed = MutableStateFlow(0L) // bytes/sec
    val todayWifiUsage = MutableStateFlow(0L) // total bytes
    val todayMobileUsage = MutableStateFlow(0L) // total bytes
    val isServiceRunning = MutableStateFlow(false)
    val wifiDbm = MutableStateFlow<Int?>(null)
    val cellularDbm = MutableStateFlow<Int?>(null)
    
    // Live chart state: list of (downloadBytesPerSec, uploadBytesPerSec)
    val speedHistory = MutableStateFlow<List<Pair<Long, Long>>>(emptyList())
}

class SpeedMonitorService : Service() {

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var database: AppDatabase
    private lateinit var repository: UsageRepository
    private lateinit var prefs: PreferencesManager

    private var lastTotalRx = 0L
    private var lastTotalTx = 0L
    private var lastMobileRx = 0L
    private var lastMobileTx = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var alertTriggeredForToday = ""

    private val monitorRunnable = object : Runnable {
        override fun run() {
            trackSpeed()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = UsageRepository(database.usageDao())
        prefs = PreferencesManager(this)

        // Initialize snapshot markers
        lastTotalRx = TrafficStats.getTotalRxBytes()
        lastTotalTx = TrafficStats.getTotalTxBytes()
        lastMobileRx = TrafficStats.getMobileRxBytes()
        lastMobileTx = TrafficStats.getMobileTxBytes()

        // Handle reboot edge cases where TrafficStats returns 0 or -1
        if (lastTotalRx == TrafficStats.UNSUPPORTED.toLong()) lastTotalRx = 0L
        if (lastTotalTx == TrafficStats.UNSUPPORTED.toLong()) lastTotalTx = 0L
        if (lastMobileRx == TrafficStats.UNSUPPORTED.toLong()) lastMobileRx = 0L
        if (lastMobileTx == TrafficStats.UNSUPPORTED.toLong()) lastMobileTx = 0L

        createNotificationChannel()
        SpeedState.isServiceRunning.value = true

        startForeground(NOTIFICATION_ID, buildSpeedNotification(0L, 0L))
        handler.post(monitorRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(monitorRunnable)
        serviceJob.cancel()
        SpeedState.isServiceRunning.value = false
    }

    private fun trackSpeed() {
        val currTotalRx = TrafficStats.getTotalRxBytes()
        val currTotalTx = TrafficStats.getTotalTxBytes()
        val currMobileRx = TrafficStats.getMobileRxBytes()
        val currMobileTx = TrafficStats.getMobileTxBytes()

        // If returned UNSUPPORTED, fallback to 0
        val totalRx = if (currTotalRx >= 0) currTotalRx else 0L
        val totalTx = if (currTotalTx >= 0) currTotalTx else 0L
        val mobileRx = if (currMobileRx >= 0) currMobileRx else 0L
        val mobileTx = if (currMobileTx >= 0) currMobileTx else 0L

        // Calculate delta (speed bytes per second)
        var rxDelta = totalRx - lastTotalRx
        var txDelta = totalTx - lastTotalTx
        var mobRxDelta = mobileRx - lastMobileRx
        var mobTxDelta = mobileTx - lastMobileTx

        // Adjust for system reboots
        if (rxDelta < 0) rxDelta = 0L
        if (txDelta < 0) txDelta = 0L
        if (mobRxDelta < 0) mobRxDelta = 0L
        if (mobTxDelta < 0) mobTxDelta = 0L

        // Wi-Fi is Total delta minus Mobile delta
        var wifiRxDelta = rxDelta - mobRxDelta
        var wifiTxDelta = txDelta - mobTxDelta

        if (wifiRxDelta < 0) {
            mobRxDelta = rxDelta
            wifiRxDelta = 0L
        }
        if (wifiTxDelta < 0) {
            mobTxDelta = txDelta
            wifiTxDelta = 0L
        }

        // Live speed flow updates
        SpeedState.downloadSpeed.value = rxDelta
        SpeedState.uploadSpeed.value = txDelta

        // Speed history rolling buffer (max 30 ticks)
        var currentHist = SpeedState.speedHistory.value.toMutableList()
        currentHist.add(Pair(rxDelta, txDelta))
        if (currentHist.size > 30) {
            currentHist.removeAt(0)
        }
        SpeedState.speedHistory.value = currentHist

        // Update database and read live daily summary
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        scope.launch {
            repository.addDataBytes(
                date = todayStr,
                mobileRxDelta = mobRxDelta,
                mobileTxDelta = mobTxDelta,
                wifiRxDelta = wifiRxDelta,
                wifiTxDelta = wifiTxDelta
            )

            val currentUsage = repository.getUsageByDate(todayStr)
            if (currentUsage != null) {
                val totalWifi = currentUsage.wifiRx + currentUsage.wifiTx
                val totalMobile = currentUsage.mobileRx + currentUsage.mobileTx
                
                SpeedState.todayWifiUsage.value = totalWifi
                SpeedState.todayMobileUsage.value = totalMobile

                // Check alert threshold limits
                val totalUsedMb = (totalWifi + totalMobile) / (1024 * 1024)
                val alertLimitMb = prefs.dailyAlertLimitMb
                if (totalUsedMb >= alertLimitMb && alertTriggeredForToday != todayStr) {
                    alertTriggeredForToday = todayStr
                    triggerAlertNotification(totalUsedMb, alertLimitMb)
                }
            }
        }

        // Apply new values to last counter snapshot
        lastTotalRx = totalRx
        lastTotalTx = totalTx
        lastMobileRx = mobileRx
        lastMobileTx = mobileTx

        // Refresh dynamic hardware DBm signal telemetry
        updateSignalTelemetry()

        // Refresh the Foreground Notification
        updateNotification(rxDelta, txDelta)
    }

    private fun updateSignalTelemetry() {
        // 1. Wi-Fi
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val wifiInfo = wifiManager?.connectionInfo
            if (wifiInfo != null && wifiInfo.networkId != -1) {
                SpeedState.wifiDbm.value = wifiInfo.rssi
            } else {
                SpeedState.wifiDbm.value = null
            }
        } catch (e: Exception) {
            SpeedState.wifiDbm.value = null
        }

        // 2. Cellular
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val hasLocationPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            if (hasLocationPerm && telephonyManager != null) {
                val cellInfoList = telephonyManager.allCellInfo
                var cellDbmValue: Int? = null
                if (cellInfoList != null) {
                    for (info in cellInfoList) {
                        if (info.isRegistered) {
                            val strength = when (info) {
                                is android.telephony.CellInfoLte -> info.cellSignalStrength
                                is android.telephony.CellInfoGsm -> info.cellSignalStrength
                                is android.telephony.CellInfoWcdma -> info.cellSignalStrength
                                is android.telephony.CellInfoCdma -> info.cellSignalStrength
                                else -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info is android.telephony.CellInfoNr) {
                                        info.cellSignalStrength
                                    } else {
                                        null
                                    }
                                }
                            }
                            if (strength != null) {
                                val dbmVal = strength.dbm
                                if (dbmVal != Integer.MAX_VALUE && dbmVal < 0) {
                                    cellDbmValue = dbmVal
                                    break
                                }
                            }
                        }
                    }
                }
                SpeedState.cellularDbm.value = cellDbmValue
            } else {
                SpeedState.cellularDbm.value = null
            }
        } catch (e: Exception) {
            SpeedState.cellularDbm.value = null
        }
    }

    private fun updateNotification(rxSpeed: Long, txSpeed: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildSpeedNotification(rxSpeed, txSpeed)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatSpeedForNotif(bytesPerSec: Long, selectedUnit: String): String {
        return when (selectedUnit) {
            "Kbps" -> String.format(Locale.US, "%.0f Kbps", (bytesPerSec * 8.0) / 1000)
            "Mbps" -> String.format(Locale.US, "%.1f Mbps", (bytesPerSec * 8.0) / 1000000)
            "MB/s" -> String.format(Locale.US, "%.1f MB/s", bytesPerSec.toDouble() / 1048576)
            else -> { // Default "KB/s"
                if (bytesPerSec >= 1048576) {
                    String.format(Locale.US, "%.1f MB/s", bytesPerSec.toDouble() / 1048576)
                } else {
                    String.format(Locale.US, "%.1f KB/s", bytesPerSec.toDouble() / 1024)
                }
            }
        }
    }

    private fun buildSpeedNotification(rxSpeed: Long, txSpeed: Long): Notification {
        val selectedUnit = prefs.speedUnit
        val rxStr = formatSpeedForNotif(rxSpeed, selectedUnit)
        val txStr = formatSpeedForNotif(txSpeed, selectedUnit)
        val combinedStr = formatSpeedForNotif(rxSpeed + txSpeed, selectedUnit)

        val fontColor = prefs.notifFontColor

        val collapsedText = "Speed: $combinedStr"
        val expandedText = "Download: $rxStr  •  Upload: $txStr"

        val todayStrStr = String.format(
            Locale.US,
            "Today usage: Mobile: %.1f MB | Wi-Fi: %.1f GB",
            SpeedState.todayMobileUsage.value.toDouble() / 1048576,
            SpeedState.todayWifiUsage.value.toDouble() / 1073741824
        )

        val titleTextFormatted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml("<font color='$fontColor'><b>↓ $rxStr  &nbsp;&nbsp;   ↑ $txStr</b></font>", Html.FROM_HTML_MODE_LEGACY)
        } else {
            "↓ $rxStr   ↑ $txStr"
        }

        val notificationColor = if (prefs.isDynamicNotifColor) {
            val totalBps = rxSpeed + txSpeed
            when {
                totalBps < 102400 -> android.graphics.Color.parseColor("#4CAF50") // Active Green for low
                totalBps < 1048576 -> android.graphics.Color.parseColor("#2196F3") // Playful Blue for medium
                else -> android.graphics.Color.parseColor("#FF9800") // Warm Amber for fast usage
            }
        } else {
            android.graphics.Color.parseColor("#00E676") // Classic brand green
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Standard robust icon
            .setContentTitle(titleTextFormatted)
            .setContentText(collapsedText)
            .setSubText(todayStrStr)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$expandedText\n$todayStrStr"))
            .setOngoing(true)
            .setColor(notificationColor)
            .setColorized(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }

    private fun triggerAlertNotification(totalUsedMb: Long, limitMb: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alertChannelId = "data_alerts_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                alertChannelId,
                "Daily Data Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when daily data consumption exceeds the preset limit"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            101,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val alertText = String.format(
            Locale.US,
            "You have used %.1f MB, which exceeds your daily limit of %d MB!",
            totalUsedMb.toDouble(),
            limitMb
        )

        val notification = NotificationCompat.Builder(this, alertChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Daily Data Warning")
            .setContentText(alertText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alertText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Live Internet Speed Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows dynamic real-time upload and download speeds inside the status bar"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ID = "netspeed_live_channel"
        const val NOTIFICATION_ID = 8881
        const val ALERT_NOTIFICATION_ID = 8882
    }
}
