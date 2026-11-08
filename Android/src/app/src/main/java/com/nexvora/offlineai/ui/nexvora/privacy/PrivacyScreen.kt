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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nexvora.offlineai.R
import com.nexvora.offlineai.data.LocalStorage
import com.nexvora.offlineai.ui.nexvora.KeyValueRow
import com.nexvora.offlineai.ui.nexvora.NexvoraScaffold
import com.nexvora.offlineai.ui.nexvora.SectionCard

private data class PermissionRow(val labelRes: Int, val granted: Boolean)

/** Permissions this app declares that people care about, with their current state. */
private fun loadPermissions(context: Context): List<PermissionRow> {
  val info =
    try {
      if (Build.VERSION.SDK_INT >= 33) {
        context.packageManager.getPackageInfo(
          context.packageName,
          PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
        )
      } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
      }
    } catch (e: Exception) {
      return emptyList()
    }
  val names = info.requestedPermissions ?: return emptyList()
  val flags = info.requestedPermissionsFlags
  val rows = LinkedHashMap<Int, Boolean>()
  names.forEachIndexed { i, name ->
    val label =
      when (name) {
        "android.permission.INTERNET" -> R.string.nx_perm_internet
        "android.permission.ACCESS_NETWORK_STATE" -> R.string.nx_perm_network_state
        "android.permission.CAMERA" -> R.string.nx_perm_camera
        "android.permission.RECORD_AUDIO" -> R.string.nx_perm_microphone
        "android.permission.POST_NOTIFICATIONS" -> R.string.nx_perm_notifications
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO" -> R.string.nx_perm_media
        "android.permission.READ_CALENDAR" -> R.string.nx_perm_calendar
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_DATA_SYNC" -> R.string.nx_perm_foreground
        else -> null
      }
    if (label != null) {
      val granted =
        flags != null &&
          i < flags.size &&
          (flags[i] and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
      rows[label] = (rows[label] ?: false) || granted
    }
  }
  return rows.map { PermissionRow(it.key, it.value) }
}

@Composable
fun PrivacyScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PrivacyViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val stats by viewModel.stats.collectAsState()
  val saveHistory by viewModel.saveHistory.collectAsState()
  val result by viewModel.result.collectAsState()
  var confirmHistory by remember { mutableStateOf(false) }
  var confirmAll by remember { mutableStateOf(false) }
  val permissions = remember { loadPermissions(context) }
  LaunchedEffect(Unit) { viewModel.refresh() }

  NexvoraScaffold(
    title = stringResource(R.string.nx_privacy_title),
    onBack = onBack,
    modifier = modifier,
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      SectionCard(stringResource(R.string.nx_privacy_no_tracking_title)) {
        Text(stringResource(R.string.nx_privacy_no_tracking_body), style = MaterialTheme.typography.bodyMedium)
      }

      SectionCard(stringResource(R.string.nx_privacy_local_data)) {
        val s = stats
        if (s == null) {
          Text(stringResource(R.string.nx_loading))
        } else {
          KeyValueRow(stringResource(R.string.nx_privacy_conversations), s.conversationCount.toString())
          KeyValueRow(stringResource(R.string.nx_privacy_messages), s.messageCount.toString())
          KeyValueRow(stringResource(R.string.nx_storage_chats), LocalStorage.formatBytes(s.databaseBytes))
          KeyValueRow(stringResource(R.string.nx_storage_models), LocalStorage.formatBytes(s.modelsBytes))
          KeyValueRow(stringResource(R.string.nx_storage_cache), LocalStorage.formatBytes(s.cacheBytes))
        }
      }

      SectionCard(stringResource(R.string.nx_privacy_controls)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.nx_save_history), style = MaterialTheme.typography.bodyLarge)
            Text(
              stringResource(R.string.nx_save_history_desc),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(checked = saveHistory, onCheckedChange = viewModel::setSaveHistory)
        }
        OutlinedButton(onClick = { confirmHistory = true }, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.nx_clear_history))
        }
        OutlinedButton(onClick = { confirmAll = true }, modifier = Modifier.fillMaxWidth()) {
          Text(stringResource(R.string.nx_clear_all_data))
        }
        result?.let {
          Text(
            stringResource(
              when (it) {
                PrivacyResult.HISTORY_CLEARED -> R.string.nx_result_history_cleared
                PrivacyResult.ALL_CLEARED -> R.string.nx_result_all_cleared
                PrivacyResult.FAILED -> R.string.nx_result_failed
              }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color =
              if (it == PrivacyResult.FAILED) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.primary,
          )
        }
      }

      SectionCard(stringResource(R.string.nx_privacy_permissions)) {
        Text(stringResource(R.string.nx_privacy_permissions_body), style = MaterialTheme.typography.bodySmall)
        permissions.forEach {
          KeyValueRow(
            stringResource(it.labelRes),
            stringResource(if (it.granted) R.string.nx_perm_granted else R.string.nx_perm_not_granted),
          )
        }
        OutlinedButton(
          modifier = Modifier.fillMaxWidth(),
          onClick = {
            context.startActivity(
              Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
          },
        ) {
          Text(stringResource(R.string.nx_open_system_settings))
        }
      }

      SectionCard(stringResource(R.string.nx_privacy_network)) {
        Text(stringResource(R.string.nx_privacy_network_body), style = MaterialTheme.typography.bodyMedium)
      }
    }
  }

  if (confirmHistory) {
    AlertDialog(
      onDismissRequest = { confirmHistory = false },
      title = { Text(stringResource(R.string.nx_clear_history)) },
      text = { Text(stringResource(R.string.nx_clear_history_confirm)) },
      confirmButton = {
        TextButton(onClick = { confirmHistory = false; viewModel.clearHistory() }) {
          Text(stringResource(R.string.nx_delete))
        }
      },
      dismissButton = { TextButton(onClick = { confirmHistory = false }) { Text(stringResource(R.string.nx_cancel)) } },
    )
  }
  if (confirmAll) {
    AlertDialog(
      onDismissRequest = { confirmAll = false },
      title = { Text(stringResource(R.string.nx_clear_all_data)) },
      text = { Text(stringResource(R.string.nx_clear_all_confirm)) },
      confirmButton = {
        TextButton(onClick = { confirmAll = false; viewModel.clearAllLocalData() }) {
          Text(stringResource(R.string.nx_delete))
        }
      },
      dismissButton = { TextButton(onClick = { confirmAll = false }) { Text(stringResource(R.string.nx_cancel)) } },
    )
  }
}
