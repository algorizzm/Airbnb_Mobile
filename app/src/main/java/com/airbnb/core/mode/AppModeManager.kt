package com.airbnb.core.mode

import android.content.Context
import android.content.SharedPreferences
import com.airbnb.core.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages and persists the current app navigation mode.
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

    private lateinit var appContext: Context

    /**
     * Lazy initialization prevents crashes caused by
     * Firebase auth callbacks happening too early.
     */
    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    private val _currentMode =
        MutableStateFlow(AppMode.TRAVELER)

    val currentMode: StateFlow<AppMode> =
        _currentMode.asStateFlow()

    // =========================================================
    // INITIALIZATION
    // =========================================================

    /**
     * Must be called once from Application.onCreate()
     */
    fun init(context: Context) {

        // Prevent accidental double initialization
        if (::appContext.isInitialized) return

        appContext = context.applicationContext

        restoreSavedMode()
    }

    // =========================================================
    // INTERNAL SAFETY
    // =========================================================

    private fun ensureInitialized() {

        check(::appContext.isInitialized) {
            """
            AppModeManager is not initialized.

            Call:
                AppModeManager.init(context)

            from Application.onCreate()
            before using AppModeManager.
            """.trimIndent()
        }
    }

    // =========================================================
    // RESTORE SAVED MODE
    // =========================================================

    fun restoreSavedMode() {

        ensureInitialized()

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
    // CURRENT MODE
    // =========================================================

    fun currentModeSnapshot(): AppMode {
        return _currentMode.value
    }

    // =========================================================
    // SET MODE
    // =========================================================

    /**
     * Switch app mode.
     *
     * HOST mode requires authentication.
     */
    fun setMode(mode: AppMode): Boolean {

        ensureInitialized()

        // Guests cannot enter HOST mode
        if (
            mode == AppMode.HOST &&
            !AuthManager.isAuthenticated()
        ) {
            return false
        }

        _currentMode.value = mode

        prefs.edit()
            .putString(KEY_MODE, mode.name)
            .apply()

        return true
    }

    // =========================================================
    // TOGGLE MODE
    // =========================================================

    fun toggleMode(): AppMode? {

        val next =
            if (_currentMode.value == AppMode.TRAVELER) {
                AppMode.HOST
            } else {
                AppMode.TRAVELER
            }

        return if (setMode(next)) {
            next
        } else {
            null
        }
    }

    // =========================================================
    // RESET MODE
    // =========================================================

    /**
     * Reset mode after logout.
     */
    fun resetToTraveler() {

        ensureInitialized()

        _currentMode.value = AppMode.TRAVELER

        prefs.edit()
            .putString(KEY_MODE, AppMode.TRAVELER.name)
            .apply()
    }
}