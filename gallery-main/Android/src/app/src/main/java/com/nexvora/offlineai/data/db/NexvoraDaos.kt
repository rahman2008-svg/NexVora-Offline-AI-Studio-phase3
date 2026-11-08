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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
  @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
  fun observeAll(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT :limit")
  fun observeRecent(limit: Int): Flow<List<ConversationEntity>>

  /** [pattern] must already be a LIKE pattern built with [SearchQuery.toLikePattern]. */
  @Query(
    """
    SELECT DISTINCT c.* FROM conversations c
    LEFT JOIN messages m ON m.conversationId = c.id
    WHERE c.title LIKE :pattern ESCAPE '\' OR m.content LIKE :pattern ESCAPE '\'
    ORDER BY c.updatedAt DESC
    """
  )
  fun observeSearch(pattern: String): Flow<List<ConversationEntity>>

  @Query(
    """
    SELECT DISTINCT c.* FROM conversations c
    LEFT JOIN messages m ON m.conversationId = c.id
    WHERE c.title LIKE :pattern ESCAPE '\' OR m.content LIKE :pattern ESCAPE '\'
    ORDER BY c.updatedAt DESC
    """
  )
  suspend fun search(pattern: String): List<ConversationEntity>

  @Query("SELECT * FROM conversations WHERE id = :id")
  suspend fun getById(id: String): ConversationEntity?

  @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
  suspend fun getAll(): List<ConversationEntity>

  @Upsert suspend fun upsert(conversation: ConversationEntity)

  @Query("UPDATE conversations SET title = :title, isTitleCustom = 1 WHERE id = :id")
  suspend fun rename(id: String, title: String): Int

  @Query("DELETE FROM conversations WHERE id = :id") suspend fun delete(id: String): Int

  @Query("DELETE FROM conversations") suspend fun deleteAll(): Int

  @Query("SELECT COUNT(*) FROM conversations") fun observeCount(): Flow<Int>

  @Query("SELECT COUNT(*) FROM conversations") suspend fun count(): Int
}

@Dao
interface MessageDao {
  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY position ASC")
  suspend fun getForConversation(conversationId: String): List<MessageEntity>

  @Query("SELECT position, createdAt FROM messages WHERE conversationId = :conversationId")
  suspend fun getPositionTimes(conversationId: String): List<MessagePositionTime>

  @Insert suspend fun insertAll(messages: List<MessageEntity>)

  @Query("DELETE FROM messages WHERE conversationId = :conversationId")
  suspend fun deleteForConversation(conversationId: String): Int

  @Query("SELECT COUNT(*) FROM messages") suspend fun count(): Int

  @Query("SELECT COUNT(*) FROM messages") fun observeCount(): Flow<Int>
}

@Dao
interface ModelMetadataDao {
  @Query("SELECT * FROM model_metadata ORDER BY displayName ASC")
  fun observeAll(): Flow<List<ModelMetadataEntity>>

  @Query("SELECT * FROM model_metadata ORDER BY displayName ASC")
  suspend fun getAll(): List<ModelMetadataEntity>

  @Query("SELECT * FROM model_metadata WHERE name = :name")
  suspend fun get(name: String): ModelMetadataEntity?

  @Upsert suspend fun upsertAll(models: List<ModelMetadataEntity>)

  @Query("UPDATE model_metadata SET lastUsedAt = :time WHERE name = :name")
  suspend fun markUsed(name: String, time: Long): Int

  @Query("DELETE FROM model_metadata") suspend fun deleteAll(): Int
}

@Dao
interface PreferenceDao {
  @Query("SELECT value FROM user_preferences WHERE `key` = :key")
  suspend fun get(key: String): String?

  @Query("SELECT value FROM user_preferences WHERE `key` = :key")
  fun observe(key: String): Flow<String?>

  @Upsert suspend fun put(preference: UserPreferenceEntity)

  @Query("DELETE FROM user_preferences WHERE `key` = :key") suspend fun remove(key: String): Int

  @Query("DELETE FROM user_preferences") suspend fun deleteAll(): Int
}
