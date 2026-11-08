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

package com.nexvora.offlineai.ui.nexvora.tools

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nexvora.offlineai.R
import com.nexvora.offlineai.data.Model
import com.nexvora.offlineai.ui.modelmanager.ModelManagerViewModel
import com.nexvora.offlineai.ui.nexvora.EmptyState
import com.nexvora.offlineai.ui.nexvora.NexvoraScaffold
import com.nexvora.offlineai.ui.nexvora.SectionCard
import kotlinx.coroutines.launch

private fun toolTitle(tool: ProductivityTool): Int =
  when (tool) {
    ProductivityTool.WRITING -> R.string.nx_tool_writing
    ProductivityTool.SUMMARIZE -> R.string.nx_tool_summarize
    ProductivityTool.TRANSLATE -> R.string.nx_tool_translate
    ProductivityTool.STUDY -> R.string.nx_tool_study
    ProductivityTool.REWRITE -> R.string.nx_tool_rewrite
    ProductivityTool.NOTES -> R.string.nx_tool_notes
  }

private fun toolHint(tool: ProductivityTool): Int =
  when (tool) {
    ProductivityTool.WRITING -> R.string.nx_tool_writing_hint
    ProductivityTool.SUMMARIZE -> R.string.nx_tool_summarize_hint
    ProductivityTool.TRANSLATE -> R.string.nx_tool_translate_hint
    ProductivityTool.STUDY -> R.string.nx_tool_study_hint
    ProductivityTool.REWRITE -> R.string.nx_tool_rewrite_hint
    ProductivityTool.NOTES -> R.string.nx_tool_notes_hint
  }

