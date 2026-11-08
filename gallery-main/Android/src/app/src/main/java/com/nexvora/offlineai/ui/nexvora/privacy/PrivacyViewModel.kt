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

package com.nexvora.offlineai.ui.nexvora.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvora.offlineai.data.db.LocalDataManager
import com.nexvora.offlineai.data.db.LocalDataStats
import com.nexvora.offlineai.data.db.PrefKeys
import com.nexvora.offlineai.data.db.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PrivacyResult { HISTORY_CLEARED, ALL_CLEARED, FAILED }

@HiltViewModel
class PrivacyViewModel
@Inject
constructor(
  private val localData: LocalDataManager,
  private val preferences: PreferenceRepository,
) : ViewModel() {
  private val _stats = MutableStateFlow<LocalDataStats?>(null)
  val stats: StateFlow<LocalDataStats?> = _stats.asStateFlow()

  private val _result = MutableStateFlow<PrivacyResult?>(null)
  val result: StateFlow<PrivacyResult?> = _result.asStateFlow()

  val saveHistory: StateFlow<Boolean> =
    preferences
      .observeBoolean(PrefKeys.AUTO_SAVE_HISTORY, true)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  fun refresh() {
    viewModelScope.launch { _stats.value = runCatching { localData.stats() }.getOrNull() }
  }

  fun setSaveHistory(enabled: Boolean) {
    viewModelScope.launch { preferences.putBoolean(PrefKeys.AUTO_SAVE_HISTORY, enabled) }
  }

  fun clearHistory() = runAndRefresh(PrivacyResult.HISTORY_CLEARED) { localData.clearChatHistory() }

  fun clearAllLocalData() = runAndRefresh(PrivacyResult.ALL_CLEARED) { localData.clearAllLocalData() }

  fun dismissResult() {
    _result.value = null
  }

  private fun runAndRefresh(success: PrivacyResult, block: suspend () -> Unit) {
    viewModelScope.launch {
      _result.value = if (runCatching { block() }.isSuccess) success else PrivacyResult.FAILED
      _stats.value = runCatching { localData.stats() }.getOrNull()
    }
  }
}
