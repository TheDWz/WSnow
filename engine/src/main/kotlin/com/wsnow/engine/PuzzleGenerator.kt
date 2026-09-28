package com.wsnow.engine

import java.util.UUID
import kotlin.random.Random

class NotEnoughWordsException(message: String) : Exception(message)

/**
 * Hides a list of words in a square letter grid.
 *
 * Words are placed longest-first, choosing a random allowed direction and preferring spots that
 * overlap letters already in the grid. Leftover cells are filled with letters drawn mostly from the
 * word list, and the grid is checked so every word appears exactly once.
 */
class PuzzleGenerator(private val random: Random = Random.Default) {

    fun generate(
        candidates: List<WordCandidate>,
        difficulty: Difficulty,
        title: String,
        theme: String,
        attempts: Int = 40,
        minWords: Int = MIN_WORDS,
    ): Puzzle {
        val usable = candidates.filter { it.letters.length <= difficulty.size }
        if (usable.size < minWords) {
            throw NotEnoughWordsException("Only ${usable.size} usable words; need at least $minWords.")
        }

        var best: Layout? = null
        for (attempt in 0 until attempts) {
            val layout = tryLayout(usable, difficulty) ?: continue
            if (best == null || layout.placed.size > best.placed.size) best = layout
            if (layout.placed.size >= difficulty.wordCount) break
        }
        val layout = best
        if (layout == null || layout.placed.size < minWords) {
            throw NotEnoughWordsException("Could not fit enough words into the grid.")
        }

        return Puzzle(
            id = UUID.randomUUID().toString(),
            title = title,
            theme = theme,
            difficulty = difficulty,
            size = difficulty.size,
            rows = layout.grid.map { String(it) },
            words = layout.placed,
            createdAt = System.currentTimeMillis(),
        )
    }

    private class Layout(val grid: Array<CharArray>, val placed: List<PlacedWord>)

    private data class Spot(val row: Int, val col: Int, val direction: Direction, val overlap: Int)

    private fun tryLayout(candidates: List<WordCandidate>, difficulty: Difficulty): Layout? {
        val size = difficulty.size
        val grid = Array(size) { CharArray(size) { EMPTY } }
        val placed = mutableListOf<PlacedWord>()

        // Keep the model's ordering to choose *which* words (the first ones are usually the best
        // fit for the theme), but place them longest-first so the long ones still have room.
        val primary = candidates.take(difficulty.wordCount).sortedByDescending { it.letters.length }
        val spares = ArrayDeque(candidates.drop(difficulty.wordCount))

        fun place(word: WordCandidate): Boolean {
            val spot = findSpot(grid, word.letters, difficulty.directions) ?: return false
            word.letters.forEachIndexed { i, ch ->
                grid[spot.row + spot.direction.dy * i][spot.col + spot.direction.dx * i] = ch
            }
            placed += PlacedWord(word.letters, word.display, spot.row, spot.col, spot.direction)
            return true
        }

        for (word in primary) {
            if (!place(word)) {
                while (spares.isNotEmpty() && !place(spares.removeFirst())) Unit
            }
        }

        val filler = fillerCells(grid)
        if (!fill(grid, filler, placed)) return null
        return Layout(grid, placed)
    }

    private fun findSpot(grid: Array<CharArray>, letters: String, directions: List<Direction>): Spot? {
        val size = grid.size
        for (direction in directions.shuffled(random)) {
            val spots = mutableListOf<Spot>()
            for (row in 0 until size) for (col in 0 until size) {
                val endRow = row + direction.dy * (letters.length - 1)
                val endCol = col + direction.dx * (letters.length - 1)
                if (endRow !in 0 until size || endCol !in 0 until size) continue
                var overlap = 0
                var fits = true
                for (i in letters.indices) {
                    val existing = grid[row + direction.dy * i][col + direction.dx * i]
                    if (existing == EMPTY) continue
                    if (existing != letters[i]) { fits = false; break }
                    overlap++
                }
                // Overlapping the whole word would just hide it inside another one.
                if (fits && overlap < letters.length) spots += Spot(row, col, direction, overlap)
            }
            if (spots.isEmpty()) continue
            val maxOverlap = spots.maxOf { it.overlap }
            return if (maxOverlap > 0 && random.nextFloat() < OVERLAP_PREFERENCE) {
                spots.filter { it.overlap == maxOverlap }.random(random)
            } else {
                spots.random(random)
            }
        }
        return null
    }

    private fun fillerCells(grid: Array<CharArray>): List<Cell> = buildList {
        for (r in grid.indices) for (c in grid.indices) if (grid[r][c] == EMPTY) add(Cell(r, c))
    }

    /** Fills empty cells, then re-rolls filler until every word appears exactly once. */
    private fun fill(grid: Array<CharArray>, filler: List<Cell>, placed: List<PlacedWord>): Boolean {
        // Letters from the words themselves make the filler look like the answers, which is what
        // makes a word search challenging. A full alphabet keeps some variety.
        val pool = (placed.joinToString("") { it.letters } + ALPHABET).toCharArray()
        val fillerSet = filler.toHashSet()
        for (cell in filler) grid[cell.row][cell.col] = pool.random(random)

        repeat(MAX_FIX_ROUNDS) {
            val duplicates = placed.flatMap { word ->
                GridSearch.occurrences(grid, word.letters).filter { it.toSet() != word.cells().toSet() }
            }
            if (duplicates.isEmpty()) return true
            val rerollable = duplicates.flatten().filter { it in fillerSet }
            // A duplicate built only from other words' letters can't be fixed by re-rolling.
            if (rerollable.isEmpty()) return false
            for (cell in rerollable) grid[cell.row][cell.col] = ALPHABET.random(random)
        }
        return false
    }

    companion object {
        const val MIN_WORDS = 4
        private const val EMPTY = '\u0000'
        private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val OVERLAP_PREFERENCE = 0.6f
        private const val MAX_FIX_ROUNDS = 200
    }
}

object GridSearch {
    /** Every straight line (any of the 8 directions) that spells [letters]. */
    fun occurrences(grid: Array<CharArray>, letters: String): List<List<Cell>> {
        val size = grid.size
        val found = mutableListOf<List<Cell>>()
        val seen = HashSet<Set<Cell>>()
        for (row in 0 until size) for (col in 0 until size) {
            if (grid[row][col] != letters[0]) continue
            for (d in Direction.entries) {
                val endRow = row + d.dy * (letters.length - 1)
                val endCol = col + d.dx * (letters.length - 1)
                if (endRow !in 0 until size || endCol !in 0 until size) continue
                if (letters.indices.all { grid[row + d.dy * it][col + d.dx * it] == letters[it] }) {
                    val cells = letters.indices.map { Cell(row + d.dy * it, col + d.dx * it) }
                    // Palindromes match twice over the same cells; count them once.
                    if (seen.add(cells.toSet())) found += cells
                }
            }
        }
        return found
    }

    fun occurrences(puzzle: Puzzle, letters: String): List<List<Cell>> =
        occurrences(puzzle.rows.map { it.toCharArray() }.toTypedArray(), letters)
}
