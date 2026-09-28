package com.wsnow.engine

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object Selection {
    /**
     * Snaps a drag from [start] toward [current] onto the nearest of the 8 grid directions,
     * clamped to the grid. Dragging never has to be perfectly straight.
     */
    fun snap(start: Cell, current: Cell, size: Int): Line {
        val dRow = current.row - start.row
        val dCol = current.col - start.col
        if (dRow == 0 && dCol == 0) return Line(start, start)

        val octant = (atan2(dRow.toDouble(), dCol.toDouble()) / (Math.PI / 4)).roundToInt()
        val angle = octant * Math.PI / 4
        val uCol = cos(angle).roundToInt()
        val uRow = sin(angle).roundToInt()

        var length = when {
            uCol != 0 && uRow != 0 -> maxOf(abs(dRow), abs(dCol))
            uCol != 0 -> abs(dCol)
            else -> abs(dRow)
        }
        while (length > 0) {
            val endRow = start.row + uRow * length
            val endCol = start.col + uCol * length
            if (endRow in 0 until size && endCol in 0 until size) break
            length--
        }
        return Line(start, Cell(start.row + uRow * length, start.col + uCol * length))
    }

    /**
     * Returns the unfound word the [line] selects, if any. The exact placed position matches
     * first; otherwise any line spelling an unfound word (forwards or backwards) counts too.
     */
    fun match(puzzle: Puzzle, line: Line, found: Set<String>): PlacedWord? {
        val cells = line.cells()
        if (cells.size < 2) return null
        val remaining = puzzle.words.filter { it.letters !in found }

        remaining.firstOrNull { word ->
            val placed = word.cells()
            placed == cells || placed == cells.reversed()
        }?.let { return it }

        val text = cells.joinToString("") { puzzle.letterAt(it).toString() }
        val reversed = text.reversed()
        return remaining.firstOrNull { it.letters == text || it.letters == reversed }
    }
}
