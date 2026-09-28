package com.wsnow.engine.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class WordListParserTest {
    @Test
    fun parsesCleanJson() {
        val r = WordListParser.parse("""{"title":"Space","words":["Moon","Star"]}""")
        assertEquals(WordList("Space", listOf("Moon", "Star")), r)
    }

    @Test
    fun ignoresThinkingFencesAndProse() {
        val reply = """
            <think>The user wants {"title": ...} hmm</think>
            Sure! Here is your list:
            ```json
            {"title": "Farm Life", "words": ["Tractor", "Barn", "Cow"]}
            ```
            Enjoy!
        """.trimIndent()
        assertEquals(WordList("Farm Life", listOf("Tractor", "Barn", "Cow")), WordListParser.parse(reply))
    }

    @Test
    fun acceptsBareArrayAndOtherKeys() {
        assertEquals(listOf("A1", "B2"), WordListParser.parse("""["A1","B2"]""").words)
        assertEquals(listOf("X", "Y"), WordListParser.parse("""{"name":"t","items":["X","Y"]}""").words)
        assertEquals(listOf("Cat"), WordListParser.parse("""{"words":[{"word":"Cat","hint":"pet"}]}""").words)
    }

    @Test
    fun fallsBackToPlainLists() {
        val reply = "Words:\n1. Apple\n2. Banana\n- Cherry\n* Date"
        assertEquals(listOf("Apple", "Banana", "Cherry", "Date"), WordListParser.parse(reply).words)
    }
}
