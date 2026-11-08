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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A saved conversation (chat session). One row per session. */
@Entity(
  tableName = "conversations",
  indices = [Index(value = ["updatedAt"]), Index(value = ["taskId"])],
)
data class ConversationEntity(
  @PrimaryKey val id: String,
  val title: String,
  /** Task that created the conversation, e.g. `llm_chat` or `nexvora_tool:summarize`. */
  val taskId: String,
  /** Name of the model that was used (matches `Model.name`). */
  val modelName: String,
  val createdAt: Long,
  val updatedAt: Long,
  /** True once the user renamed the conversation; automatic titles then never overwrite it. */
  val isTitleCustom: Boolean = false,
  val messageCount: Int = 0,
  /** Short text of the last text message, for list screens. */
  val preview: String = "",
)

/**
 * One message of a conversation. The text columns make search and list screens cheap; [payload]
 * holds the complete serialized `ChatMessageProto` so a session can be restored losslessly
 * (images, audio clips, files, etc.).
 */
@Entity(
  tableName = "messages",
  foreignKeys =
    [
      ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE,
      )
    ],
  indices = [Index(value = ["conversationId", "position"], unique = true)],
)
data class MessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val conversationId: String,
  val position: Int,
  /** USER, MODEL, SYSTEM or UNSPECIFIED. */
  val role: String,
  /** Upstream message type: TEXT, THINKING, INFO, WARNING, ERROR, ... */
  val type: String,
  val content: String,
  val latencyMs: Float,
  val createdAt: Long,
  @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val payload: ByteArray,
) {
  override fun equals(other: Any?): Boolean =
    other is MessageEntity &&
      id == other.id &&
      conversationId == other.conversationId &&
      position == other.position &&
      role == other.role &&
      type == other.type &&
      content == other.content &&
      latencyMs == other.latencyMs &&
      createdAt == other.createdAt &&
      payload.contentEquals(other.payload)

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + conversationId.hashCode()
    result = 31 * result + position
    result = 31 * result + content.hashCode()
    result = 31 * result + payload.contentHashCode()
    return result
  }
}

/** Metadata the app remembers about a model (installed state, size, last use). */
@Entity(tableName = "model_metadata")
data class ModelMetadataEntity(
  /** `Model.name`. */
  @PrimaryKey val name: String,
  val displayName: String,
  val sizeBytes: Long,
  val runtime: String,
  val minDeviceMemoryGb: Int?,
  val isInstalled: Boolean,
  val firstSeenAt: Long,
  val lastSeenAt: Long,
  val lastUsedAt: Long?,
)

/** A simple key/value user preference. */
@Entity(tableName = "user_preferences")
data class UserPreferenceEntity(
  @PrimaryKey val key: String,
  val value: String,
  val updatedAt: Long,
)

/** Projection used to keep message timestamps stable across re-saves. */
data class MessagePositionTime(val position: Int, val createdAt: Long)
