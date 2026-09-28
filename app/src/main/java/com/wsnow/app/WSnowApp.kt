package com.wsnow.app

import android.app.Application
import com.wsnow.app.data.PuzzleRepository
import com.wsnow.app.data.SettingsRepository
import com.wsnow.engine.ai.OpenAiClient
import com.wsnow.engine.ai.WordListGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class WSnowApp : Application() {
    /** Outlives screens so saves finish even when the user navigates away. */
    val appScope = CoroutineScope(SupervisorJob())

    val settings by lazy { SettingsRepository(this) }
    val puzzles by lazy { PuzzleRepository(this, appScope) }
    val aiClient by lazy { OpenAiClient() }
    val wordListGenerator by lazy { WordListGenerator(aiClient) }
}