/** AI Writing, Summarization, Translation, Study Assistant, Rewriting and Notes on the local LLM. */
@Composable
fun ToolsScreen(
  modelManagerViewModel: ModelManagerViewModel,
  initialTool: ProductivityTool?,
  onBack: () -> Unit,
  onOpenModelHub: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ProductivityViewModel = hiltViewModel(),
) {
  val context = LocalContext.current
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  val clipboard = LocalClipboard.current
  val ui by viewModel.state.collectAsState()
  val defaultModelName by viewModel.defaultModelName.collectAsState()
  val savedLanguage by viewModel.savedTargetLanguage.collectAsState()
  val mmState by modelManagerViewModel.uiState.collectAsState()
  val models = remember(mmState.modelDownloadStatus, mmState.tasks) { modelManagerViewModel.getAllDownloadedModels() }

  var tool by remember { mutableStateOf(initialTool ?: ProductivityTool.WRITING) }
  var input by remember { mutableStateOf("") }
  var options by remember { mutableStateOf(ToolOptions()) }
  var pickedModelName by remember { mutableStateOf<String?>(null) }
  var modelMenu by remember { mutableStateOf(false) }
  var optionMenu by remember { mutableStateOf(false) }

  LaunchedEffect(savedLanguage) { options = options.copy(targetLanguage = savedLanguage) }

  val model: Model? =
    models.firstOrNull { it.name == pickedModelName }
      ?: models.firstOrNull { it.name == defaultModelName }
      ?: models.firstOrNull()

  DisposableEffect(Unit) { onDispose { viewModel.release(context, modelManagerViewModel) } }

  NexvoraScaffold(title = stringResource(R.string.nx_tools_title), onBack = onBack, modifier = modifier) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Tool picker.
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ProductivityTool.entries.chunked(3).forEach { row ->
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { t ->
              FilterChip(
                selected = t == tool,
                onClick = { if (!ui.running && !ui.preparing) { tool = t; viewModel.clearOutput() } },
                label = { Text(stringResource(toolTitle(t)), maxLines = 1, overflow = TextOverflow.Ellipsis) },
              )
            }
          }
        }
      }

      if (models.isEmpty()) {
        SectionCard(stringResource(R.string.nx_tools_no_model_title)) {
          EmptyState(stringResource(R.string.nx_tools_no_model_body))
          Button(onClick = onOpenModelHub, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.nx_open_model_hub))
          }
        }
        return@Column
      }

      // Model picker.
      Column {
        Text(stringResource(R.string.nx_tools_model), style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { modelMenu = true }, modifier = Modifier.fillMaxWidth()) {
          Text(model?.let { it.displayName.ifEmpty { it.name } } ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
          models.forEach { m ->
            DropdownMenuItem(
              text = { Text(m.displayName.ifEmpty { m.name }) },
              onClick = { pickedModelName = m.name; modelMenu = false },
            )
          }
        }
      }

      // Tool-specific options.
      when (tool) {
        ProductivityTool.TRANSLATE ->
          OptionPicker(
            label = stringResource(R.string.nx_tools_target_language),
            current = options.targetLanguage,
            items = ProductivityPrompts.TARGET_LANGUAGES.map { it to it },
            expanded = optionMenu,
            onExpand = { optionMenu = it },
            onPick = {
              options = options.copy(targetLanguage = it)
              viewModel.rememberTargetLanguage(it)
            },
          )
        ProductivityTool.WRITING ->
          OptionPicker(
            label = stringResource(R.string.nx_tools_kind),
            current = options.writingKind.name,
            items = WritingKind.entries.map { it.name to stringResource(writingKindLabel(it)) },
            expanded = optionMenu,
            onExpand = { optionMenu = it },
            onPick = { options = options.copy(writingKind = WritingKind.valueOf(it)) },
          )
        ProductivityTool.REWRITE ->
          OptionPicker(
            label = stringResource(R.string.nx_tools_tone),
            current = options.tone.name,
            items = RewriteTone.entries.map { it.name to stringResource(toneLabel(it)) },
            expanded = optionMenu,
            onExpand = { optionMenu = it },
            onPick = { options = options.copy(tone = RewriteTone.valueOf(it)) },
          )
        ProductivityTool.STUDY ->
          OptionPicker(
            label = stringResource(R.string.nx_tools_mode),
            current = options.studyMode.name,
            items = StudyMode.entries.map { it.name to stringResource(studyModeLabel(it)) },
            expanded = optionMenu,
            onExpand = { optionMenu = it },
            onPick = { options = options.copy(studyMode = StudyMode.valueOf(it)) },
          )
        ProductivityTool.NOTES ->
          OptionPicker(
            label = stringResource(R.string.nx_tools_mode),
            current = options.notesMode.name,
            items = NotesMode.entries.map { it.name to stringResource(notesModeLabel(it)) },
            expanded = optionMenu,
            onExpand = { optionMenu = it },
            onPick = { options = options.copy(notesMode = NotesMode.valueOf(it)) },
          )
        ProductivityTool.SUMMARIZE -> {}
      }

      OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
        label = { Text(stringResource(toolHint(tool))) },
        supportingText = {
          Text(stringResource(R.string.nx_tools_char_count, input.length, ProductivityPrompts.MAX_INPUT_CHARS))
        },
      )

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (ui.running || ui.preparing) {
          OutlinedButton(onClick = { model?.let { viewModel.stop(it) } }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.nx_tools_stop))
          }
        } else {
          Button(
            enabled = input.isNotBlank() && model != null,
            onClick = { model?.let { viewModel.run(context, modelManagerViewModel, it, tool, input, options) } },
            modifier = Modifier.weight(1f),
          ) {
            Text(stringResource(R.string.nx_tools_run))
          }
        }
      }

      if (ui.preparing) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.nx_tools_loading_model), style = MaterialTheme.typography.bodySmall)
      }
      ui.error?.let {
        Text(
          stringResource(
            when (it) {
              ToolError.NO_TASK -> R.string.nx_tools_error_no_task
              ToolError.MODEL_INIT_FAILED -> R.string.nx_tools_error_init
              ToolError.INFERENCE_FAILED -> R.string.nx_tools_error_inference
            }
          ) + if (ui.errorDetail.isNotBlank()) "\n" + ui.errorDetail else "",
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      if (ui.output.isNotEmpty()) {
        SectionCard(stringResource(R.string.nx_tools_result)) {
          SelectionContainer { Text(ui.output, style = MaterialTheme.typography.bodyMedium) }
          if (!ui.running) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("NexVora", ui.output))) } }
              ) {
                Text(stringResource(R.string.nx_tools_copy))
              }
              OutlinedButton(
                onClick = {
                  val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ui.output)
                  context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
              ) {
                Text(stringResource(R.string.nx_tools_share))
              }
            }
            if (ui.savedToHistory) {
              Text(stringResource(R.string.nx_tools_saved), style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OptionPicker(
  label: String,
  current: String,
  items: List<Pair<String, String>>,
  expanded: Boolean,
  onExpand: (Boolean) -> Unit,
  onPick: (String) -> Unit,
) {
  Column {
    Text(label, style = MaterialTheme.typography.labelMedium)
    OutlinedButton(onClick = { onExpand(true) }, modifier = Modifier.fillMaxWidth()) {
      Text(items.firstOrNull { it.first == current }?.second ?: current)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { onExpand(false) }) {
      items.forEach { (key, text) ->
        DropdownMenuItem(text = { Text(text) }, onClick = { onPick(key); onExpand(false) })
      }
    }
  }
}

private fun writingKindLabel(k: WritingKind): Int =
  when (k) {
    WritingKind.FREE -> R.string.nx_kind_free
    WritingKind.EMAIL -> R.string.nx_kind_email
    WritingKind.BLOG -> R.string.nx_kind_blog
    WritingKind.SOCIAL -> R.string.nx_kind_social
  }

private fun toneLabel(t: RewriteTone): Int =
  when (t) {
    RewriteTone.PROFESSIONAL -> R.string.nx_tone_professional
    RewriteTone.FRIENDLY -> R.string.nx_tone_friendly
    RewriteTone.CASUAL -> R.string.nx_tone_casual
    RewriteTone.FORMAL -> R.string.nx_tone_formal
    RewriteTone.CONCISE -> R.string.nx_tone_concise
  }

private fun studyModeLabel(m: StudyMode): Int =
  when (m) {
    StudyMode.EXPLAIN -> R.string.nx_study_explain
    StudyMode.QUIZ -> R.string.nx_study_quiz
    StudyMode.FLASHCARDS -> R.string.nx_study_flashcards
  }

private fun notesModeLabel(m: NotesMode): Int =
  when (m) {
    NotesMode.CLEAN_UP -> R.string.nx_notes_cleanup
    NotesMode.ACTION_ITEMS -> R.string.nx_notes_actions
    NotesMode.OUTLINE -> R.string.nx_notes_outline
  }
