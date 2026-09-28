package com.wsnow.engine

import kotlinx.serialization.Serializable

@Serializable
enum class Direction(val dx: Int, val dy: Int) {
    E(1, 0), S(0, 1), SE(1, 1), NE(1, -1),
    W(-1, 0), N(0, -1), NW(-1, -1), SW(-1, 1);

    companion object {
        fun of(dx: Int, dy: Int): Direction? = entries.firstOrNull { it.dx == dx && it.dy == dy }
    }
}

@Serializable
enum class Difficulty(
    val label: String,
    val size: Int,
    val wordCount: Int,
    val directions: List<Direction>,
) {
    EASY("Easy", 10, 8, listOf(Direction.E, Direction.S)),
    MEDIUM("Medium", 13, 12, listOf(Direction.E, Direction.S, Direction.SE, Direction.NE)),
    HARD("Hard", 15, 16, Direction.entries.toList());

    val maxWordLength: Int get() = size
}

@Serializable
data class Cell(val row: Int, val col: Int)

/** A straight run of cells from [start] to [end] inclusive. */
@Serializable
data class Line(val start: Cell, val end: Cell) {
    fun cells(): List<Cell> {
        val dr = Integer.signum(end.row - start.row)
        val dc = Integer.signum(end.col - start.col)
        val len = maxOf(kotlin.math.abs(end.row - start.row), kotlin.math.abs(end.col - start.col))
        return (0..len).map { Cell(start.row + dr * it, start.col + dc * it) }
    }
}

@Serializable
data class PlacedWord(
    /** Letters exactly as they appear in the grid, e.g. "ICECREAM". */
    val letters: String,
    /** What to show in the word list, e.g. "ICE CREAM". */
    val display: String,
    val row: Int,
    val col: Int,
    val direction: Direction,
) {
    val line: Line
        get() = Line(
            Cell(row, col),
            Cell(row + direction.dy * (letters.length - 1), col + direction.dx * (letters.length - 1)),
        )

    fun cells(): List<Cell> = letters.indices.map { Cell(row + direction.dy * it, col + direction.dx * it) }
}

@Serializable
data class Puzzle(
    val id: String,
    val title: String,
    val theme: String,
    val difficulty: Difficulty,
    val size: Int,
    /** One string per row, each [size] characters long. */
    val rows: List<String>,
    val words: List<PlacedWord>,
    val createdAt: Long,
) {
    fun letterAt(cell: Cell): Char = rows[cell.row][cell.col]

    fun inBounds(cell: Cell): Boolean = cell.row in 0 until size && cell.col in 0 until size
}
