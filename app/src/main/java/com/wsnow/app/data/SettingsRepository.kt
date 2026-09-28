package com.wsnow.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wsnow.engine.Difficulty
import com.wsnow.engine.ai.ApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class Settings(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val difficulty: Difficulty = Difficulty.MEDIUM,
) {
    val apiConfig: ApiConfig get() = ApiConfig(baseUrl, apiKey)
    val isAiConfigured: Boolean get() = baseUrl.isNotBlank() && model.isNotBlank()
}

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val baseUrl = stringPreferencesKey("base_url")
        val apiKey = stringPreferencesKey("api_key")
        val model = stringPreferencesKey("model")
        val difficulty = stringPreferencesKey("difficulty")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    suspend fun setBaseUrl(value: String) = set(Keys.baseUrl, value)
    suspend fun setApiKey(value: String) = set(Keys.apiKey, value)
    suspend fun setModel(value: String) = set(Keys.model, value)
    suspend fun setDifficulty(value: Difficulty) = set(Keys.difficulty, value.name)

    private suspend fun set(key: Preferences.Key<String>, value: String) {
        context.dataStore.edit { it[key] = value }
    }

    private fun Preferences.toSettings() = Settings(
        baseUrl = this[Keys.baseUrl].orEmpty(),
        apiKey = this[Keys.apiKey].orEmpty(),
        model = this[Keys.model].orEmpty(),
        difficulty = this[Keys.difficulty]
            ?.let { name -> Difficulty.entries.firstOrNull { it.name == name } }
            ?: Difficulty.MEDIUM,
    )
}
