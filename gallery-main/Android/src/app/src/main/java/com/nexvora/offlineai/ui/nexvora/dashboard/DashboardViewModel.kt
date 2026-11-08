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

package com.nexvora.offlineai.ui.nexvora.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvora.offlineai.data.db.ConversationEntity
import com.nexvora.offlineai.data.db.LocalDataManager
import com.nexvora.offlineai.data.db.LocalDataStats
import com.nexvora.offlineai.data.db.ModelMetadataRepository
import com.nexvora.offlineai.data.db.ModelSnapshot
import com.nexvora.offlineai.data.db.PrefKeys
import com.nexvora.offlineai.data.db.PreferenceRepository
import com.nexvora.offlineai.data.db.RoomChatSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel
@Inject
constructor(
  chats: RoomChatSessionRepository,
  preferences: PreferenceRepository,
  private val localData: LocalDataManager,
  private val modelMetadata: ModelMetadataRepository,
) : ViewModel() {
  val recentChats: StateFlow<List<ConversationEntity>> =
    chats
      .observeRecent(RECENT_LIMIT)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val saveHistoryEnabled: StateFlow<Boolean> =
    preferences
      .observeBoolean(PrefKeys.AUTO_SAVE_HISTORY, true)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  private val _stats = MutableStateFlow<LocalDataStats?>(null)
  val stats: StateFlow<LocalDataStats?> = _stats.asStateFlow()

  fun refreshStats() {
    viewModelScope.launch { _stats.value = runCatching { localData.stats() }.getOrNull() }
  }

  fun syncModels(snapshots: List<ModelSnapshot>) {
    viewModelScope.launch { runCatching { modelMetadata.sync(snapshots) } }
  }

  companion object {
    const val RECENT_LIMIT = 5
  }
}
