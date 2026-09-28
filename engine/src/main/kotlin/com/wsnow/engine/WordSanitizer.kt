package com.wsnow.engine

import java.text.Normalizer

data class WordCandidate(val letters: String, val display: String)

object WordSanitizer {
    const val MIN_LENGTH = 3

    private val diacritics = Regex("\\p{Mn}+")
    private val whitespace = Regex("\\s+")

    /** Converts one raw word to grid letters (A–Z only) plus a tidy display form. */
    fun normalize(raw: String): WordCandidate? {
        val display = raw.trim().replace(whitespace, " ").uppercase()
        val letters = Normalizer.normalize(display, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .filter { it in 'A'..'Z' }
        if (letters.isEmpty()) return null
        val cleanDisplay = Normalizer.normalize(display, Normalizer.Form.NFD)
            .replace(diacritics, "")
            .filter { it in 'A'..'Z' || it == ' ' || it == '-' || it == '\'' }
            .trim()
        return WordCandidate(letters, cleanDisplay.ifEmpty { letters })
    }

    /**
     * Cleans a raw word list: normalises, enforces length limits, removes duplicates, and drops
     * any word that is contained (forwards or backwards) inside another, since it would appear
     * in the grid twice. Original order is preserved.
     */
    fun sanitize(raw: List<String>, maxLength: Int, minLength: Int = MIN_LENGTH): List<WordCandidate> {
        val seen = HashSet<String>()
        val normalized = raw.mapNotNull(::normalize)
            .filter { it.letters.length in minLength..maxLength }
            .filter { seen.add(it.letters) }

        return normalized.filter { candidate ->
            val rev = candidate.letters.reversed()
            normalized.none { other ->
                other !== candidate && (
                    (other.letters.length > candidate.letters.length &&
                        (other.letters.contains(candidate.letters) || other.letters.contains(rev))) ||
                        // A word and its exact reverse (e.g. STOP / POTS) can't both be unique.
                        (other.letters == rev && normalized.indexOf(other) < normalized.indexOf(candidate))
                    )
            }
        }
    }
}
