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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nexvora.offlineai.R
import com.nexvora.offlineai.data.db.ConversationEntity
import com.nexvora.offlineai.ui.nexvora.EmptyState
import com.nexvora.offlineai.ui.nexvora.NexvoraScaffold
import com.nexvora.offlineai.ui.nexvora.formatDateTime

/** Saved conversations: search, rename, delete, open. */
@Composable
fun HistoryScreen(
  onBack: () -> Unit,
  onOpenConversation: (String) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HistoryViewModel = hiltViewModel(),
) {
  val query by viewModel.query.collectAsState()
  val conversations by viewModel.conversations.collectAsState()
  val renameFailed by viewModel.renameFailed.collectAsState()
  var renameTarget by remember { mutableStateOf<ConversationEntity?>(null) }
  var deleteTarget by remember { mutableStateOf<ConversationEntity?>(null) }

  NexvoraScaffold(
    title = stringResource(R.string.nx_history_title),
    onBack = onBack,
    modifier = modifier,
  ) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
      OutlinedTextField(
        value = query,
        onValueChange = viewModel::setQuery,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.nx_history_search_hint)) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { viewModel.setQuery("") }) {
              Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.nx_clear_search))
            }
          }
        },
      )
      if (renameFailed) {
        Text(
          stringResource(R.string.nx_rename_failed),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
        )
      }
      if (conversations.isEmpty()) {
        EmptyState(
          stringResource(
            if (query.isBlank()) R.string.nx_history_empty else R.string.nx_history_no_results
          )
        )
      } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          items(conversations, key = { it.id }) { chat ->
            ConversationCard(
              chat = chat,
              onOpen = { onOpenConversation(chat.id) },
              onRename = { viewModel.clearRenameError(); renameTarget = chat },
              onDelete = { deleteTarget = chat },
            )
          }
        }
      }
    }
  }

  renameTarget?.let { target ->
    var text by remember(target.id) { mutableStateOf(target.title) }
    AlertDialog(
      onDismissRequest = { renameTarget = null },
      title = { Text(stringResource(R.string.nx_rename_title)) },
      text = {
        OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
      },
      confirmButton = {
        TextButton(
          enabled = text.isNotBlank(),
          onClick = {
            viewModel.rename(target.id, text)
            renameTarget = null
          },
        ) {
          Text(stringResource(R.string.nx_save))
        }
      },
      dismissButton = {
        TextButton(onClick = { renameTarget = null }) { Text(stringResource(R.string.nx_cancel)) }
      },
    )
  }

  deleteTarget?.let { target ->
    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      title = { Text(stringResource(R.string.nx_delete_chat_title)) },
      text = { Text(stringResource(R.string.nx_delete_chat_body, target.title)) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.delete(target.id)
            deleteTarget = null
          }
        ) {
          Text(stringResource(R.string.nx_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.nx_cancel)) }
      },
    )
  }
}

@Composable
private fun ConversationCard(
  chat: ConversationEntity,
  onOpen: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onOpen),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(chat.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (chat.preview.isNotBlank()) {
          Text(
            chat.preview,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          stringResource(R.string.nx_chat_meta, formatDateTime(chat.updatedAt), chat.messageCount),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary,
        )
      }
      IconButton(onClick = onRename) {
        Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.nx_rename_title))
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.nx_delete))
      }
    }
  }
}

/** Read-only transcript of one conversation, with an option to continue it in the chat screen. */
@Composable
fun ConversationDetailScreen(
  conversationId: String,
  onBack: () -> Unit,
  canContinue: (ConversationEntity) -> Boolean,
  onContinue: (ConversationEntity) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ConversationDetailViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsState()
  var confirmDelete by remember { mutableStateOf(false) }
  LaunchedEffect(conversationId) { viewModel.load(conversationId) }

  val title = (state as? ConversationDetailViewModel.UiState.Loaded)?.conversation?.title
  NexvoraScaffold(
    title = title ?: stringResource(R.string.nx_history_title),
    onBack = onBack,
    modifier = modifier,
    actions = {
      if (state is ConversationDetailViewModel.UiState.Loaded) {
        IconButton(onClick = { confirmDelete = true }) {
          Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.nx_delete))
        }
      }
    },
  ) { padding ->
    when (val s = state) {
      ConversationDetailViewModel.UiState.Loading ->
        Column(
          modifier = Modifier.fillMaxSize().padding(padding),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          CircularProgressIndicator()
        }
      ConversationDetailViewModel.UiState.NotFound ->
        EmptyState(stringResource(R.string.nx_conversation_not_found), Modifier.padding(padding))
      is ConversationDetailViewModel.UiState.Loaded ->
        Column(
          modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            val visible = s.messages.filter { it.content.isNotBlank() }
            if (visible.isEmpty()) EmptyState(stringResource(R.string.nx_conversation_empty))
            visible.forEach { m ->
              Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                  CardDefaults.cardColors(
                    containerColor =
                      if (m.role == "USER") MaterialTheme.colorScheme.primaryContainer
                      else MaterialTheme.colorScheme.surfaceContainerHigh
                  ),
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    stringResource(
                      when (m.role) {
                        "USER" -> R.string.nx_role_you
                        "MODEL" -> R.string.nx_role_model
                        else -> R.string.nx_role_system
                      }
                    ) + " · " + formatDateTime(m.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                  )
                  SelectionContainer { Text(m.content, style = MaterialTheme.typography.bodyMedium) }
                }
              }
            }
          }
          if (canContinue(s.conversation)) {
            Button(onClick = { onContinue(s.conversation) }, modifier = Modifier.fillMaxWidth()) {
              Text(stringResource(R.string.nx_continue_chat))
            }
          }
        }
    }
  }

  if (confirmDelete) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text(stringResource(R.string.nx_delete_chat_title)) },
      text = { Text(stringResource(R.string.nx_delete_chat_body, title ?: "")) },
      confirmButton = {
        TextButton(
          onClick = {
            confirmDelete = false
            viewModel.delete(conversationId, onDone = onBack)
          }
        ) {
          Text(stringResource(R.string.nx_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.nx_cancel)) }
      },
    )
  }
}
