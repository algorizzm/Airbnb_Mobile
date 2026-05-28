package com.airbnb.core.mode

import android.content.Context
import android.content.SharedPreferences
import com.airbnb.core.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages and persists the current app navigation mode (TRAVELER / HOST).
 *
 * Responsibilities:
 * - Persist current mode across app restarts via SharedPreferences
 * - Expose the current mode as a StateFlow for reactive UI updates
 * - Provide mode switching (authenticated users only for HOST mode)
 * - Reset to TRAVELER on logout
 *
 * Usage:
 *   AppModeManager.init(context)
 *   AppModeManager.currentMode.collect { mode -> ... }
 *   AppModeManager.setMode(AppMode.HOST)
 */
object AppModeManager {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private const val PREFS_NAME = "app_mode_prefs"
    private const val KEY_MODE = "current_mode"

    // =========================================================
    // STATE
    // =========================================================

    private lateinit var prefs: SharedPreferences

    private val _currentMode = MutableStateFlow(AppMode.TRAVELER)
    val currentMode: StateFlow<AppMode> = _currentMode.asStateFlow()

    // =========================================================
    // INITIALIZATION
    // =========================================================

    /**
     * Must be called once from Application.onCreate() or MainActivity.onCreate()
     * before any mode reads or writes.
     */
    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_MODE, AppMode.TRAVELER.name) ?: AppMode.TRAVELER.name
        _currentMode.value = runCatching { AppMode.valueOf(saved) }.getOrDefault(AppMode.TRAVELER)
    }

    fun restoreSavedMode() {

        val saved =
            prefs.getString(
                KEY_MODE,
                AppMode.TRAVELER.name
            ) ?: AppMode.TRAVELER.name

        _currentMode.value =
            runCatching {
                AppMode.valueOf(saved)
            }.getOrDefault(AppMode.TRAVELER)
    }

    // =========================================================
    // CURRENT MODE (SNAPSHOT)
    // =========================================================

    fun currentModeSnapshot(): AppMode = _currentMode.value

    // =========================================================
    // SET MODE
    // =========================================================

    /**
     * Switches the app to the requested mode.
     *
     * HOST mode is only allowed for authenticated users.
     * Guests attempting to switch to HOST are silently ignored —
     * callers should check authentication before calling this.
     *
     * @param mode The target mode to switch to.
     * @return true if the switch succeeded, false if blocked (guest → HOST).
     */
    fun setMode(mode: AppMode): Boolean {
        if (mode == AppMode.HOST && !AuthManager.isAuthenticated()) {
            // Guests cannot enter host mode
            return false
        }
        _currentMode.value = mode
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        return true
    }

    // =========================================================
    // TOGGLE
    // =========================================================

    /**
     * Toggles between TRAVELER and HOST mode.
     *
     * @return The new mode after toggle, or null if blocked (guest → HOST).
     */
    fun toggleMode(): AppMode? {
        val next = if (_currentMode.value == AppMode.TRAVELER) AppMode.HOST else AppMode.TRAVELER
        return if (setMode(next)) next else null
    }

    // =========================================================
    // RESET (LOGOUT)
    // =========================================================

    /**
     * Resets mode to TRAVELER. Must be called on user logout.
     */
    fun resetToTraveler() {
        _currentMode.value = AppMode.TRAVELER
        prefs.edit().putString(KEY_MODE, AppMode.TRAVELER.name).apply()
    }
}
