/*
 * Copyright 2026 NexVora Lab's Ofc
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

package com.nexvora.offlineai.ui.nexvora.tools

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvora.offlineai.common.processLlmResponse
import com.nexvora.offlineai.data.BuiltInTaskId
import com.nexvora.offlineai.data.Model
import com.nexvora.offlineai.data.Task
import com.nexvora.offlineai.data.db.PrefKeys
import com.nexvora.offlineai.data.db.PreferenceRepository
import com.nexvora.offlineai.data.db.RoomChatSessionRepository
import com.nexvora.offlineai.proto.ChatMessageProto
import com.nexvora.offlineai.proto.ChatSessionProto
import com.nexvora.offlineai.proto.ChatSideProto
import com.nexvora.offlineai.runtime.runtimeHelper
import com.nexvora.offlineai.ui.modelmanager.ModelManagerViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "NexvoraTools"
private const val INIT_TIMEOUT_MS = 180_000L
private const val POLL_MS = 200L

/** Task id prefix used for conversations created by the AI tools. */
const val TOOL_TASK_PREFIX = "nexvora_tool:"

enum class ToolError { NO_TASK, MODEL_INIT_FAILED, INFERENCE_FAILED }

data class ToolUiState(
  val preparing: Boolean = false,
  val running: Boolean = false,
  val output: String = "",
  val error: ToolError? = null,
  val errorDetail: String = "",
  val savedToHistory: Boolean = false,
)

/**
 * Runs the AI productivity tools on the on-device LLM through the app's existing runtime helper
 * (LiteRT-LM). No output is ever produced without the model: if no model is installed or loading
 * fails, the screen shows an error instead.
 */
@HiltViewModel
class ProductivityViewModel
@Inject
constructor(
  private val chats: RoomChatSessionRepository,
  private val preferences: PreferenceRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(ToolUiState())
  val state: StateFlow<ToolUiState> = _state.asStateFlow()

  val defaultModelName: StateFlow<String> =
    preferences
      .observeString(PrefKeys.DEFAULT_MODEL, "")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

  val savedTargetLanguage: StateFlow<String> =
    preferences
      .observeString(PrefKeys.TOOLS_TARGET_LANGUAGE, "English")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "English")

  private var job: Job? = null
  private var loadedTask: Task? = null
  private var loadedModel: Model? = null

  fun rememberTargetLanguage(language: String) {
    viewModelScope.launch { preferences.putString(PrefKeys.TOOLS_TARGET_LANGUAGE, language) }
  }

  fun run(
    context: Context,
    modelManager: ModelManagerViewModel,
    model: Model,
    tool: ProductivityTool,
    rawInput: String,
    options: ToolOptions,
  ) {
    val prompt = ProductivityPrompts.build(tool, rawInput, options) ?: return
    if (_state.value.preparing || _state.value.running) return
    val task =
      modelManager.getTaskById(BuiltInTaskId.LLM_PROMPT_LAB)
        ?: modelManager.getTaskById(BuiltInTaskId.LLM_CHAT)
    if (task == null) {
      _state.value = ToolUiState(error = ToolError.NO_TASK)
      return
    }
    loadedTask = task
    loadedModel = model
    _state.value = ToolUiState(preparing = true)

    job =
      viewModelScope.launch(Dispatchers.Default) {
        try {
          // onDone fires when the model is ready (also when it was already loaded); onError when
          // loading fails. If another screen is already loading the same model neither fires, so
          // we also watch the model instance itself.
          val finished = CompletableDeferred<String?>()
          modelManager.initializeModel(
            context,
            task,
            model,
            onDone = { finished.complete(null) },
            onError = { finished.complete(it.ifBlank { "unknown error" }) },
          )
          withTimeout(INIT_TIMEOUT_MS) {
            while (!finished.isCompleted && model.instance == null) delay(POLL_MS)
          }
          val initError = if (finished.isCompleted) finished.await() else null
          if (initError != null || model.instance == null) {
            fail(ToolError.MODEL_INIT_FAILED, initError)
            return@launch
          }
        } catch (e: Exception) {
          Log.e(TAG, "Model initialization failed: ${e.message}", e)
          fail(ToolError.MODEL_INIT_FAILED, e.message)
          return@launch
        }

        val startedAt = System.currentTimeMillis()
        var response = ""
        try {
          model.runtimeHelper.resetConversation(model = model, supportImage = false, supportAudio = false)
          _state.update { it.copy(preparing = false, running = true) }
          model.runtimeHelper.runInference(
            model = model,
            input = prompt,
            resultListener = { partial: String, done: Boolean, _: String? ->
              response = processLlmResponse(response = "$response$partial")
              _state.update { it.copy(output = response) }
              if (done) {
                _state.update { it.copy(running = false) }
                saveToHistory(tool, ProductivityPrompts.trimInput(rawInput), response, model, System.currentTimeMillis() - startedAt)
              }
            },
            cleanUpListener = { _state.update { it.copy(preparing = false, running = false) } },
            onError = { message -> fail(ToolError.INFERENCE_FAILED, message) },
            coroutineScope = viewModelScope,
          )
        } catch (e: Exception) {
          Log.e(TAG, "Inference failed: ${e.message}", e)
          fail(ToolError.INFERENCE_FAILED, e.message)
        }
      }
  }

  fun stop(model: Model) {
    runCatching { model.runtimeHelper.stopResponse(model) }
    _state.update { it.copy(running = false, preparing = false) }
  }

  fun clearOutput() {
    if (!_state.value.running && !_state.value.preparing) _state.value = ToolUiState()
  }

  /** Frees the model after leaving the screen, which matters on devices with ~6 GB of RAM. */
  fun release(context: Context, modelManager: ModelManagerViewModel) {
    val task = loadedTask ?: return
    val model = loadedModel ?: return
    job?.cancel()
    runCatching { model.runtimeHelper.stopResponse(model) }
    modelManager.cleanupModel(context = context, task = task, model = model)
    loadedTask = null
    loadedModel = null
  }

  private fun fail(error: ToolError, detail: String?) {
    _state.update { it.copy(preparing = false, running = false, error = error, errorDetail = detail.orEmpty()) }
  }

  private fun saveToHistory(tool: ProductivityTool, input: String, output: String, model: Model, latencyMs: Long) {
    if (output.isBlank()) return
    viewModelScope.launch(Dispatchers.IO) {
      val now = System.currentTimeMillis()
      val session =
        ChatSessionProto.newBuilder()
          .setSessionId(UUID.randomUUID().toString())
          .setTitle(tool.id.replaceFirstChar { it.uppercase() } + ": " + input.take(30))
          .setTimestampMs(now)
          .setOriginalModel(model.name)
          .setTaskId(TOOL_TASK_PREFIX + tool.id)
          .addMessages(
            ChatMessageProto.newBuilder().setMessageType("TEXT").setContent(input).setSide(ChatSideProto.CHAT_SIDE_USER).build()
          )
          .addMessages(
            ChatMessageProto.newBuilder()
              .setMessageType("TEXT")
              .setContent(output)
              .setSide(ChatSideProto.CHAT_SIDE_MODEL)
              .setLatencyMs(latencyMs.toFloat())
              .setIsMarkdown(true)
              .build()
          )
          .build()
      try {
        chats.saveChatSession(session)
        _state.update { it.copy(savedToHistory = true) }
      } catch (e: Exception) {
        Log.w(TAG, "Could not save tool result: ${e.message}")
      }
    }
  }

  override fun onCleared() {
    job?.cancel()
    super.onCleared()
  }
}
