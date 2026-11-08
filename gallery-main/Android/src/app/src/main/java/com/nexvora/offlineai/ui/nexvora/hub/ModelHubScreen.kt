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

package com.nexvora.offlineai.ui.nexvora.hub

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nexvora.offlineai.R
import com.nexvora.offlineai.common.getModelStorageDir
import com.nexvora.offlineai.data.LocalStorage
import com.nexvora.offlineai.data.Model
import com.nexvora.offlineai.data.ModelDownloadStatus
import com.nexvora.offlineai.data.ModelDownloadStatusType
import com.nexvora.offlineai.ui.modelmanager.ModelManagerViewModel
import com.nexvora.offlineai.ui.nexvora.EmptyState
import com.nexvora.offlineai.ui.nexvora.KeyValueRow
import com.nexvora.offlineai.ui.nexvora.NexvoraScaffold
import com.nexvora.offlineai.ui.nexvora.SectionCard

private fun deviceTotalMemory(context: Context): Long {
  val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
  val info = ActivityManager.MemoryInfo()
  am.getMemoryInfo(info)
  return info.totalMem
}

private fun freeStorage(context: Context): Long =
  runCatching { StatFs(getModelStorageDir(context).path).availableBytes }.getOrDefault(0L)

/**
 * Model Hub: installed and available models, real download progress (from the existing download
 * manager), cancel / retry / delete, compatibility hints and storage usage.
 */
@Composable
fun ModelHubScreen(
  modelManagerViewModel: ModelManagerViewModel,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val uiState by modelManagerViewModel.uiState.collectAsState()
  val models = remember(uiState.tasks, uiState.modelImportingUpdateTrigger) { modelManagerViewModel.getAllModels() }
  val statusOf = { m: Model -> uiState.modelDownloadStatus[m.name] }

  val installed = models.filter { statusOf(it)?.status == ModelDownloadStatusType.SUCCEEDED }
  val downloading =
    models.filter {
      statusOf(it)?.status == ModelDownloadStatusType.IN_PROGRESS ||
        statusOf(it)?.status == ModelDownloadStatusType.UNZIPPING
    }
  val available = models.filter { it !in installed && it !in downloading }

  val totalMem = remember { deviceTotalMemory(context) }
  val free = remember(uiState.modelDownloadStatus) { freeStorage(context) }
  var confirmDownload by remember { mutableStateOf<Model?>(null) }
  var confirmDelete by remember { mutableStateOf<Model?>(null) }

  NexvoraScaffold(
    title = stringResource(R.string.nx_hub_title),
    onBack = onBack,
    modifier = modifier,
  ) { padding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        SectionCard(stringResource(R.string.nx_storage_overview)) {
          KeyValueRow(
            stringResource(R.string.nx_hub_installed_size),
            LocalStorage.formatBytes(installed.sumOf { it.downloadInfo.totalBytes }),
          )
          KeyValueRow(stringResource(R.string.nx_storage_free), LocalStorage.formatBytes(free))
          KeyValueRow(
            stringResource(R.string.nx_hub_device_ram),
            stringResource(R.string.nx_hub_gb, ModelCompatibility.nominalDeviceGb(totalMem)),
          )
          Text(stringResource(R.string.nx_hub_internet_note), style = MaterialTheme.typography.bodySmall)
        }
      }

      if (uiState.loadingModelAllowlist && models.isEmpty()) {
        item { EmptyState(stringResource(R.string.nx_loading)) }
      } else if (models.isEmpty()) {
        item {
          EmptyState(
            uiState.loadingModelAllowlistError.ifEmpty { stringResource(R.string.nx_hub_no_models) }
          )
        }
      }

      if (downloading.isNotEmpty()) {
        item { Text(stringResource(R.string.nx_hub_downloading), style = MaterialTheme.typography.titleSmall) }
        items(downloading, key = { "d-" + it.name }) { m ->
          ModelCard(m, statusOf(m), totalMem, free, installed = false, onDownload = {}, onCancel = { modelManagerViewModel.cancelDownloadModel(m) }, onDelete = {})
        }
      }
      if (installed.isNotEmpty()) {
        item { Text(stringResource(R.string.nx_hub_installed), style = MaterialTheme.typography.titleSmall) }
        items(installed, key = { "i-" + it.name }) { m ->
          ModelCard(m, statusOf(m), totalMem, free, installed = true, onDownload = {}, onCancel = {}, onDelete = { confirmDelete = m })
        }
      }
      if (available.isNotEmpty()) {
        item { Text(stringResource(R.string.nx_hub_available), style = MaterialTheme.typography.titleSmall) }
        items(available, key = { "a-" + it.name }) { m ->
          ModelCard(m, statusOf(m), totalMem, free, installed = false, onDownload = { confirmDownload = m }, onCancel = {}, onDelete = {})
        }
      }
    }
  }

  confirmDownload?.let { m ->
    AlertDialog(
      onDismissRequest = { confirmDownload = null },
      title = { Text(stringResource(R.string.nx_hub_confirm_download_title)) },
      text = {
        Text(
          stringResource(
            R.string.nx_hub_confirm_download_body,
            m.displayName.ifEmpty { m.name },
            LocalStorage.formatBytes(m.downloadInfo.totalBytes),
          )
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            confirmDownload = null
            modelManagerViewModel.downloadModel(task = null, model = m)
          }
        ) {
          Text(stringResource(R.string.nx_hub_download))
        }
      },
      dismissButton = { TextButton(onClick = { confirmDownload = null }) { Text(stringResource(R.string.nx_cancel)) } },
    )
  }
  confirmDelete?.let { m ->
    AlertDialog(
      onDismissRequest = { confirmDelete = null },
      title = { Text(stringResource(R.string.nx_hub_confirm_delete_title)) },
      text = { Text(stringResource(R.string.nx_hub_confirm_delete_body, m.displayName.ifEmpty { m.name })) },
      confirmButton = {
        TextButton(
          onClick = {
            confirmDelete = null
            modelManagerViewModel.deleteModel(model = m)
          }
        ) {
          Text(stringResource(R.string.nx_delete))
        }
      },
      dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.nx_cancel)) } },
    )
  }
}

