package com.example.iqoo_code.terminal

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

data class TerminalSettings(
    val fontSize: Int = 13,
    val theme: String = "dark",
    val scrollbackLimit: Int = 1000,
    val cursorStyle: String = "block"
)

object SettingKeys {
    val FONT_SIZE = intPreferencesKey("font_size")
    val THEME = stringPreferencesKey("theme")
    val SCROLLBACK_LIMIT = intPreferencesKey("scrollback_limit")
    val CURSOR_STYLE = stringPreferencesKey("cursor_style")
}
