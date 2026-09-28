package com.wsnow.app.data

import android.content.Context
import android.util.Log
import com.wsnow.engine.Line
import com.wsnow.engine.Puzzle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** A puzzle plus the player's progress on it. */
@Serializable
data class SavedGame(
    val puzzle: Puzzle,
    /** Found word letters → the line the player drew. */
    val found: Map<String, Line> = emptyMap(),
    val elapsedMs: Long = 0,
    val hintsUsed: Int = 0,
) {
    val isComplete: Boolean get() = found.size >= puzzle.words.size
}

/** Stores each game as a JSON file in app-private storage. */
class PuzzleRepository(context: Context, private val scope: CoroutineScope) {
    private val dir = File(context.filesDir, "puzzles").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val _games = MutableStateFlow<List<SavedGame>>(emptyList())
    /** All saved games, newest first. */
    val games: StateFlow<List<SavedGame>> = _games.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            val loaded = dir.listFiles().orEmpty().filter { it.extension == "json" }.mapNotNull { file ->
                runCatching { json.decodeFromString(SavedGame.serializer(), file.readText()) }
                    .onFailure { Log.w(TAG, "Skipping unreadable ${file.name}", it) }
                    .getOrNull()
            }
            _games.update { current ->
                (current + loaded).distinctBy { it.puzzle.id }.sortedByDescending { it.puzzle.createdAt }
            }
        }
    }

    fun get(id: String): SavedGame? = _games.value.firstOrNull { it.puzzle.id == id }

    /** Updates memory immediately and writes to disk in the background. */
    fun save(game: SavedGame) {
        _games.update { list ->
            (listOf(game) + list.filter { it.puzzle.id != game.puzzle.id }).sortedByDescending { it.puzzle.createdAt }
        }
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                val file = fileFor(game.puzzle.id)
                val tmp = File(dir, "${file.name}.tmp")
                tmp.writeText(json.encodeToString(SavedGame.serializer(), game))
                tmp.renameTo(file)
            }
        }
    }

    suspend fun delete(id: String) {
        _games.update { list -> list.filter { it.puzzle.id != id } }
        withContext(Dispatchers.IO) { mutex.withLock { fileFor(id).delete() } }
    }

    private fun fileFor(id: String) = File(dir, "$id.json")

    private companion object {
        const val TAG = "PuzzleRepository"
    }
}
