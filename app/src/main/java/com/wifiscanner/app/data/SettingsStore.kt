package com.wifiscanner.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wifiscanner.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Application settings (DataStore). Language is handled by AppCompatDelegate per-app locales. */
class SettingsStore(private val context: Context) {

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[THEME] ?: ThemeMode.DARK.name) }.getOrDefault(ThemeMode.DARK)
    }

    /** 0 = manual only; otherwise seconds between automatic scans while the scanner screen is visible. */
    val autoRefreshSeconds: Flow<Int> = context.settingsDataStore.data.map { it[AUTO_REFRESH] ?: 0 }

    val speedServer: Flow<String> = context.settingsDataStore.data.map { it[SPEED_SERVER] ?: DEFAULT_SPEED_SERVER }

    /** IDs of router-checklist items the user has ticked. */
    val checklist: Flow<Set<String>> = context.settingsDataStore.data.map { it[CHECKLIST] ?: emptySet() }

    suspend fun setChecklistItem(id: String, checked: Boolean) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[CHECKLIST] ?: emptySet()
            prefs[CHECKLIST] = if (checked) current + id else current - id
        }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsDataStore.edit { it[THEME] = mode.name }
    }

    suspend fun setAutoRefresh(seconds: Int) {
        context.settingsDataStore.edit { it[AUTO_REFRESH] = seconds }
    }

    suspend fun setSpeedServer(url: String) {
        context.settingsDataStore.edit { it[SPEED_SERVER] = url.trim().trimEnd('/') }
    }

    suspend fun resetSpeedServer() {
        context.settingsDataStore.edit { it.remove(SPEED_SERVER) }
    }

    companion object {
        const val DEFAULT_SPEED_SERVER = "https://speed.cloudflare.com"
        private val THEME = stringPreferencesKey("theme")
        private val AUTO_REFRESH = intPreferencesKey("auto_refresh_seconds")
        private val SPEED_SERVER = stringPreferencesKey("speed_server")
        private val CHECKLIST = stringSetPreferencesKey("router_checklist")
    }
}
