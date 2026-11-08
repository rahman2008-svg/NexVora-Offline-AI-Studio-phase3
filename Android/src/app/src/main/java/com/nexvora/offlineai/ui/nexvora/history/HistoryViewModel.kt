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

package com.nexvora.offlineai.ui.nexvora.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexvora.offlineai.data.db.ConversationEntity
import com.nexvora.offlineai.data.db.MessageEntity
import com.nexvora.offlineai.data.db.RoomChatSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Search, rename and delete saved conversations. */
@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(private val chats: RoomChatSessionRepository) : ViewModel() {
  private val _query = MutableStateFlow("")
  val query: StateFlow<String> = _query.asStateFlow()

  val conversations: StateFlow<List<ConversationEntity>> =
    _query
      .debounce(200)
      .flatMapLatest { chats.observeSearch(it) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _renameFailed = MutableStateFlow(false)
  val renameFailed: StateFlow<Boolean> = _renameFailed.asStateFlow()

  fun setQuery(value: String) {
    _query.value = value
  }

  fun rename(id: String, title: String) {
    viewModelScope.launch { _renameFailed.value = !chats.renameChatSession(id, title) }
  }

  fun clearRenameError() {
    _renameFailed.value = false
  }

  fun delete(id: String) {
    viewModelScope.launch { chats.deleteChatSession(id) }
  }
}

/** Loads one conversation and its messages for the read-only transcript screen. */
@HiltViewModel
class ConversationDetailViewModel @Inject constructor(private val chats: RoomChatSessionRepository) :
  ViewModel() {
  sealed interface UiState {
    data object Loading : UiState

    data object NotFound : UiState

    data class Loaded(val conversation: ConversationEntity, val messages: List<MessageEntity>) : UiState
  }

  private val _state = MutableStateFlow<UiState>(UiState.Loading)
  val state: StateFlow<UiState> = _state.asStateFlow()

  fun load(id: String) {
    viewModelScope.launch {
      val conversation = chats.getConversation(id)
      _state.value =
        if (conversation == null) UiState.NotFound
        else UiState.Loaded(conversation, chats.getMessages(id))
    }
  }

  fun delete(id: String, onDone: () -> Unit) {
    viewModelScope.launch {
      chats.deleteChatSession(id)
      onDone()
    }
  }
}
