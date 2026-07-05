package com.rbmr.simpletodos.ui.theme

import android.content.Context

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private const val PREFS_NAME = "settings"
private const val KEY_THEME_MODE = "theme_mode"

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ThemeMode =
        prefs.getString(KEY_THEME_MODE, null)
            ?.let { saved -> runCatching { ThemeMode.valueOf(saved) }.getOrNull() }
            ?: ThemeMode.SYSTEM

    fun save(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }
}
