package com.wsnow.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectionTest {
    @Test
    fun snapsSloppyDragsToStraightLines() {
        val start = Cell(5, 5)
        assertEquals(Line(start, Cell(5, 9)), Selection.snap(start, Cell(6, 9), 10)) // mostly right
        assertEquals(Line(start, Cell(9, 9)), Selection.snap(start, Cell(9, 8), 10)) // diagonal
        assertEquals(Line(start, Cell(1, 5)), Selection.snap(start, Cell(1, 4), 10)) // up
        assertEquals(Line(start, Cell(2, 2)), Selection.snap(start, Cell(2, 3), 10)) // up-left
        assertEquals(Line(start, start), Selection.snap(start, start, 10))
    }

    @Test
    fun clampsToGrid() {
        assertEquals(Line(Cell(7, 7), Cell(7, 9)), Selection.snap(Cell(7, 7), Cell(9, 12), 10))
    }

    private val puzzle = Puzzle(
        id = "p", title = "t", theme = "t", difficulty = Difficulty.EASY, size = 4,
        rows = listOf(
            "CATX",
            "XDOG",
            "XXXX",
            "TACX",
        ),
        words = listOf(
            PlacedWord("CAT", "CAT", 0, 0, Direction.E),
            PlacedWord("DOG", "DOG", 1, 1, Direction.E),
        ),
        createdAt = 0,
    )

    @Test
    fun matchesPlacedWordEitherWay() {
        assertEquals("CAT", Selection.match(puzzle, Line(Cell(0, 0), Cell(0, 2)), emptySet())?.letters)
        assertEquals("DOG", Selection.match(puzzle, Line(Cell(1, 3), Cell(1, 1)), emptySet())?.letters)
        assertNull(Selection.match(puzzle, Line(Cell(0, 0), Cell(0, 2)), setOf("CAT")))
        assertNull(Selection.match(puzzle, Line(Cell(0, 0), Cell(0, 1)), emptySet()))
    }

    @Test
    fun acceptsAnyLineSpellingAnUnfoundWord() {
        assertEquals("CAT", Selection.match(puzzle, Line(Cell(3, 2), Cell(3, 0)), emptySet())?.letters)
    }
}
