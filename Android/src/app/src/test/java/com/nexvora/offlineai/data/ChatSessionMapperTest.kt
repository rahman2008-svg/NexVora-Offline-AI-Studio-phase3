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

package com.nexvora.offlineai.data

import com.nexvora.offlineai.data.db.ChatSessionMapper
import com.nexvora.offlineai.data.db.ConversationEntity
import com.nexvora.offlineai.data.db.MessageEntity
import com.nexvora.offlineai.proto.ChatMessageProto
import com.nexvora.offlineai.proto.ChatSessionProto
import com.nexvora.offlineai.proto.ChatSideProto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSessionMapperTest {
  private fun message(text: String, side: ChatSideProto, type: String = "TEXT") =
    ChatMessageProto.newBuilder().setMessageType(type).setContent(text).setSide(side).setLatencyMs(12.5f).build()

  private fun session(title: String = "Hello", ts: Long = 1_000L) =
    ChatSessionProto.newBuilder()
      .setSessionId("s1")
      .setTitle(title)
      .setTimestampMs(ts)
      .setOriginalModel("gemma")
      .setTaskId("llm_chat")
      .addMessages(message("Hi there", ChatSideProto.CHAT_SIDE_USER))
      .addMessages(message("Hello! How can I help?", ChatSideProto.CHAT_SIDE_MODEL))
      .build()

  @Test
  fun roundTrip_keepsEveryMessageAndField() {
    val s = session()
    val conv = ChatSessionMapper.toConversation(s, existing = null, now = 5_000L)
    val msgs = ChatSessionMapper.toMessages(s, emptyMap(), now = 5_000L)
    val back = ChatSessionMapper.toSession(conv, msgs)
    assertEquals(s.sessionId, back.sessionId)
    assertEquals(s.title, back.title)
    assertEquals(s.originalModel, back.originalModel)
    assertEquals(s.taskId, back.taskId)
    assertEquals(s.messagesList, back.messagesList)
  }

  @Test
  fun conversation_usesSessionTimestamp_andCountsMessages() {
    val conv = ChatSessionMapper.toConversation(session(ts = 1_000L), null, now = 9_999L)
    assertEquals(1_000L, conv.createdAt)
    assertEquals(1_000L, conv.updatedAt)
    assertEquals(2, conv.messageCount)
  }

  @Test
  fun conversation_withoutTimestamp_usesNow() {
    val conv = ChatSessionMapper.toConversation(session(ts = 0L), null, now = 7_000L)
    assertEquals(7_000L, conv.updatedAt)
  }

  @Test
  fun existingConversation_keepsCreatedAt() {
    val first = ChatSessionMapper.toConversation(session(ts = 1_000L), null, 1_000L)
    val second = ChatSessionMapper.toConversation(session(ts = 2_000L), first, 2_000L)
    assertEquals(1_000L, second.createdAt)
    assertEquals(2_000L, second.updatedAt)
  }

  @Test
  fun customTitle_isNotOverwrittenByAutomaticTitle() {
    val renamed = ConversationEntity("s1", "My title", "llm_chat", "gemma", 1L, 1L, isTitleCustom = true)
    val conv = ChatSessionMapper.toConversation(session(title = "Auto title"), renamed, now = 3L)
    assertEquals("My title", conv.title)
    assertTrue(conv.isTitleCustom)
  }

  @Test
  fun automaticTitle_isUsedWhenNotRenamed() {
    val conv = ChatSessionMapper.toConversation(session(title = "Auto title"), null, now = 3L)
    assertEquals("Auto title", conv.title)
    assertEquals(false, conv.isTitleCustom)
  }

  @Test
  fun blankTitle_fallsBackToDefault() {
    val conv = ChatSessionMapper.toConversation(session(title = "   "), null, now = 3L)
    assertEquals(ChatSessionMapper.DEFAULT_TITLE, conv.title)
  }

  @Test
  fun messages_keepOldTimestamps_andStampNewOnes() {
    val msgs = ChatSessionMapper.toMessages(session(), previousTimes = mapOf(0 to 111L), now = 999L)
    assertEquals(111L, msgs[0].createdAt)
    assertEquals(999L, msgs[1].createdAt)
    assertEquals(listOf(0, 1), msgs.map { it.position })
    assertEquals(listOf("USER", "MODEL"), msgs.map { it.role })
  }

  @Test
  fun preview_isLastTextMessage_trimmedAndSingleLine() {
    val s =
      session()
        .toBuilder()
        .addMessages(message("  line one\n\n  line   two  ", ChatSideProto.CHAT_SIDE_MODEL))
        .addMessages(message("ignored warning", ChatSideProto.CHAT_SIDE_SYSTEM, type = "WARNING"))
        .build()
    assertEquals("line one line two", ChatSessionMapper.previewOf(s))
  }

  @Test
  fun preview_isCutToMaxLength() {
    val long = "x".repeat(500)
    val s = session().toBuilder().addMessages(message(long, ChatSideProto.CHAT_SIDE_MODEL)).build()
    assertEquals(ChatSessionMapper.PREVIEW_LENGTH, ChatSessionMapper.previewOf(s).length)
  }

  @Test
  fun preview_isEmptyWithoutTextMessages() {
    val s = ChatSessionProto.newBuilder().setSessionId("e").build()
    assertEquals("", ChatSessionMapper.previewOf(s))
  }

  @Test
  fun normalizeTitle_trimsCollapsesAndLimits() {
    assertEquals("a b c", ChatSessionMapper.normalizeTitle("  a   b \n c "))
    assertNull(ChatSessionMapper.normalizeTitle("    "))
    assertEquals(ChatSessionMapper.MAX_TITLE_LENGTH, ChatSessionMapper.normalizeTitle("z".repeat(300))!!.length)
  }

  @Test
  fun corruptPayload_fallsBackToTextColumns() {
    val entity =
      MessageEntity(
        conversationId = "s1",
        position = 0,
        role = "MODEL",
        type = "TEXT",
        content = "recovered",
        latencyMs = 3f,
        createdAt = 1L,
        payload = byteArrayOf(0x0F, 0x7F, 0x01),
      )
    val proto = ChatSessionMapper.toMessageProto(entity)
    assertEquals("recovered", proto.content)
    assertEquals(ChatSideProto.CHAT_SIDE_MODEL, proto.side)
    assertEquals("TEXT", proto.messageType)
  }

  @Test
  fun roles_mapBothWays() {
    for (side in listOf(ChatSideProto.CHAT_SIDE_USER, ChatSideProto.CHAT_SIDE_MODEL, ChatSideProto.CHAT_SIDE_SYSTEM)) {
      assertEquals(side, ChatSessionMapper.sideOf(ChatSessionMapper.roleOf(side)))
    }
    assertEquals(ChatSideProto.CHAT_SIDE_UNSPECIFIED, ChatSessionMapper.sideOf("nonsense"))
  }
}
