/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hartagis.edgear.server

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import java.io.ByteArrayInputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.text.isEmpty
import kotlin.text.trim
import kotlinx.coroutines.CancellationException

private const val TAG = "LocalInferenceServer"
private const val DEFAULT_PORT = 8080
private const val MAX_TOKENS = 4096
private const val TEMPERATURE = 0.7f
private const val TOP_K = 40
private const val TOP_P = 0.95f

/**
 * OpenAI-compatible local inference server using LiteRT LM.
 *
 * Endpoints:
 * - GET  /v1/models              → list loaded model
 * - POST /v1/chat/completions    → generate (supports stream: true via SSE)
 * - GET  /health                 → server status
 *
 * Uses NanoHTTPD 2.3.1 — lightweight, ~30KB, no framework overhead.
 */
class LocalInferenceServer(
  private val context: Context,
  val serverPort: Int = DEFAULT_PORT,  // must be val for LocalServerManager access
) : NanoHTTPD(serverPort) {

  internal val actualPort get() = getListeningPort().takeIf { it > 0 } ?: serverPort

  private val gson = com.google.gson.Gson()

  // Model state
  private var engine: Engine? = null
  private var conversation: Conversation? = null

  var isRunning: Boolean = false
  var isModelLoaded: Boolean = false
  var loadedModelPath: String? = null

  // ── Server lifecycle ─────────────────────────────────────────

  fun startServer() {
    try {
      start(SOCKET_READ_TIMEOUT, SOCKET_READ_TIMEOUT)
      isRunning = true
      Log.i(TAG, "Server started on port $serverPort")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start server on port $serverPort", e)
      isRunning = false
    }
  }

  fun stopServer() {
    try {
      stop()
      unloadModel()
      isRunning = false
      Log.i(TAG, "Server stopped")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to stop server", e)
    }
  }

  // ── Model management ─────────────────────────────────────────

  fun loadModel(modelPath: String): Result<Unit> {
    if (isModelLoaded) unloadModel()

    return try {
      val engineConfig = EngineConfig(
        modelPath = modelPath,
        backend = Backend.GPU(),
        visionBackend = null,
        audioBackend = null,
        maxNumTokens = MAX_TOKENS,
        cacheDir = context.getExternalFilesDir(null)?.absolutePath,
      )

      engine = Engine(engineConfig).apply { initialize() }
      conversation = engine!!.createConversation(
        ConversationConfig(
          samplerConfig = SamplerConfig(
            topK = TOP_K,
            topP = TOP_P.toDouble(),
            temperature = TEMPERATURE.toDouble(),
          ),
        )
      )

      loadedModelPath = modelPath
      isModelLoaded = true
      Log.i(TAG, "Model loaded: ${modelPath.substringAfterLast("/")} at $serverPort")
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load model: $modelPath", e)
      unloadModel()
      Result.failure(e)
    }
  }

  fun unloadModel() {
    try { conversation?.close() } catch (_: Exception) {}
    try { engine?.close() } catch (_: Exception) {}
    engine = null
    conversation = null
    isModelLoaded = false
    loadedModelPath = null
  }

  fun stopResponse() { conversation?.cancelProcess() }

  // ── HTTP routing ─────────────────────────────────────────────

  override fun serve(session: IHTTPSession): Response {
    val uri = session.uri
    val method = session.method

    val cors = mapOf(
      "Access-Control-Allow-Origin" to "*",
      "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers" to "Content-Type, Authorization",
    )

    return when {
      method == Method.OPTIONS -> {
        val response = newFixedLengthResponse(Status.OK, "text/plain", "")
        cors.forEach { (k, v) -> response.addHeader(k, v) }
        response
      }

      uri == "/v1/models" && method == Method.GET -> handleGetModels(cors)
      uri == "/v1/chat/completions" && method == Method.POST -> handleChatCompletions(session, cors)
      uri == "/health" && method == Method.GET -> handleHealth(cors)

      else -> errorResponse(Status.NOT_FOUND, "Not found", cors)
    }
  }

  // ── Endpoints ────────────────────────────────────────────────

  private fun handleHealth(cors: Map<String, String>): Response {
    val json = gson.toJson(mapOf(
      "status" to if (isRunning) "ok" else "stopped",
      "model_loaded" to isModelLoaded,
      "model" to (loadedModelPath ?: "none"),
    ))
    return jsonResponse(Status.OK, json, cors)
  }

  private fun handleGetModels(cors: Map<String, String>): Response {
    val models = if (isModelLoaded) {
      listOf(mapOf(
        "id" to (loadedModelPath?.substringAfterLast("/") ?: "local_model"),
        "object" to "model",
        "created" to (System.currentTimeMillis() / 1000),
        "owned_by" to "local",
      ))
    } else emptyList()

    val json = gson.toJson(mapOf(
      "object" to "list",
      "data" to models,
    ))
    return jsonResponse(Status.OK, json, cors)
  }

  private fun handleChatCompletions(session: IHTTPSession, cors: Map<String, String>): Response {
    if (!isModelLoaded) {
      return errorResponse(Status.SERVICE_UNAVAILABLE, "No model loaded. Upload a .litertlm model first.", cors)
    }

    val body = mutableMapOf<String, String>()
    session.parseBody(body)
    val payload = body["postData"] ?: ""

    if (payload.isEmpty()) {
      return errorResponse(Status.BAD_REQUEST, "Empty request body", cors)
    }

    val request = try {
      gson.fromJson(payload, ChatCompletionRequest::class.java)
    } catch (e: Exception) {
      return errorResponse(Status.BAD_REQUEST, "Invalid JSON: ${e.message}", cors)
    }

    if (request.messages.isNullOrEmpty()) {
      return errorResponse(Status.BAD_REQUEST, "No messages provided", cors)
    }

    val prompt = buildPrompt(request.messages)

    return if (request.stream == true) {
      handleStreaming(prompt, request.model, cors)
    } else {
      handleNonStreaming(prompt, request.model, cors)
    }
  }

  // ── Prompt building ──────────────────────────────────────────

  private fun buildPrompt(messages: List<ChatMessage>): String {
    return messages.joinToString("\n") { msg ->
      when (msg.role) {
        "system" -> msg.content
        "user" -> msg.content
        "assistant" -> msg.content
        else -> msg.content
      }
    }
  }

  // ── Streaming (SSE via PipedInputStream) ─────────────────────

  private fun handleStreaming(prompt: String, modelId: String?, cors: Map<String, String>): Response {
    Log.i(TAG, "Starting streaming inference")

    val pipedInput = PipedInputStream(8192)
    val pipedOutput = PipedOutputStream(pipedInput)
    val buffer = java.io.BufferedOutputStream(pipedOutput)

    var chunkId = 0

    val callback = object : MessageCallback {
      override fun onMessage(message: Message) {
        val text = message.toString()
        if (text.startsWith("<ctrl")) return

        val chunk = buildStreamChunk(modelId, "$chunkId", text, false)
        val line = "data: ${gson.toJson(chunk)}\n\n"
        try {
          buffer.write(line.toByteArray())
          buffer.flush()
        } catch (_: Exception) {}
        chunkId++
      }

      override fun onDone() {
        try {
          val done = buildStreamChunk(modelId, "done", "", true)
          buffer.write("data: ${gson.toJson(done)}\n\n".toByteArray())
          buffer.write("data: [DONE]\n\n".toByteArray())
          buffer.close()
        } catch (_: Exception) {}
        Log.i(TAG, "Streaming complete")
      }

      override fun onError(throwable: Throwable) {
        val msg = if (throwable is CancellationException) "cancelled" else "error: ${throwable.message}"
        try {
          val err = buildStreamChunk(modelId, "error", msg, true)
          buffer.write("data: ${gson.toJson(err)}\n\n".toByteArray())
          buffer.write("data: [DONE]\n\n".toByteArray())
          buffer.close()
        } catch (_: Exception) {}
      }
    }

    val conv = conversation
      ?: return errorResponse(Status.SERVICE_UNAVAILABLE, "Conversation not ready", cors)

    try {
      val contents = mutableListOf<Content>()
      if (prompt.trim().isNotEmpty()) contents.add(Content.Text(prompt))
      conv.sendMessageAsync(Contents.of(contents), callback, emptyMap())
    } catch (e: Exception) {
      try { buffer.close() } catch (_: Exception) {}
      return errorResponse(Status.INTERNAL_ERROR, "Inference failed: ${e.message}", cors)
    }

    val response = newChunkedResponse(Status.OK, "text/event-stream", pipedInput)
    cors.forEach { (k, v) -> response.addHeader(k, v) }
    return response
  }

  // ── Non-streaming ────────────────────────────────────────────

  private fun handleNonStreaming(prompt: String, modelId: String?, cors: Map<String, String>): Response {
    Log.i(TAG, "Starting non-streaming inference")

    val responseText = StringBuilder()
    val latch = CountDownLatch(1)

    val callback = object : MessageCallback {
      override fun onMessage(message: Message) {
        val text = message.toString()
        if (!text.startsWith("<ctrl")) responseText.append(text)
      }

      override fun onDone() { latch.countDown() }

      override fun onError(throwable: Throwable) {
        Log.e(TAG, "Inference error", throwable)
        latch.countDown()
      }
    }

    val conv = conversation
      ?: return errorResponse(Status.SERVICE_UNAVAILABLE, "Conversation not ready", cors)

    try {
      val contents = mutableListOf<Content>()
      if (prompt.trim().isNotEmpty()) contents.add(Content.Text(prompt))
      conv.sendMessageAsync(Contents.of(contents), callback, emptyMap())
    } catch (e: Exception) {
      return errorResponse(Status.INTERNAL_ERROR, "Inference failed: ${e.message}", cors)
    }

    latch.await(300, TimeUnit.SECONDS)

    val result = mapOf(
      "id" to "chatcmpl-1",
      "object" to "chat.completion",
      "created" to (System.currentTimeMillis() / 1000),
      "model" to (modelId ?: "local_model"),
      "choices" to listOf(
        mapOf(
          "index" to 0,
          "message" to mapOf(
            "role" to "assistant",
            "content" to responseText.toString(),
          ),
          "finish_reason" to "stop",
        )
      ),
      "usage" to mapOf(
        "prompt_tokens" to 0,
        "completion_tokens" to 0,
        "total_tokens" to 0,
      ),
    )

    return jsonResponse(Status.OK, gson.toJson(result), cors)
  }

  // ── Helpers ──────────────────────────────────────────────────

  private fun buildStreamChunk(modelId: String?, id: String, content: String, done: Boolean): Map<String, Any> {
    return mapOf(
      "id" to "chatcmpl-$id",
      "object" to "chat.completion.chunk",
      "created" to (System.currentTimeMillis() / 1000),
      "model" to (modelId ?: "local_model"),
      "choices" to listOf(
        mapOf(
          "index" to 0,
          "delta" to mapOf("content" to content),
          if (done) "finish_reason" to "stop" else "" to null,
        )
      ),
    )
  }

  private fun jsonResponse(status: Status, json: String, headers: Map<String, String>): Response {
    return newFixedLengthResponse(status, "application/json", json).apply {
      headers.forEach { (k, v) -> addHeader(k, v) }
    }
  }

  private fun errorResponse(status: Status, message: String, headers: Map<String, String>): Response {
    return jsonResponse(status, gson.toJson(mapOf(
      "error" to mapOf(
        "message" to message,
        "type" to "invalid_request_error",
      )
    )), headers)
  }

  companion object {
    private const val SOCKET_READ_TIMEOUT = 5000
  }
}

// ── OpenAI-compatible request models ──

data class ChatCompletionRequest(
  val model: String?,
  val messages: List<ChatMessage>?,
  val stream: Boolean?,
  val temperature: Double?,
  val max_tokens: Int?,
  val top_p: Double?,
)

data class ChatMessage(
  val role: String,
  val content: String
)
