/*
 * Copyright 2026 Google LLC
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

package com.nexvora.offlineai.customtasks.agentchat

import com.nexvora.offlineai.agent.AgentChatExecutor
import com.nexvora.offlineai.agent.AgentRuntimeExecutor
import com.nexvora.offlineai.agent.sessions.LlmSessionManager
import com.nexvora.offlineai.data.DataStoreRepository
import com.nexvora.offlineai.data.SystemPromptRepository
import com.nexvora.offlineai.ui.llmchat.LlmChatViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AgentChatViewModel
@Inject
constructor(
  systemPromptRepository: SystemPromptRepository,
  @AgentChatExecutor runtimeExecutor: AgentRuntimeExecutor,
  llmSessionManager: LlmSessionManager,
  dataStoreRepository: DataStoreRepository? = null,
) :
LlmChatViewModel(systemPromptRepository, runtimeExecutor,
llmSessionManager, dataStoreRepository)
