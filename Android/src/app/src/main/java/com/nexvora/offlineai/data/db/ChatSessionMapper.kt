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

package com.nexvora.offlineai.data.db

import com.google.protobuf.InvalidProtocolBufferException
import com.nexvora.offlineai.proto.ChatMessageProto
import com.nexvora.offlineai.proto.ChatSessionProto
import com.nexvora.offlineai.proto.ChatSideProto

/** Pure conversion between the app's `ChatSessionProto` model and Room entities. */
object ChatSessionMapper {
  const val DEFAULT_TITLE = "New Chat Session"
  const val MAX_TITLE_LENGTH = 80
  const val PREVIEW_LENGTH = 120

  fun roleOf(side: ChatSideProto): String =
    when (side) {
      ChatSideProto.CHAT_SIDE_USER -> "USER"
      ChatSideProto.CHAT_SIDE_MODEL -> "MODEL"
      ChatSideProto.CHAT_SIDE_SYSTEM -> "SYSTEM"
      else -> "UNSPECIFIED"
    }

  fun sideOf(role: String): ChatSideProto =
    when (role) {
      "USER" -> ChatSideProto.CHAT_SIDE_USER
      "MODEL" -> ChatSideProto.CHAT_SIDE_MODEL
      "SYSTEM" -> ChatSideProto.CHAT_SIDE_SYSTEM
      else -> ChatSideProto.CHAT_SIDE_UNSPECIFIED
    }

  /** Normalizes a user-entered title. Returns null if nothing usable remains. */
  fun normalizeTitle(raw: String): String? =
    raw.trim().replace(Regex("\\s+"), " ").take(MAX_TITLE_LENGTH).ifBlank { null }

  fun previewOf(session: ChatSessionProto): String =
    session.messagesList
      .lastOrNull { it.messageType == "TEXT" && it.content.isNotBlank() }
      ?.content
      ?.trim()
      ?.replace(Regex("\\s+"), " ")
      ?.take(PREVIEW_LENGTH) ?: ""

  /**
   * Builds the conversation row. A title the user chose by renaming is kept; otherwise the title
   * of [session] is used.
   */
  fun toConversation(
    session: ChatSessionProto,
    existing: ConversationEntity?,
    now: Long,
  ): ConversationEntity {
    val keepCustomTitle = existing?.isTitleCustom == true
    val updatedAt = if (session.timestampMs > 0) session.timestampMs else now
    return ConversationEntity(
      id = session.sessionId,
      title =
        if (keepCustomTitle) existing!!.title
        else normalizeTitle(session.title) ?: DEFAULT_TITLE,
      taskId = session.taskId,
      modelName = session.originalModel,
      createdAt = existing?.createdAt ?: updatedAt,
      updatedAt = updatedAt,
      isTitleCustom = keepCustomTitle,
      messageCount = session.messagesCount,
      preview = previewOf(session),
    )
  }

  /**
   * Builds message rows. Messages that already existed at the same position keep their original
   * timestamp ([previousTimes]); new ones get [now].
   */
  fun toMessages(
    session: ChatSessionProto,
    previousTimes: Map<Int, Long>,
    now: Long,
  ): List<MessageEntity> =
    session.messagesList.mapIndexed { index, message ->
      MessageEntity(
        conversationId = session.sessionId,
        position = index,
        role = roleOf(message.side),
        type = message.messageType,
        content = message.content,
        latencyMs = message.latencyMs,
        createdAt = previousTimes[index] ?: now,
        payload = message.toByteArray(),
      )
    }

  /** Rebuilds a message from its row; falls back to the text columns if the payload is unreadable. */
  fun toMessageProto(entity: MessageEntity): ChatMessageProto =
    try {
      ChatMessageProto.parseFrom(entity.payload)
    } catch (e: InvalidProtocolBufferException) {
      ChatMessageProto.newBuilder()
        .setMessageType(entity.type)
        .setContent(entity.content)
        .setSide(sideOf(entity.role))
        .setLatencyMs(entity.latencyMs)
        .build()
    }

  fun toSession(
    conversation: ConversationEntity,
    messages: List<MessageEntity>,
  ): ChatSessionProto =
    ChatSessionProto.newBuilder()
      .setSessionId(conversation.id)
      .setTitle(conversation.title)
      .setTimestampMs(conversation.updatedAt)
      .setOriginalModel(conversation.modelName)
      .setTaskId(conversation.taskId)
      .addAllMessages(messages.sortedBy { it.position }.map { toMessageProto(it) })
      .build()
}
