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

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Keys and defaults of the preferences stored in Room. */
object PrefKeys {
  /** "system", "en" or "bn". */
  const val LANGUAGE = "language"
  /** "true"/"false". When false, chats are not saved to local history. */
  const val AUTO_SAVE_HISTORY = "auto_save_history"
  /** `Model.name` of the model preferred for AI tools, or empty. */
  const val DEFAULT_MODEL = "default_model"
  /** Set once the one-time import of upstream proto chat history finished. */
  const val LEGACY_IMPORT_DONE = "legacy_chat_import_done"
  /** Last translation target language used in the AI tools. */
  const val TOOLS_TARGET_LANGUAGE = "tools_target_language"

  const val LANGUAGE_SYSTEM = "system"
  const val LANGUAGE_ENGLISH = "en"
  const val LANGUAGE_BANGLA = "bn"
}

/** Typed access to [UserPreferenceEntity] rows. */
@Singleton
class PreferenceRepository @Inject constructor(private val dao: PreferenceDao) {
  suspend fun getString(key: String, default: String = ""): String = dao.get(key) ?: default

  fun observeString(key: String, default: String = ""): Flow<String> =
    dao.observe(key).map { it ?: default }

  suspend fun putString(key: String, value: String) {
    dao.put(UserPreferenceEntity(key = key, value = value, updatedAt = System.currentTimeMillis()))
  }

  suspend fun getBoolean(key: String, default: Boolean): Boolean =
    dao.get(key)?.toBooleanStrictOrNull() ?: default

  fun observeBoolean(key: String, default: Boolean): Flow<Boolean> =
    dao.observe(key).map { it?.toBooleanStrictOrNull() ?: default }

  suspend fun putBoolean(key: String, value: Boolean) = putString(key, value.toString())

  suspend fun remove(key: String) {
    dao.remove(key)
  }

  /** Removes every preference (used by "Clear local data"). */
  suspend fun clearAll() {
    dao.deleteAll()
  }
}
