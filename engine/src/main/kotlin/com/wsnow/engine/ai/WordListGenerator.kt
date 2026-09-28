package com.wsnow.engine.ai

import com.wsnow.engine.Difficulty
import com.wsnow.engine.WordCandidate
import com.wsnow.engine.WordSanitizer

data class GeneratedWords(val title: String, val theme: String, val words: List<WordCandidate>)

/** Asks a model for a themed word list and cleans it up for the grid. */
class WordListGenerator(private val client: OpenAiClient) {

    fun generate(
        config: ApiConfig,
        model: String,
        theme: String,
        difficulty: Difficulty,
        avoidThemes: List<String> = emptyList(),
    ): GeneratedWords {
        val wanted = (difficulty.wordCount * 3 + 1) / 2
        val messages = mutableListOf(
            ChatMessage("system", SYSTEM_PROMPT),
            ChatMessage("user", userPrompt(theme, difficulty, wanted, avoidThemes)),
        )

        var best: GeneratedWords? = null
        repeat(2) { round ->
            val reply = client.chat(config, model, messages)
            val parsed = WordListParser.parse(reply)
            val words = WordSanitizer.sanitize(parsed.words, difficulty.maxWordLength)
            val title = parsed.title?.trim()?.takeIf { it.isNotEmpty() }
                ?: theme.trim().ifEmpty { "Word Search" }.replaceFirstChar { it.uppercase() }
            val result = GeneratedWords(title.take(60), theme.trim().ifEmpty { title }, words)
            if (best == null || result.words.size > best!!.words.size) best = result
            if (words.size >= difficulty.wordCount) return result

            if (round == 0) {
                messages += ChatMessage("assistant", reply)
                messages += ChatMessage(
                    "user",
                    "That gave only ${words.size} usable words. I need at least $wanted distinct words, " +
                        "each 3 to ${difficulty.maxWordLength} letters long (spaces and punctuation are " +
                        "removed). Reply again with only the JSON object.",
                )
            }
        }
        return best!!
    }

    private fun userPrompt(theme: String, difficulty: Difficulty, wanted: Int, avoid: List<String>): String = buildString {
        if (theme.isBlank()) {
            append("Pick a fun, specific theme for a word search puzzle yourself.")
            if (avoid.isNotEmpty()) append(" Avoid these recently used themes: ${avoid.joinToString(", ")}.")
            append("\n")
        } else {
            append("Theme: ${theme.trim()}\n")
        }
        append("Difficulty: ${difficulty.label}\n")
        append("Give exactly $wanted words.\n")
        append("Each word must be 3 to ${difficulty.maxWordLength} letters long once spaces are removed.")
        when (difficulty) {
            Difficulty.EASY -> append(" Prefer common, well-known words.")
            Difficulty.MEDIUM -> append(" Mix familiar and less obvious words.")
            Difficulty.HARD -> append(" Include some less common or longer words.")
        }
    }

    companion object {
        const val SYSTEM_PROMPT =
            "You create word lists for word search puzzles. " +
                "Respond with ONLY a JSON object, no markdown and no commentary, in the form " +
                "{\"title\": \"Short catchy puzzle title\", \"words\": [\"WORD\", ...]}. " +
                "Rules: every word must clearly relate to the theme; use letters A-Z only " +
                "(short two-word phrases are fine, spaces will be removed); no duplicates; " +
                "no word may contain another word from the list; no offensive words."
    }
}
