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
import com.nexvora.offlineai.common.getModelStorageDir
import com.nexvora.offlineai.data.LocalStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Numbers shown by the dashboard and the Privacy Center. */
data class LocalDataStats(
  val conversationCount: Int,
  val messageCount: Int,
  val databaseBytes: Long,
  val modelsBytes: Long,
  val cacheBytes: Long,
)

/** Measures and deletes the data this app keeps on the device. Nothing here touches the network. */
@Singleton
class LocalDataManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
  private val db: NexvoraDatabase,
  private val chats: RoomChatSessionRepository,
  private val preferences: PreferenceRepository,
  private val modelMetadata: ModelMetadataRepository,
) {
  suspend fun stats(): LocalDataStats =
    withContext(Dispatchers.IO) {
      val dbFile = context.getDatabasePath(NexvoraDatabase.FILE_NAME)
      val dbBytes =
        listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm")).sumOf {
          if (it.exists()) it.length() else 0L
        }
      LocalDataStats(
        conversationCount = db.conversationDao().count(),
        messageCount = db.messageDao().count(),
        databaseBytes = dbBytes,
        modelsBytes = LocalStorage.sizeOf(getModelStorageDir(context)),
        cacheBytes = LocalStorage.sizeOf(context.cacheDir),
      )
    }

  /** Deletes every saved conversation and message. Downloaded models are not touched. */
  suspend fun clearChatHistory() {
    chats.clearAllChatSessions()
  }

  /**
   * Deletes chat history, stored preferences, model metadata and cached files. Downloaded model
   * files are kept; delete them from the Model Hub.
   */
  suspend fun clearAllLocalData() {
    chats.clearAllChatSessions()
    preferences.clearAll()
    modelMetadata.clearAll()
    withContext(Dispatchers.IO) { LocalStorage.clearDirectory(context.cacheDir) }
  }
}
