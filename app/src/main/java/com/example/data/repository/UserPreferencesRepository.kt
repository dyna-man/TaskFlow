package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("taskflow_user_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _focusDurationMinutes = MutableStateFlow(prefs.getInt("focus_duration", 25))
    val focusDurationMinutes: StateFlow<Int> = _focusDurationMinutes.asStateFlow()

    private val _streakDays = MutableStateFlow(prefs.getInt("streak_days", 0))
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private val _lastResetDate = MutableStateFlow(prefs.getString("last_reset_date", "") ?: "")
    val lastResetDate: StateFlow<String> = _lastResetDate.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString("user_name", "Productive User") ?: "Productive User")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userAvatarIndex = MutableStateFlow(prefs.getInt("user_avatar_index", 0))
    val userAvatarIndex: StateFlow<Int> = _userAvatarIndex.asStateFlow()

    private val _userAvatarUri = MutableStateFlow(prefs.getString("user_avatar_uri", null))
    val userAvatarUri: StateFlow<String?> = _userAvatarUri.asStateFlow()

    private val _defaultReminders = MutableStateFlow(prefs.getString("default_reminders", "15 mins before due time") ?: "15 mins before due time")
    val defaultReminders: StateFlow<String> = _defaultReminders.asStateFlow()

    private val _showFocusMode = MutableStateFlow(prefs.getBoolean("show_focus_mode", true))
    val showFocusMode: StateFlow<Boolean> = _showFocusMode.asStateFlow()

    private val _quietHoursEnabled = MutableStateFlow(prefs.getBoolean("quiet_hours_enabled", false))
    val quietHoursEnabled: StateFlow<Boolean> = _quietHoursEnabled.asStateFlow()

    private fun getSavedThemeMode(): AppThemeMode {
        val modeStr = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        return try {
            AppThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _themeMode.value = mode
    }

    fun setDefaultReminders(reminder: String) {
        prefs.edit().putString("default_reminders", reminder).apply()
        _defaultReminders.value = reminder
    }

    fun setShowFocusMode(enabled: Boolean) {
        prefs.edit().putBoolean("show_focus_mode", enabled).apply()
        _showFocusMode.value = enabled
    }

    fun setFocusDuration(minutes: Int) {
        val valid = minutes.coerceIn(5, 120)
        prefs.edit().putInt("focus_duration", valid).apply()
        _focusDurationMinutes.value = valid
    }

    fun setUserName(name: String) {
        prefs.edit().putString("user_name", name).apply()
        _userName.value = name
    }

    fun setUserAvatar(index: Int, uri: String? = null) {
        prefs.edit().putInt("user_avatar_index", index).putString("user_avatar_uri", uri).apply()
        _userAvatarIndex.value = index
        _userAvatarUri.value = uri
    }

    fun toggleQuietHours() {
        val current = _quietHoursEnabled.value
        prefs.edit().putBoolean("quiet_hours_enabled", !current).apply()
        _quietHoursEnabled.value = !current
    }

    fun setStreakDays(days: Int) {
        prefs.edit().putInt("streak_days", days).apply()
        _streakDays.value = days
    }

    fun setLastResetDate(dateStr: String) {
        prefs.edit().putString("last_reset_date", dateStr).apply()
        _lastResetDate.value = dateStr
    }
}
