package com.wsnow.app.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wsnow.app.WSnowApp
import com.wsnow.app.data.SavedGame
import com.wsnow.engine.Cell
import com.wsnow.engine.Line
import com.wsnow.engine.Selection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as WSnowApp).puzzles

    private val _game = MutableStateFlow<SavedGame?>(null)
    val game = _game.asStateFlow()

    /** Cell briefly highlighted by the hint button. */
    private val _hint = MutableStateFlow<Cell?>(null)
    val hint = _hint.asStateFlow()

    private var hintJob: Job? = null
    private var unsavedMs = 0L

    fun load(id: String) {
        if (_game.value?.puzzle?.id == id) return
        viewModelScope.launch {
            // Saved games load from disk asynchronously at startup.
            _game.value = repo.games.map { list -> list.firstOrNull { it.puzzle.id == id } }.filterNotNull().first()
        }
    }

    /** Returns the word found by this selection, or null. */
    fun submit(line: Line): String? {
        val g = _game.value ?: return null
        val word = Selection.match(g.puzzle, line, g.found.keys) ?: return null
        update(g.copy(found = g.found + (word.letters to line)))
        val hinted = _hint.value
        if (hinted != null && hinted in word.cells()) _hint.value = null
        return word.letters
    }

    fun tick(ms: Long) {
        val g = _game.value ?: return
        if (g.isComplete) return
        _game.value = g.copy(elapsedMs = g.elapsedMs + ms)
        unsavedMs += ms
        if (unsavedMs >= 10_000) update(_game.value!!)
    }

    fun hint() {
        val g = _game.value ?: return
        val word = g.puzzle.words.filter { it.letters !in g.found }.randomOrNull() ?: return
        update(g.copy(hintsUsed = g.hintsUsed + 1))
        hintJob?.cancel()
        _hint.value = Cell(word.row, word.col)
        hintJob = viewModelScope.launch {
            delay(2500)
            _hint.value = null
        }
    }

    fun restart() {
        val g = _game.value ?: return
        update(g.copy(found = emptyMap(), elapsedMs = 0, hintsUsed = 0))
    }

    fun persist() {
        _game.value?.let(::update)
    }

    private fun update(g: SavedGame) {
        _game.value = g
        unsavedMs = 0
        repo.save(g)
    }

    override fun onCleared() {
        persist()
    }
}
