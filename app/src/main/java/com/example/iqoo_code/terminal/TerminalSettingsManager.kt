package com.example.iqoo_code.terminal

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "terminal_settings")

class TerminalSettingsManager(private val context: Context) {

    val settings: Flow<TerminalSettings> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { pref ->
            TerminalSettings(
                fontSize = pref[SettingKeys.FONT_SIZE] ?: 13,
                theme = pref[SettingKeys.THEME] ?: "dark",
                scrollbackLimit = pref[SettingKeys.SCROLLBACK_LIMIT] ?: 1000,
                cursorStyle = pref[SettingKeys.CURSOR_STYLE] ?: "block"
            )
        }

    suspend fun updateFontSize(size: Int) {
        context.dataStore.edit { it[SettingKeys.FONT_SIZE] = size }
    }

    suspend fun updateTheme(theme: String) {
        context.dataStore.edit { it[SettingKeys.THEME] = theme }
    }

    suspend fun updateScrollbackLimit(limit: Int) {
        context.dataStore.edit { it[SettingKeys.SCROLLBACK_LIMIT] = limit }
    }

    suspend fun updateCursorStyle(style: String) {
        context.dataStore.edit { it[SettingKeys.CURSOR_STYLE] = style }
    }
}