@Composable
private fun ModelCard(
  model: Model,
  status: ModelDownloadStatus?,
  totalMem: Long,
  free: Long,
  installed: Boolean,
  onDownload: () -> Unit,
  onCancel: () -> Unit,
  onDelete: () -> Unit,
) {
  val type = status?.status ?: ModelDownloadStatusType.NOT_DOWNLOADED
  val size = model.downloadInfo.totalBytes
  val compat = ModelCompatibility.evaluate(model.minDeviceMemoryInGb, totalMem, size, free, installed)
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(model.displayName.ifEmpty { model.name }, style = MaterialTheme.typography.titleSmall)
      Text(
        stringResource(R.string.nx_hub_size_runtime, LocalStorage.formatBytes(size), model.backendSpec.runtimeType.name),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        when (compat.level) {
          CompatLevel.OK -> stringResource(R.string.nx_hub_compat_ok, compat.requiredGb ?: 0, compat.deviceGb)
          CompatLevel.LOW_MEMORY -> stringResource(R.string.nx_hub_compat_low, compat.requiredGb ?: 0, compat.deviceGb)
          CompatLevel.NOT_ENOUGH_STORAGE -> stringResource(R.string.nx_hub_compat_storage)
          CompatLevel.UNKNOWN -> stringResource(R.string.nx_hub_compat_unknown)
        },
        style = MaterialTheme.typography.bodySmall,
        color =
          if (compat.level == CompatLevel.LOW_MEMORY || compat.level == CompatLevel.NOT_ENOUGH_STORAGE)
            MaterialTheme.colorScheme.error
          else MaterialTheme.colorScheme.secondary,
      )

      when (type) {
        ModelDownloadStatusType.IN_PROGRESS,
        ModelDownloadStatusType.UNZIPPING -> {
          val total = status?.totalBytes ?: 0L
          val received = status?.receivedBytes ?: 0L
          if (type == ModelDownloadStatusType.UNZIPPING || total <= 0L) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          } else {
            LinearProgressIndicator(
              progress = { (received.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth(),
            )
          }
          Text(
            if (type == ModelDownloadStatusType.UNZIPPING) stringResource(R.string.nx_hub_unzipping)
            else
              stringResource(
                R.string.nx_hub_progress,
                LocalStorage.formatBytes(received),
                LocalStorage.formatBytes(total),
                LocalStorage.formatBytes(status?.bytesPerSecond ?: 0L),
              ),
            style = MaterialTheme.typography.labelSmall,
          )
          OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nx_cancel))
          }
        }
        ModelDownloadStatusType.SUCCEEDED ->
          OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nx_delete))
          }
        ModelDownloadStatusType.UNAVAILABLE ->
          Text(stringResource(R.string.nx_hub_unavailable), style = MaterialTheme.typography.bodySmall)
        else -> {
          if (type == ModelDownloadStatusType.FAILED) {
            val message =
              if (status?.authError != null) stringResource(R.string.nx_hub_auth_required)
              else status?.errorMessage?.takeIf { it.isNotBlank() } ?: stringResource(R.string.nx_hub_failed)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
          }
          Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
            Text(
              stringResource(
                when (type) {
                  ModelDownloadStatusType.FAILED -> R.string.nx_hub_retry
                  ModelDownloadStatusType.PARTIALLY_DOWNLOADED -> R.string.nx_hub_resume
                  else -> R.string.nx_hub_download
                }
              )
            )
          }
        }
      }
    }
  }
}
