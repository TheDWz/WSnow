package com.wsnow.engine.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ApiConfig(val baseUrl: String, val apiKey: String)

data class ChatMessage(val role: String, val content: String)

class ApiException(message: String, val statusCode: Int? = null) : Exception(message)

/**
 * Minimal client for any server that speaks the OpenAI REST API
 * (OpenAI, OpenRouter, Groq, Together, Ollama, LM Studio, llama.cpp, vLLM, …).
 * Calls are blocking; run them off the main thread.
 */
class OpenAiClient(private val http: OkHttpClient = defaultHttpClient()) {

    fun listModels(config: ApiConfig): List<String> {
        val base = normalizeBaseUrl(config.baseUrl)
        val request = requestBuilder(config, "$base/models").get().build()
        val body = execute(request, base)
        val root = parse(body)
        val data = (root as? JsonObject)?.get("data") ?: (root as? JsonArray)
            ?: (root as? JsonObject)?.get("models")
            ?: throw ApiException("Unexpected /models response: ${body.take(200)}")
        return data.jsonArray.mapNotNull { item ->
            when (item) {
                is JsonPrimitive -> item.contentOrNull
                is JsonObject -> (item["id"] ?: item["name"] ?: item["model"])?.jsonPrimitive?.contentOrNull
                else -> null
            }
        }.distinct().sortedBy { it.lowercase() }
    }

    /** Sends a chat completion and returns the assistant's text. */
    fun chat(
        config: ApiConfig,
        model: String,
        messages: List<ChatMessage>,
        temperature: Double? = 0.9,
    ): String {
        val base = normalizeBaseUrl(config.baseUrl)
        val payload = buildJsonObject {
            put("model", model)
            put("messages", buildJsonArray {
                messages.forEach { m ->
                    add(buildJsonObject {
                        put("role", m.role)
                        put("content", m.content)
                    })
                }
            })
            if (temperature != null) put("temperature", temperature)
            put("stream", false)
        }
        val request = requestBuilder(config, "$base/chat/completions")
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .build()

        val body = try {
            execute(request, base)
        } catch (e: ApiException) {
            // Some models (e.g. reasoning models) only accept the default temperature.
            if (temperature != null && e.statusCode == 400 && e.message.orEmpty().contains("temperature", true)) {
                return chat(config, model, messages, temperature = null)
            }
            throw e
        }

        val root = parse(body).jsonObject
        val message = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
            ?: throw ApiException("Response had no choices: ${body.take(200)}")
        val content = message.textOf("content")
        return content.ifBlank { message.textOf("reasoning_content") }.ifBlank {
            throw ApiException("The model returned an empty reply.")
        }
    }

    private fun JsonObject.textOf(key: String): String = when (val v = this[key]) {
        is JsonPrimitive -> v.contentOrNull.orEmpty()
        // Content can also be an array of parts: [{"type":"text","text":"..."}]
        is JsonArray -> v.joinToString("") { part ->
            (part as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()
        }
        else -> ""
    }

    private fun requestBuilder(config: ApiConfig, url: String): Request.Builder {
        val builder = try {
            Request.Builder().url(url)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid base URL: ${config.baseUrl}")
        }
        builder.header("Accept", "application/json")
        if (config.apiKey.isNotBlank()) builder.header("Authorization", "Bearer ${config.apiKey.trim()}")
        return builder
    }

    private fun execute(request: Request, base: String): String {
        try {
            http.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = errorMessage(body) ?: body.take(300).ifBlank { response.message }
                    val hint = when {
                        response.code == 401 || response.code == 403 -> " (check your API key)"
                        response.code == 404 && !base.contains("/v1") ->
                            " (most servers expect the base URL to end in /v1)"
                        else -> ""
                    }
                    throw ApiException("HTTP ${response.code}: $detail$hint", response.code)
                }
                return body
            }
        } catch (e: IOException) {
            throw ApiException("Could not reach ${request.url.host}: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun errorMessage(body: String): String? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        when (val err = root["error"]) {
            is JsonObject -> err["message"]?.jsonPrimitive?.contentOrNull
            is JsonPrimitive -> err.contentOrNull
            else -> root["message"]?.jsonPrimitive?.contentOrNull
        }
    }.getOrNull()

    private fun parse(body: String): JsonElement = try {
        json.parseToJsonElement(body)
    } catch (e: Exception) {
        throw ApiException("Server did not return JSON: ${body.take(200)}")
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
        private val json = Json { ignoreUnknownKeys = true; isLenient = true }

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            // Local models on modest hardware can take a while to answer.
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        /**
         * Tidies what the user typed: adds a scheme, drops trailing slashes, and strips an
         * endpoint path pasted by mistake (".../chat/completions", ".../models").
         */
        fun normalizeBaseUrl(raw: String): String {
            var url = raw.trim()
            if (url.isEmpty()) throw ApiException("Set a base URL in Settings first.")
            if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) url = "https://$url"
            url = url.trimEnd('/')
            for (suffix in listOf("/chat/completions", "/completions", "/models")) {
                if (url.endsWith(suffix, true)) url = url.dropLast(suffix.length)
            }
            return url.trimEnd('/')
        }
    }
}
