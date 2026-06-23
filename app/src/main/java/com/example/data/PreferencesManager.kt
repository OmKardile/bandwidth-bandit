package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("netspeed_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_SPEED_UNIT = "speed_unit"
        const val KEY_DAILY_ALERT_LIMIT = "daily_alert_limit" // in MB
        const val KEY_APP_THEME = "app_theme" // "Light", "Dark", "System"
        const val KEY_COLOR_PALETTE = "color_palette" // "Cyber Turquoise", "Sunset Retro", "Forest Emerald", "Royal Amethyst"
        const val KEY_NOTIF_FONT_COLOR = "notif_font_color" // hex color like "#FFFFFF"
        const val KEY_DYNAMIC_NOTIF_COLOR = "dynamic_notif_color"
        const val KEY_SERVICE_ENABLED = "service_enabled"

        // Display unit options
        val UNITS = listOf("KB/s", "MB/s", "Kbps", "Mbps")
        val COLORS = mapOf(
            "Classic White" to "#FFFFFF",
            "Neon Green" to "#00FF66",
            "Azure Blue" to "#00CCFF",
            "Safety Orange" to "#FF9900",
            "Light Yellow" to "#FFFF66"
        )
    }

    var speedUnit: String
        get() = prefs.getString(KEY_SPEED_UNIT, "KB/s") ?: "KB/s"
        set(value) = prefs.edit().putString(KEY_SPEED_UNIT, value).apply()

    var dailyAlertLimitMb: Long
        get() = prefs.getLong(KEY_DAILY_ALERT_LIMIT, 2048L) // Default 2GB (2048MB)
        set(value) = prefs.edit().putLong(KEY_DAILY_ALERT_LIMIT, value).apply()

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, "System") ?: "System"
        set(value) = prefs.edit().putString(KEY_APP_THEME, value).apply()

    var colorPalette: String
        get() = prefs.getString(KEY_COLOR_PALETTE, "Cyber Turquoise") ?: "Cyber Turquoise"
        set(value) = prefs.edit().putString(KEY_COLOR_PALETTE, value).apply()

    var notifFontColor: String
        get() = prefs.getString(KEY_NOTIF_FONT_COLOR, "#FFFFFF") ?: "#FFFFFF"
        set(value) = prefs.edit().putString(KEY_NOTIF_FONT_COLOR, value).apply()

    var isDynamicNotifColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_NOTIF_COLOR, true)
        set(value) = prefs.edit().putBoolean(KEY_DYNAMIC_NOTIF_COLOR, value).apply()

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()
}
