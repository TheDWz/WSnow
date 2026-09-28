package com.wsnow.engine.ai

import com.wsnow.engine.Difficulty
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenAiClientTest {
    private val server = MockWebServer()
    private val client = OpenAiClient()
    private lateinit var config: ApiConfig

    @Before fun setUp() {
        server.start()
        config = ApiConfig(server.url("/v1/").toString(), "sk-test")
    }

    @After fun tearDown() = server.shutdown()

    @Test
    fun listsModelsSorted() {
        server.enqueue(MockResponse().setBody("""{"object":"list","data":[{"id":"gpt-b"},{"id":"Gpt-a"},{"id":"llama3"}]}"""))
        assertEquals(listOf("Gpt-a", "gpt-b", "llama3"), client.listModels(config))

        val request = server.takeRequest()
        assertEquals("/v1/models", request.path)
        assertEquals("Bearer sk-test", request.getHeader("Authorization"))
    }

    @Test
    fun omitsAuthHeaderWithoutKey() {
        server.enqueue(MockResponse().setBody("""{"data":[]}"""))
        client.listModels(config.copy(apiKey = ""))
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun reportsServerErrorMessage() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":{"message":"Invalid API key"}}"""))
        val e = runCatching { client.listModels(config) }.exceptionOrNull()
        assertTrue(e is ApiException)
        assertTrue(e!!.message!!, e.message!!.contains("Invalid API key"))
    }

    @Test
    fun retriesWithoutTemperatureWhenRejected() {
        server.enqueue(
            MockResponse().setResponseCode(400)
                .setBody("""{"error":{"message":"Unsupported value: 'temperature' does not support 0.9"}}"""),
        )
        server.enqueue(MockResponse().setBody("""{"choices":[{"message":{"role":"assistant","content":"hi"}}]}"""))

        assertEquals("hi", client.chat(config, "m", listOf(ChatMessage("user", "hello"))))
        assertTrue(server.takeRequest().body.readUtf8().contains("temperature"))
        val retry = server.takeRequest()
        assertEquals("/v1/chat/completions", retry.path)
        assertTrue(!retry.body.readUtf8().contains("temperature"))
    }

    @Test
    fun generatesWordListEndToEnd() {
        val content = "```json\\n{\\\"title\\\": \\\"Deep Blue\\\", \\\"words\\\": [\\\"Shark\\\", \\\"Whale\\\", \\\"Coral\\\", \\\"Kelp\\\", \\\"Squid\\\", \\\"Orca\\\", \\\"Clam\\\", \\\"Reef\\\", \\\"Tide\\\", \\\"Wave\\\", \\\"Crab\\\", \\\"Eel\\\"]}\\n```"
        server.enqueue(MockResponse().setBody("""{"choices":[{"message":{"content":"$content"}}]}"""))

        val result = WordListGenerator(client).generate(config, "m", "ocean", Difficulty.EASY)
        assertEquals("Deep Blue", result.title)
        assertEquals(12, result.words.size)
        assertEquals("SHARK", result.words.first().letters)
    }

    @Test
    fun normalizesBaseUrls() {
        assertEquals("https://api.openai.com/v1", OpenAiClient.normalizeBaseUrl("api.openai.com/v1/"))
        assertEquals("http://10.0.0.2:11434/v1", OpenAiClient.normalizeBaseUrl(" http://10.0.0.2:11434/v1/chat/completions "))
        assertEquals("https://x.ai/v1", OpenAiClient.normalizeBaseUrl("https://x.ai/v1/models"))
    }
}
