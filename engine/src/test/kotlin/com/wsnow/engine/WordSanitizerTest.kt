package com.wsnow.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WordSanitizerTest {
    @Test
    fun normalizesSpacesAccentsAndPunctuation() {
        assertEquals(WordCandidate("ICECREAM", "ICE CREAM"), WordSanitizer.normalize("  ice   cream "))
        assertEquals(WordCandidate("CREMEBRULEE", "CREME BRULEE"), WordSanitizer.normalize("Crème brûlée"))
        assertEquals(WordCandidate("TREX", "T-REX"), WordSanitizer.normalize("T-Rex!"))
        assertEquals(WordCandidate("RD", "RD"), WordSanitizer.normalize("R2D2"))
        assertNull(WordSanitizer.normalize("123"))
    }

    @Test
    fun filtersLengthDuplicatesAndContainedWords() {
        val result = WordSanitizer.sanitize(
            listOf("Sun", "sun", "Sunflower", "Rose", "Tulip", "Ox", "Chrysanthemums", "Lily", "Pilut"),
            maxLength = 10,
        ).map { it.letters }
        // SUN is inside SUNFLOWER; PILUT is TULIP backwards; OX too short; CHRYSANTHEMUMS too long.
        assertEquals(listOf("SUNFLOWER", "ROSE", "TULIP", "LILY"), result)
    }
}
