package com.wsnow.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PuzzleGeneratorTest {
    private val words = listOf(
        "Dolphin", "Octopus", "Seahorse", "Jellyfish", "Starfish", "Coral", "Shark", "Whale",
        "Lobster", "Sea Turtle", "Anemone", "Plankton", "Kelp", "Stingray", "Squid", "Urchin",
        "Barracuda", "Clownfish", "Manatee", "Walrus", "Narwhal", "Orca", "Mussel", "Oyster",
    )

    @Test
    fun everyPlacedWordIsInTheGridExactlyOnce() {
        for (difficulty in Difficulty.entries) {
            repeat(25) { seed ->
                val candidates = WordSanitizer.sanitize(words, difficulty.maxWordLength)
                val puzzle = PuzzleGenerator(Random(seed)).generate(candidates, difficulty, "Ocean", "ocean")

                assertEquals(difficulty.size, puzzle.rows.size)
                puzzle.rows.forEach { row ->
                    assertEquals(difficulty.size, row.length)
                    assertTrue("only A-Z in grid: $row", row.all { it in 'A'..'Z' })
                }
                assertEquals(difficulty.wordCount, puzzle.words.size)

                for (word in puzzle.words) {
                    val spelled = word.cells().joinToString("") { puzzle.letterAt(it).toString() }
                    assertEquals(word.letters, spelled)
                    assertTrue("${word.direction} allowed in $difficulty", word.direction in difficulty.directions)
                    val hits = GridSearch.occurrences(puzzle, word.letters)
                    assertEquals("${word.letters} occurs once (seed $seed, $difficulty)", 1, hits.size)
                }
            }
        }
    }

    @Test
    fun sameSeedGivesSameGrid() {
        val candidates = WordSanitizer.sanitize(words, 13)
        val a = PuzzleGenerator(Random(7)).generate(candidates, Difficulty.MEDIUM, "t", "t")
        val b = PuzzleGenerator(Random(7)).generate(candidates, Difficulty.MEDIUM, "t", "t")
        assertEquals(a.rows, b.rows)
    }

    @Test
    fun placesWhatItCanWithFewWords() {
        val candidates = WordSanitizer.sanitize(listOf("cat", "dog", "bird", "fish", "frog"), 10)
        val puzzle = PuzzleGenerator(Random(1)).generate(candidates, Difficulty.EASY, "Pets", "pets")
        assertEquals(5, puzzle.words.size)
    }

    @Test(expected = NotEnoughWordsException::class)
    fun rejectsTooFewWords() {
        val candidates = WordSanitizer.sanitize(listOf("cat", "dog"), 10)
        PuzzleGenerator(Random(1)).generate(candidates, Difficulty.EASY, "Pets", "pets")
    }
}
