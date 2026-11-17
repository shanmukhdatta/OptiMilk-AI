package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "optimilk_settings")

data class UserSettings(
    val demoModeEnabled: Boolean = false,
    val defaultSamplePrefix: String = "Milk Sample",
    val lastDeviceAddress: String? = null,
    val lastDeviceName: String? = null,
)

class AppPreferences(private val context: Context) {
    private object Keys {
        val DEMO_MODE = booleanPreferencesKey("demo_mode_enabled")
        val SAMPLE_PREFIX = stringPreferencesKey("default_sample_prefix")
        val LAST_DEVICE_ADDRESS = stringPreferencesKey("last_device_address")
        val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            demoModeEnabled = prefs[Keys.DEMO_MODE] ?: false,
            defaultSamplePrefix = prefs[Keys.SAMPLE_PREFIX]?.takeIf { it.isNotBlank() } ?: "Milk Sample",
            lastDeviceAddress = prefs[Keys.LAST_DEVICE_ADDRESS],
            lastDeviceName = prefs[Keys.LAST_DEVICE_NAME],
        )
    }

    suspend fun setDemoModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEMO_MODE] = enabled
        }
    }

    suspend fun setDefaultSamplePrefix(prefix: String) {
        val clean = prefix.trim().ifEmpty { "Milk Sample" }
        context.dataStore.edit { prefs ->
            prefs[Keys.SAMPLE_PREFIX] = clean
        }
    }

    suspend fun saveLastDevice(address: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_DEVICE_ADDRESS] = address
            prefs[Keys.LAST_DEVICE_NAME] = name
        }
    }
}
