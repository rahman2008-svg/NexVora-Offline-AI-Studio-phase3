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

import android.app.LocaleManager
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.nexvora.offlineai.R
import com.nexvora.offlineai.data.db.PrefKeys
import com.nexvora.offlineai.proto.Theme
import com.nexvora.offlineai.ui.modelmanager.ModelManagerViewModel
import com.nexvora.offlineai.ui.nexvora.NexvoraScaffold
import com.nexvora.offlineai.ui.nexvora.SectionCard
import com.nexvora.offlineai.ui.theme.ThemeSettings

private val THEMES = listOf(Theme.THEME_AUTO, Theme.THEME_LIGHT, Theme.THEME_DARK)
private val LANGUAGES = listOf(PrefKeys.LANGUAGE_SYSTEM, PrefKeys.LANGUAGE_ENGLISH, PrefKeys.LANGUAGE_BANGLA)

/** The per-app language needs Android 13 (API 33); below that the app follows the system language. */
private val PER_APP_LANGUAGE_SUPPORTED = Build.VERSION.SDK_INT >= 33

private fun currentLanguage(context: Context): String {
  if (Build.VERSION.SDK_INT < 33) return PrefKeys.LANGUAGE_SYSTEM
  val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
  return if (locales.isEmpty) PrefKeys.LANGUAGE_SYSTEM else locales.get(0).language
}

private fun applyLanguage(context: Context, tag: String) {
  if (Build.VERSION.SDK_INT < 33) return
  val list = if (tag == PrefKeys.LANGUAGE_SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
  context.getSystemService(LocaleManager::class.java).applicationLocales = list
}

private fun themeLabel(theme: Theme): Int =
  when (theme) {
    Theme.THEME_LIGHT -> R.string.theme_light
    Theme.THEME_DARK -> R.string.theme_dark
    else -> R.string.theme_auto
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  modelManagerViewModel: ModelManagerViewModel,
  onBack: () -> Unit,
  onOpenAbout: () -> Unit,
  onOpenPrivacy: () -> Unit,
  onOpenModelHub: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val saveHistory by viewModel.saveHistory.collectAsState()
  val defaultModel by viewModel.defaultModel.collectAsState()
  var selectedTheme by remember { mutableStateOf(modelManagerViewModel.readThemeOverride()) }
  var selectedLanguage by remember { mutableStateOf(currentLanguage(context)) }
  var modelMenu by remember { mutableStateOf(false) }
  val uiState by modelManagerViewModel.uiState.collectAsState()
  val downloaded = remember(uiState.modelDownloadStatus, uiState.tasks) { modelManagerViewModel.getAllDownloadedModels() }

  NexvoraScaffold(title = stringResource(R.string.nx_settings_title), onBack = onBack, modifier = modifier) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      SectionCard(stringResource(R.string.theme_title)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          THEMES.forEachIndexed { index, theme ->
            SegmentedButton(
              selected = theme == selectedTheme,
              onClick = {
                selectedTheme = theme
                ThemeSettings.themeOverride.value = theme
                modelManagerViewModel.saveThemeOverride(theme)
                val ui = context.applicationContext.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                ui.setApplicationNightMode(
                  when (theme) {
                    Theme.THEME_LIGHT -> UiModeManager.MODE_NIGHT_NO
                    Theme.THEME_DARK -> UiModeManager.MODE_NIGHT_YES
                    else -> UiModeManager.MODE_NIGHT_AUTO
                  }
                )
              },
              shape = SegmentedButtonDefaults.itemShape(index = index, count = THEMES.size),
              label = { Text(stringResource(themeLabel(theme)), maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
            )
          }
        }
      }

      SectionCard(stringResource(R.string.nx_settings_language)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          LANGUAGES.forEachIndexed { index, tag ->
            SegmentedButton(
              selected = tag == selectedLanguage,
              enabled = PER_APP_LANGUAGE_SUPPORTED,
              onClick = {
                selectedLanguage = tag
                viewModel.rememberLanguage(tag)
                applyLanguage(context, tag)
              },
              shape = SegmentedButtonDefaults.itemShape(index = index, count = LANGUAGES.size),
              label = {
                Text(
                  stringResource(
                    when (tag) {
                      PrefKeys.LANGUAGE_ENGLISH -> R.string.nx_language_english
                      PrefKeys.LANGUAGE_BANGLA -> R.string.nx_language_bangla
                      else -> R.string.nx_language_system
                    }
                  ),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  softWrap = false,
                )
              },
            )
          }
        }
        if (!PER_APP_LANGUAGE_SUPPORTED) {
          Text(stringResource(R.string.nx_settings_language_unsupported), style = MaterialTheme.typography.bodySmall)
        }
      }

      SectionCard(stringResource(R.string.nx_settings_preferences)) {
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
        Text(stringResource(R.string.nx_settings_default_model), style = MaterialTheme.typography.bodyLarge)
        Column {
          OutlinedButton(onClick = { modelMenu = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
              downloaded.firstOrNull { it.name == defaultModel }?.let { it.displayName.ifEmpty { it.name } }
                ?: stringResource(R.string.nx_settings_default_model_auto),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
            DropdownMenuItem(
              text = { Text(stringResource(R.string.nx_settings_default_model_auto)) },
              onClick = { viewModel.setDefaultModel(""); modelMenu = false },
            )
            downloaded.forEach { m ->
              DropdownMenuItem(
                text = { Text(m.displayName.ifEmpty { m.name }) },
                onClick = { viewModel.setDefaultModel(m.name); modelMenu = false },
              )
            }
          }
        }
        if (downloaded.isEmpty()) {
          Text(stringResource(R.string.nx_settings_no_models), style = MaterialTheme.typography.bodySmall)
        }
      }

      SectionCard(stringResource(R.string.nx_settings_more)) {
        OutlinedButton(onClick = onOpenModelHub, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.nx_quick_model_hub)) }
        OutlinedButton(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.nx_privacy_title)) }
        OutlinedButton(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.about_open_menu_item)) }
        OutlinedButton(
          onClick = {
            context.startActivity(Intent(context, OssLicensesMenuActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(stringResource(R.string.about_open_source_licenses))
        }
      }
    }
  }
}
