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

import androidx.room.withTransaction
import com.nexvora.offlineai.data.ChatSessionRepository
import com.nexvora.offlineai.proto.ChatSessionProto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed chat history. It implements the same [ChatSessionRepository] interface the upstream
 * chat code already uses, so the existing chat screens save and restore their sessions here without
 * any change in the inference code.
 *
 * Extra functions (search, rename, summaries) are used by the NexVora history screens.
 */
@Singleton
class RoomChatSessionRepository
@Inject
constructor(
  private val db: NexvoraDatabase,
  private val preferences: PreferenceRepository,
) : ChatSessionRepository {
  private val conversationDao = db.conversationDao()
  private val messageDao = db.messageDao()

  /** All sessions, newest first, including their messages (needed to restore a chat). */
  override val chatSessions: Flow<List<ChatSessionProto>> =
    conversationDao.observeAll().map { rows -> rows.map { loadSession(it) } }

  override suspend fun getAllChatSessions(): List<ChatSessionProto> =
    conversationDao.getAll().map { loadSession(it) }

  override suspend fun getChatSession(sessionId: String): ChatSessionProto? =
    conversationDao.getById(sessionId)?.let { loadSession(it) }

  override suspend fun saveChatSession(session: ChatSessionProto) {
    if (session.sessionId.isEmpty()) return
    // The user can turn history off in Settings; then nothing is written.
    if (!preferences.getBoolean(PrefKeys.AUTO_SAVE_HISTORY, true)) return
    val now = System.currentTimeMillis()
    db.withTransaction {
      val existing = conversationDao.getById(session.sessionId)
      val previousTimes =
        if (existing == null) emptyMap()
        else messageDao.getPositionTimes(session.sessionId).associate { it.position to it.createdAt }
      conversationDao.upsert(ChatSessionMapper.toConversation(session, existing, now))
      messageDao.deleteForConversation(session.sessionId)
      messageDao.insertAll(ChatSessionMapper.toMessages(session, previousTimes, now))
    }
  }

  override suspend fun deleteChatSession(sessionId: String) {
    conversationDao.delete(sessionId)
  }

  override suspend fun clearAllChatSessions() {
    conversationDao.deleteAll()
  }

  override suspend fun renameChatSession(sessionId: String, title: String): Boolean {
    val normalized = ChatSessionMapper.normalizeTitle(title) ?: return false
    return conversationDao.rename(sessionId, normalized) > 0
  }

  // ---- Lightweight APIs for list screens (no message loading) ----

  fun observeConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

  fun observeRecent(limit: Int): Flow<List<ConversationEntity>> = conversationDao.observeRecent(limit)

  /** Blank [query] returns every conversation. */
  fun observeSearch(query: String): Flow<List<ConversationEntity>> {
    val pattern = SearchQuery.toLikePattern(query) ?: return conversationDao.observeAll()
    return conversationDao.observeSearch(pattern)
  }

  suspend fun search(query: String): List<ConversationEntity> {
    val pattern = SearchQuery.toLikePattern(query) ?: return conversationDao.getAll()
    return conversationDao.search(pattern)
  }

  suspend fun getConversation(id: String): ConversationEntity? = conversationDao.getById(id)

  suspend fun getMessages(id: String): List<MessageEntity> = messageDao.getForConversation(id)

  fun observeConversationCount(): Flow<Int> = conversationDao.observeCount()

  private suspend fun loadSession(conversation: ConversationEntity): ChatSessionProto =
    ChatSessionMapper.toSession(conversation, messageDao.getForConversation(conversation.id))
}
