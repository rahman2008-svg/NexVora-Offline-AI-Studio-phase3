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

package com.nexvora.offlineai.ui.nexvora.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvora.offlineai.data.db.PrefKeys
import com.nexvora.offlineai.data.db.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(private val preferences: PreferenceRepository) : ViewModel() {
  val saveHistory: StateFlow<Boolean> =
    preferences
      .observeBoolean(PrefKeys.AUTO_SAVE_HISTORY, true)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

  /** `Model.name` of the preferred model for AI tools, or empty for "first installed". */
  val defaultModel: StateFlow<String> =
    preferences
      .observeString(PrefKeys.DEFAULT_MODEL, "")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

  fun setSaveHistory(enabled: Boolean) {
    viewModelScope.launch { preferences.putBoolean(PrefKeys.AUTO_SAVE_HISTORY, enabled) }
  }

  fun setDefaultModel(name: String) {
    viewModelScope.launch { preferences.putString(PrefKeys.DEFAULT_MODEL, name) }
  }

  fun rememberLanguage(tag: String) {
    viewModelScope.launch { preferences.putString(PrefKeys.LANGUAGE, tag) }
  }
}
