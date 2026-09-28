package com.wsnow.engine.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class WordList(val title: String?, val words: List<String>)

/** Pulls a title and word list out of whatever a model replied with. */
object WordListParser {
    private val json = Json { isLenient = true }
    private val thinkBlock = Regex("<think(?:ing)?>.*?</think(?:ing)?>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val fence = Regex("```(?:json)?\\s*(.*?)```", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val bullet = Regex("^\\s*(?:[-*•]|\\d+[.)])\\s*")

    fun parse(reply: String): WordList {
        val text = thinkBlock.replace(reply, "").trim()
        val candidates = buildList {
            fence.findAll(text).forEach { add(it.groupValues[1].trim()) }
            add(text)
        }

        for (candidate in candidates) {
            objectSlice(candidate)?.let { slice ->
                parseJson(slice)?.let { return it }
            }
            arraySlice(candidate)?.let { slice ->
                parseJson(slice)?.let { return it }
            }
        }
        return WordList(title = null, words = plainLines(text))
    }

    private fun objectSlice(s: String): String? {
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        return if (start >= 0 && end > start) s.substring(start, end + 1) else null
    }

    private fun arraySlice(s: String): String? {
        val start = s.indexOf('[')
        val end = s.lastIndexOf(']')
        return if (start >= 0 && end > start) s.substring(start, end + 1) else null
    }

    private fun parseJson(s: String): WordList? {
        val element = runCatching { json.parseToJsonElement(s) }.getOrNull() ?: return null
        return when (element) {
            is JsonObject -> {
                val title = (element["title"] as? JsonPrimitive)?.contentOrNull
                val words = listOf("words", "word_list", "wordList", "list").firstNotNullOfOrNull { key ->
                    (element[key] as? JsonArray)?.let(::strings)
                } ?: element.values.firstNotNullOfOrNull { (it as? JsonArray)?.let(::strings) }
                words?.takeIf { it.isNotEmpty() }?.let { WordList(title, it) }
            }
            is JsonArray -> strings(element).takeIf { it.isNotEmpty() }?.let { WordList(null, it) }
            else -> null
        }
    }

    private fun strings(array: JsonArray): List<String> = array.mapNotNull { item: JsonElement ->
        when (item) {
            is JsonPrimitive -> item.contentOrNull
            is JsonObject -> (item["word"] as? JsonPrimitive)?.contentOrNull
            else -> null
        }
    }

    private fun plainLines(text: String): List<String> =
        text.split('\n', ',')
            .map { bullet.replace(it, "").trim().trim('"', '\'', '*') }
            .filter { it.isNotEmpty() && it.length <= 30 && !it.endsWith(":") }
}
