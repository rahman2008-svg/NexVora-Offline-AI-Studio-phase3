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

import androidx.datastore.core.DataStore
import com.nexvora.offlineai.proto.UserData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * One-time move of chat sessions that older builds kept in the proto DataStore into Room. It runs
 * at app start, is skipped once finished, and never deletes the old data unless every session
 * was copied.
 */
@Singleton
class LegacyChatImporter
@Inject
constructor(
  private val userData: DataStore<UserData>,
  private val target: RoomChatSessionRepository,
  private val preferences: PreferenceRepository,
) {
  /** Returns how many sessions were imported. */
  suspend fun importIfNeeded(): Int {
    if (preferences.getBoolean(PrefKeys.LEGACY_IMPORT_DONE, false)) return 0
    // With history turned off nothing can be written, so try again later.
    if (!preferences.getBoolean(PrefKeys.AUTO_SAVE_HISTORY, true)) return 0
    val legacy = userData.data.first().chatSessionsList
    var imported = 0
    for (session in legacy) {
      target.saveChatSession(session)
      if (target.getConversation(session.sessionId) != null) imported++
    }
    if (imported == legacy.size) {
      if (legacy.isNotEmpty()) {
        userData.updateData { it.toBuilder().clearChatSessions().build() }
      }
      preferences.putBoolean(PrefKeys.LEGACY_IMPORT_DONE, true)
    }
    return imported
  }
}
