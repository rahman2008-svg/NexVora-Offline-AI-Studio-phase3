/*
 * Copyright 2025 Google LLC
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

package com.nexvora.offlineai.ui.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.nexvora.offlineai.BuildConfig
import com.nexvora.offlineai.R

private const val DEVELOPER = "Prince AR Abdur Rahman"
private const val PUBLISHER = "NexVora Lab's Ofc"
private const val EMAIL = "prince.ar.abdur.rahman2008@gmail.com"
private const val WHATSAPP_1 = "01707424006"
private const val WHATSAPP_2 = "01796951709"
private const val FACEBOOK = "https://www.facebook.com/share/1BNn32qoJo/"
private const val INSTAGRAM = "https://www.instagram.com/ur___abdur____rahman__2008"

/** Local Bangladesh numbers (01XXXXXXXXX) -> international format for wa.me links. */
private fun whatsappUrl(localNumber: String): String = "https://wa.me/880" + localNumber.removePrefix("0")

private fun openLink(context: Context, intent: Intent) {
  try {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  } catch (_: Exception) {
    // No app can handle this link; ignore instead of crashing.
  }
}

private fun openUrl(context: Context, url: String) =
  openLink(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

/** NexVora developer / app information screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navigateUp: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.about_title)) },
        navigationIcon = {
          IconButton(onClick = navigateUp) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(R.string.about_navigate_up),
            )
          }
        },
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Icon(
        painterResource(R.drawable.logo),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = Modifier.size(96.dp),
      )
      Text(
        stringResource(R.string.app_name),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        textAlign = TextAlign.Center,
      )
      Text(
        stringResource(R.string.about_tagline),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Text(
        stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )

      InfoCard {
        LabeledValue(stringResource(R.string.about_developed_by), DEVELOPER)
        LabeledValue(stringResource(R.string.about_published_by), PUBLISHER)
      }

      InfoCard {
        Text(
          stringResource(R.string.about_contact),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.secondary,
        )
        ContactRow(stringResource(R.string.about_whatsapp), WHATSAPP_1) {
          openUrl(context, whatsappUrl(WHATSAPP_1))
        }
        ContactRow(stringResource(R.string.about_whatsapp), WHATSAPP_2) {
          openUrl(context, whatsappUrl(WHATSAPP_2))
        }
        ContactRow(stringResource(R.string.about_email), EMAIL) {
          openLink(context, Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$EMAIL")))
        }
        ContactRow(stringResource(R.string.about_facebook), FACEBOOK) { openUrl(context, FACEBOOK) }
        ContactRow(stringResource(R.string.about_instagram), INSTAGRAM) {
          openUrl(context, INSTAGRAM)
        }
      }

      InfoCard {
        Text(
          stringResource(R.string.about_credits),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.secondary,
        )
        Text(stringResource(R.string.about_credits_body), style = MaterialTheme.typography.bodyMedium)
        Text(
          stringResource(R.string.about_upstream),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      OutlinedButton(
        onClick = {
          openLink(context, Intent(context, OssLicensesMenuActivity::class.java))
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.about_open_source_licenses))
      }

      Text(
        stringResource(R.string.about_copyright),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(12.dp))
    }
  }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      content()
    }
  }
}

@Composable
private fun LabeledValue(label: String, value: String) {
  Column {
    Text(
      label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(value, style = MaterialTheme.typography.titleMedium)
  }
}

@Composable
private fun ContactRow(label: String, value: String, onClick: () -> Unit) {
  androidx.compose.material3.TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(label, style = MaterialTheme.typography.labelLarge)
      Text(
        value,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(1f).padding(start = 12.dp),
        maxLines = 2,
      )
    }
  }
}
