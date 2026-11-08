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

package com.nexvora.offlineai

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nexvora.offlineai.data.db.ModelMetadataRepository
import com.nexvora.offlineai.data.db.ModelSnapshot
import com.nexvora.offlineai.data.db.NexvoraDatabase
import com.nexvora.offlineai.data.db.PrefKeys
import com.nexvora.offlineai.data.db.PreferenceRepository
import com.nexvora.offlineai.data.db.RoomChatSessionRepository
import com.nexvora.offlineai.proto.ChatMessageProto
import com.nexvora.offlineai.proto.ChatSessionProto
import com.nexvora.offlineai.proto.ChatSideProto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a device or emulator (`./gradlew :app:connectedDebugAndroidTest`) against an in-memory
 * Room database. It is NOT executed by the GitHub workflows, which have no emulator.
 */
@RunWith(AndroidJUnit4::class)
class NexvoraDatabaseTest {
  private lateinit var db: NexvoraDatabase
  private lateinit var prefs: PreferenceRepository
  private lateinit var chats: RoomChatSessionRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, NexvoraDatabase::class.java).allowMainThreadQueries().build()
    prefs = PreferenceRepository(db.preferenceDao())
    chats = RoomChatSessionRepository(db, prefs)
  }

  @After fun tearDown() = db.close()

  private fun session(id: String, title: String, vararg texts: String, ts: Long = 1_000L): ChatSessionProto {
    val b = ChatSessionProto.newBuilder().setSessionId(id).setTitle(title).setTimestampMs(ts).setOriginalModel("m").setTaskId("llm_chat")
    texts.forEachIndexed { i, t ->
      b.addMessages(
        ChatMessageProto.newBuilder()
          .setMessageType("TEXT")
          .setContent(t)
          .setSide(if (i % 2 == 0) ChatSideProto.CHAT_SIDE_USER else ChatSideProto.CHAT_SIDE_MODEL)
          .build()
      )
    }
    return b.build()
  }

  @Test
  fun saveAndLoad_roundTripsMessages() = runBlocking {
    val s = session("a", "First", "hello", "hi, how can I help?")
    chats.saveChatSession(s)
    val loaded = chats.getChatSession("a")
    assertNotNull(loaded)
    assertEquals(s.messagesList, loaded!!.messagesList)
    assertEquals("First", loaded.title)
    assertEquals(1, chats.getAllChatSessions().size)
  }

  @Test
  fun resave_replacesMessages_andKeepsOldTimestamps() = runBlocking {
    chats.saveChatSession(session("a", "T", "one"))
    val firstTime = chats.getMessages("a").single().createdAt
    Thread.sleep(5)
    chats.saveChatSession(session("a", "T", "one", "two"))
    val msgs = chats.getMessages("a")
    assertEquals(2, msgs.size)
    assertEquals(firstTime, msgs[0].createdAt)
    assertTrue(msgs[1].createdAt >= firstTime)
  }

  @Test
  fun rename_persists_andSurvivesAutomaticResave() = runBlocking {
    chats.saveChatSession(session("a", "Auto", "hello"))
    assertTrue(chats.renameChatSession("a", "  My   chat "))
    assertEquals("My chat", chats.getConversation("a")!!.title)
    chats.saveChatSession(session("a", "Auto again", "hello", "world"))
    assertEquals("My chat", chats.getConversation("a")!!.title)
  }

  @Test
  fun rename_blankOrUnknown_isRejected() = runBlocking {
    chats.saveChatSession(session("a", "Auto", "hello"))
    assertFalse(chats.renameChatSession("a", "   "))
    assertFalse(chats.renameChatSession("missing", "x"))
  }

  @Test
  fun delete_removesConversationAndMessages() = runBlocking {
    chats.saveChatSession(session("a", "A", "x"))
    chats.saveChatSession(session("b", "B", "y"))
    chats.deleteChatSession("a")
    assertNull(chats.getConversation("a"))
    assertTrue(chats.getMessages("a").isEmpty())
    assertEquals(1, db.messageDao().count())
  }

  @Test
  fun clearAll_removesEverything() = runBlocking {
    chats.saveChatSession(session("a", "A", "x"))
    chats.saveChatSession(session("b", "B", "y"))
    chats.clearAllChatSessions()
    assertEquals(0, db.conversationDao().count())
    assertEquals(0, db.messageDao().count())
  }

  @Test
  fun search_matchesTitleAndMessageText_andEscapesWildcards() = runBlocking {
    chats.saveChatSession(session("a", "Trip plan", "book flights to Dhaka"))
    chats.saveChatSession(session("b", "Recipes", "100% organic"))
    assertEquals(listOf("a"), chats.search("dhaka").map { it.id })
    assertEquals(listOf("a"), chats.search("trip").map { it.id })
    assertEquals(listOf("b"), chats.search("100%").map { it.id })
    assertEquals(0, chats.search("%").count { it.id == "a" })
    assertEquals(2, chats.search("").size)
  }

  @Test
  fun list_isNewestFirst() = runBlocking {
    chats.saveChatSession(session("old", "Old", "x", ts = 1_000L))
    chats.saveChatSession(session("new", "New", "y", ts = 2_000L))
    assertEquals(listOf("new", "old"), chats.observeConversations().first().map { it.id })
  }

  @Test
  fun historyOff_savesNothing() = runBlocking {
    prefs.putBoolean(PrefKeys.AUTO_SAVE_HISTORY, false)
    chats.saveChatSession(session("a", "A", "x"))
    assertEquals(0, db.conversationDao().count())
    prefs.putBoolean(PrefKeys.AUTO_SAVE_HISTORY, true)
    chats.saveChatSession(session("a", "A", "x"))
    assertEquals(1, db.conversationDao().count())
  }

  @Test
  fun preferences_defaultsAndUpdates() = runBlocking {
    assertTrue(prefs.getBoolean(PrefKeys.AUTO_SAVE_HISTORY, true))
    assertEquals("system", prefs.getString(PrefKeys.LANGUAGE, "system"))
    prefs.putString(PrefKeys.LANGUAGE, "bn")
    assertEquals("bn", prefs.getString(PrefKeys.LANGUAGE, "system"))
    prefs.clearAll()
    assertEquals("system", prefs.getString(PrefKeys.LANGUAGE, "system"))
  }

  @Test
  fun modelMetadata_syncAndMarkUsed() = runBlocking {
    val repo = ModelMetadataRepository(db.modelMetadataDao())
    repo.sync(listOf(ModelSnapshot("m1", "Model 1", 1000, "LITERT_LM", 6, true)), now = 10L)
    repo.markUsed("m1", now = 20L)
    repo.sync(listOf(ModelSnapshot("m1", "Model 1", 2000, "LITERT_LM", 6, true)), now = 30L)
    val row = repo.getAll().single()
    assertEquals(10L, row.firstSeenAt)
    assertEquals(30L, row.lastSeenAt)
    assertEquals(20L, row.lastUsedAt)
    assertEquals(2000L, row.sizeBytes)
  }
}
