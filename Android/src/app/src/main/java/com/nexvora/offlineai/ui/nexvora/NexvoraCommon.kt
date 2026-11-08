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

package com.nexvora.offlineai.ui.nexvora

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nexvora.offlineai.R
import java.text.DateFormat
import java.util.Date

/** Shared top bar + content scaffold for the NexVora screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexvoraScaffold(
  title: String,
  onBack: (() -> Unit)?,
  modifier: Modifier = Modifier,
  actions: @Composable RowScope.() -> Unit = {},
  content: @Composable (PaddingValues) -> Unit,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(title) },
        navigationIcon = {
          if (onBack != null) {
            IconButton(onClick = onBack) {
              Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.about_navigate_up),
              )
            }
          }
        },
        actions = actions,
      )
    },
    content = content,
  )
}

/** A titled card that groups related content. */
@Composable
fun SectionCard(
  title: String,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      content()
    }
  }
}

@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    Text(
      value,
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.End,
      modifier = Modifier.padding(start = 12.dp),
    )
  }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
  Box(modifier = modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
    Text(
      message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

fun formatDateTime(epochMillis: Long): String =
  DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))

/**
 * Hand-off from the history screen to the chat screen: "continue this conversation".
 * The request expires after [TTL_MS] so a request that was never consumed cannot restore an old
 * conversation much later by surprise.
 */
object PendingChatRestore {
  const val TTL_MS = 120_000L

  @Volatile private var sessionId: String? = null
  @Volatile private var requestedAt: Long = 0L

  fun request(id: String, now: Long = System.currentTimeMillis()) {
    sessionId = id
    requestedAt = now
  }

  /** Returns the requested session id, or null if none or the request expired. */
  fun peek(now: Long = System.currentTimeMillis()): String? =
    if (now - requestedAt <= TTL_MS) sessionId else null

  fun clear() {
    sessionId = null
  }
}
