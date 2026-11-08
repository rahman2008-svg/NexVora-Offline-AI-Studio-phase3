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

import android.os.StatFs
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nexvora.offlineai.R
import com.nexvora.offlineai.common.getModelStorageDir
import com.nexvora.offlineai.data.LocalStorage
import com.nexvora.offlineai.data.ModelDownloadStatusType
import com.nexvora.offlineai.data.db.ModelSnapshot
import com.nexvora.offlineai.ui.modelmanager.ModelManagerViewModel
import com.nexvora.offlineai.ui.nexvora.EmptyState
import com.nexvora.offlineai.ui.nexvora.KeyValueRow
import com.nexvora.offlineai.ui.nexvora.SectionCard
import com.nexvora.offlineai.ui.nexvora.formatDateTime
import com.nexvora.offlineai.ui.nexvora.tools.ProductivityTool

/** NexVora home: new chat, recent chats, installed models, AI tools, storage and privacy status. */
@Composable
fun DashboardScreen(
  modelManagerViewModel: ModelManagerViewModel,
  onNewChat: () -> Unit,
  onOpenHistory: () -> Unit,
  onOpenConversation: (String) -> Unit,
  onOpenTool: (ProductivityTool?) -> Unit,
  onOpenAllTasks: () -> Unit,
  onOpenModelHub: () -> Unit,
  onOpenPrivacy: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenAbout: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: DashboardViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val uiState by modelManagerViewModel.uiState.collectAsState()
  val recentChats by viewModel.recentChats.collectAsState()
  val stats by viewModel.stats.collectAsState()
  val saveHistory by viewModel.saveHistoryEnabled.collectAsState()

  val installedModels =
    remember(uiState.modelDownloadStatus, uiState.tasks) {
      modelManagerViewModel.getAllDownloadedModels()
    }

  // Remember model facts locally (installed state, size) whenever the download state changes.
  LaunchedEffect(uiState.modelDownloadStatus, uiState.tasks) {
    val snapshots =
      modelManagerViewModel.getAllModels().map { m ->
        ModelSnapshot(
          name = m.name,
          displayName = m.displayName.ifEmpty { m.name },
          sizeBytes = m.downloadInfo.totalBytes,
          runtime = m.backendSpec.runtimeType.name,
          minDeviceMemoryGb = m.minDeviceMemoryInGb,
          isInstalled =
            uiState.modelDownloadStatus[m.name]?.status == ModelDownloadStatusType.SUCCEEDED,
        )
      }
    if (snapshots.isNotEmpty()) viewModel.syncModels(snapshots)
  }
  LaunchedEffect(installedModels.size, recentChats.size) { viewModel.refreshStats() }

  val freeBytes =
    remember(stats) {
      runCatching { StatFs(getModelStorageDir(context).path).availableBytes }.getOrDefault(0L)
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painterResource(R.drawable.logo),
          contentDescription = null,
          tint = Color.Unspecified,
          modifier = Modifier.size(56.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
          Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
          )
          Text(
            stringResource(R.string.nx_dash_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(onClick = onOpenSettings) {
          Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.nx_quick_settings))
        }
      }
    }

    item {
      Button(onClick = onNewChat, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.nx_quick_new_chat), modifier = Modifier.padding(start = 8.dp))
      }
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        QuickAction(Icons.Rounded.History, stringResource(R.string.nx_quick_history), onOpenHistory, Modifier.weight(1f))
        QuickAction(Icons.Rounded.Memory, stringResource(R.string.nx_quick_model_hub), onOpenModelHub, Modifier.weight(1f))
        QuickAction(Icons.Rounded.Lock, stringResource(R.string.nx_quick_privacy), onOpenPrivacy, Modifier.weight(1f))
      }
    }

    item {
      SectionCard(stringResource(R.string.nx_recent_chats)) {
        if (recentChats.isEmpty()) {
          EmptyState(stringResource(R.string.nx_recent_empty))
        } else {
          recentChats.forEach { chat ->
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable(role = Role.Button) { onOpenConversation(chat.id) }
                  .padding(vertical = 4.dp)
            ) {
              Text(
                chat.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                formatDateTime(chat.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.nx_view_all)) }
        }
      }
    }

    item {
      SectionCard(stringResource(R.string.nx_installed_models)) {
        if (installedModels.isEmpty()) {
          EmptyState(stringResource(R.string.nx_installed_empty))
          OutlinedButton(onClick = onOpenModelHub, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nx_open_model_hub))
          }
        } else {
          installedModels.forEach { model ->
            KeyValueRow(
              label = model.displayName.ifEmpty { model.name },
              value = LocalStorage.formatBytes(model.downloadInfo.totalBytes),
            )
          }
          TextButton(onClick = onOpenModelHub) { Text(stringResource(R.string.nx_open_model_hub)) }
        }
      }
    }

    item {
      SectionCard(stringResource(R.string.nx_ai_tools)) {
        ToolEntry(Icons.Rounded.AutoAwesome, R.string.nx_tool_writing) { onOpenTool(ProductivityTool.WRITING) }
        ToolEntry(Icons.Rounded.Summarize, R.string.nx_tool_summarize) { onOpenTool(ProductivityTool.SUMMARIZE) }
        ToolEntry(Icons.Rounded.Translate, R.string.nx_tool_translate) { onOpenTool(ProductivityTool.TRANSLATE) }
        ToolEntry(Icons.Rounded.School, R.string.nx_tool_study) { onOpenTool(ProductivityTool.STUDY) }
        ToolEntry(Icons.Rounded.EditNote, R.string.nx_tool_rewrite) { onOpenTool(ProductivityTool.REWRITE) }
        ToolEntry(Icons.Rounded.EditNote, R.string.nx_tool_notes) { onOpenTool(ProductivityTool.NOTES) }
        ToolEntry(Icons.Rounded.Apps, R.string.nx_all_ai_tasks, onOpenAllTasks)
      }
    }

    item {
      SectionCard(stringResource(R.string.nx_storage_overview)) {
        val s = stats
        if (s == null) {
          Text(stringResource(R.string.nx_loading), style = MaterialTheme.typography.bodyMedium)
        } else {
          KeyValueRow(stringResource(R.string.nx_storage_models), LocalStorage.formatBytes(s.modelsBytes))
          KeyValueRow(stringResource(R.string.nx_storage_chats), LocalStorage.formatBytes(s.databaseBytes))
          KeyValueRow(stringResource(R.string.nx_storage_cache), LocalStorage.formatBytes(s.cacheBytes))
          KeyValueRow(stringResource(R.string.nx_storage_free), LocalStorage.formatBytes(freeBytes))
        }
      }
    }

    item {
      SectionCard(stringResource(R.string.nx_privacy_status)) {
        Text(stringResource(R.string.nx_privacy_status_body), style = MaterialTheme.typography.bodyMedium)
        KeyValueRow(
          stringResource(R.string.nx_save_history),
          stringResource(if (saveHistory) R.string.nx_on else R.string.nx_off),
        )
        TextButton(onClick = onOpenPrivacy) { Text(stringResource(R.string.nx_open_privacy_center)) }
      }
    }

    item {
      TextButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.about_open_menu_item))
      }
    }
  }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier.clickable(role = Role.Button, onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
      Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
private fun ToolEntry(icon: ImageVector, labelRes: Int, onClick: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
    Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
  }
}
