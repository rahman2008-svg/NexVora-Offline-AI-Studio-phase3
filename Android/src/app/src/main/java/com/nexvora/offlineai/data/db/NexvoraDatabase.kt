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

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

/**
 * NexVora's local Room database.
 *
 * Schema versions:
 * - 1: conversations, messages, model_metadata, user_preferences (Phase 3 baseline).
 *
 * Schemas are exported to `app/schemas/` at build time (see `ksp { arg("room.schemaLocation") }`).
 * When the schema changes: bump [version], add a [Migration] to [NexvoraMigrations.ALL], and commit
 * the new schema JSON. Destructive migration is intentionally NOT enabled so user chats are never
 * silently wiped.
 */
@Database(
  entities =
    [
      ConversationEntity::class,
      MessageEntity::class,
      ModelMetadataEntity::class,
      UserPreferenceEntity::class,
    ],
  version = 1,
  exportSchema = true,
)
abstract class NexvoraDatabase : RoomDatabase() {
  abstract fun conversationDao(): ConversationDao

  abstract fun messageDao(): MessageDao

  abstract fun modelMetadataDao(): ModelMetadataDao

  abstract fun preferenceDao(): PreferenceDao

  companion object {
    const val FILE_NAME = "nexvora.db"

    fun create(context: Context): NexvoraDatabase =
      Room.databaseBuilder(context.applicationContext, NexvoraDatabase::class.java, FILE_NAME)
        .addMigrations(*NexvoraMigrations.ALL)
        .build()
  }
}

/** All schema migrations, oldest first. Empty while the schema is at its first version. */
object NexvoraMigrations {
  val ALL: Array<Migration> = emptyArray()
}

/** Builds safe SQL LIKE patterns from free text typed by the user. */
object SearchQuery {
  /** Returns null for blank input. Escapes `%`, `_` and the escape character itself. */
  fun toLikePattern(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val escaped = buildString {
      for (c in trimmed) {
        if (c == '\\' || c == '%' || c == '_') append('\\')
        append(c)
      }
    }
    return "%$escaped%"
  }
}
