/*
 * Copyright 2025 Google LLC
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

package com.nexvora.offlineai

import android.os.Bundle
import androidx.annotation.VisibleForTesting

/**
 * NexVora Offline AI Studio: local-only event sink.
 *
 * The upstream app sent usage events to a cloud analytics service. NexVora removes that completely, so
 * this object is now a privacy-preserving no-op: events are never stored and never leave the
 * device. The call sites are kept so that the existing UI and inference code stays untouched.
 * Tests can still set [eventHandler] / [galleryEventSinkForTesting] to observe events.
 */
object GalleryAnalytics {
  /** Optional handler that intercepts every logged event. Only used by unit tests. */
  @VisibleForTesting @Volatile var eventHandler: ((name: String, params: Bundle?) -> Unit)? = null

  /** Optional sink for [logGalleryEvent]. Only used by unit tests. */
  @VisibleForTesting @Volatile var galleryEventSinkForTesting: ((String, Bundle) -> Unit)? = null

  /** Forwards the event to the test handler when one is set; otherwise the event is dropped. */
  fun logEvent(name: String, params: Bundle? = null) {
    eventHandler?.invoke(name, params)
  }

  /** Forwards the event to the test sink when one is set; otherwise the event is dropped. */
  fun logGalleryEvent(name: String, params: Bundle) {
    val sink = galleryEventSinkForTesting
    if (sink != null) {
      sink(name, params)
      return
    }
    logEvent(name, params)
  }
}

/** Always returns the local no-op event sink. Nothing is sent over the network. */
val localEvents: GalleryAnalytics
  get() = GalleryAnalytics

enum class GalleryEvent(val id: String) {
  CAPABILITY_SELECT(id = "capability_select"),
  MODEL_DOWNLOAD(id = "model_download"),
  GENERATE_ACTION(id = "generate_action"),
  BUTTON_CLICKED(id = "button_clicked"),
  SKILL_MANAGEMENT(id = "skill_management"),
  SKILL_EXECUTION(id = "skill_execution"),
  CHAT_HISTORY(id = "chat_history"),
  MCP_MANAGEMENT(id = "mcp_management"),
  MCP_EXECUTION(id = "mcp_execution"),
  MODEL_CONFIG_CHANGE(id = "model_config_change"),
  MODEL_INITIALIZE(id = "model_initialize"),
  INFERENCE_METRICS(id = "inference_metrics"),
  CONTEXT_COMPRESSION(id = "context_compression"),
  AUTO_COMPACTION_ENABLE(id = "auto_compaction_enable"),
  RESOURCE_LINK_CLICK(id = "resource_link_click"),
  OSS_MODEL_IMPORT(id = "oss_model_import"),
}

const val BUTTON_CLICK_EVENT_LIVE_METRICS_TOGGLE = "live_metrics_toggle"

/** Strongly typed action values for [GalleryEvent.BUTTON_CLICKED] telemetry. */
enum class ButtonClickAction(val id: String) {
  TURN_ON("turn_on"),
  TURN_OFF("turn_off"),
}

/** Strongly typed parameter keys for local inference and model lifecycle metrics. */
enum class InferenceMetricsParam(val key: String) {
  MODEL_ID("model_id"),
  MODEL_VERSION("model_version"),
  CAPABILITY_NAME("capability_name"),
  ACCELERATOR("accelerator"),
  STATUS("status"),
  ERROR_CODE("error_code"),
  TURN_INDEX("turn_index"),
  INIT_DURATION_MS("init_duration_ms"),
  SAMPLER_CONFIG("sampler_config"),
  MODEL_CAPABILITIES("model_capabilities"),
  TOTAL_LATENCY_MS("total_latency_ms"),
  TTFT_MS("ttft_ms"),
  DECODE_DURATION_MS("decode_duration_ms"),
  PREFILL_SPEED_TPS("prefill_speed_tps"),
  DECODE_SPEED_TPS("decode_speed_tps"),
  PROMPT_TOKENS("prompt_tokens"),
  OUTPUT_TOKENS("output_tokens"),
  TURN_TOTAL_TOKENS("turn_total_tokens"),
  CONSUMED_CONTEXT_TOKENS("consumed_context_tokens"),
  MAX_CONTEXT_TOKENS("max_context_tokens"),
  PEAK_MEMORY_MB("peak_memory_mb"),
  AVG_MEMORY_MB("avg_memory_mb"),
  LIVE_METRICS_ENABLED("live_metrics_enabled"),
}
