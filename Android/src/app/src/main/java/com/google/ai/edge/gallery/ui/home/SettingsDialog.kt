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

package com.google.ai.edge.gallery.ui.home

import android.app.UiModeManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.ai.edge.gallery.BuildConfig
import com.google.ai.edge.gallery.proto.Theme
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import com.google.ai.edge.gallery.ui.theme.ThemeSettings

private val THEME_OPTIONS = listOf(Theme.THEME_AUTO, Theme.THEME_LIGHT, Theme.THEME_DARK)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
  curThemeOverride: Theme,
  modelManagerViewModel: ModelManagerViewModel,
  onDismissed: () -> Unit,
) {
  var selectedTheme by remember { mutableStateOf(curThemeOverride) }
  val context = LocalContext.current

  Dialog(onDismissRequest = onDismissed) {
    Card(
      modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
      shape = RoundedCornerShape(24.dp),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("Settings", style = MaterialTheme.typography.headlineSmall)
          Text(
            "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f, fill = false),
          verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Appearance", fontWeight = FontWeight.SemiBold)
            MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              THEME_OPTIONS.forEachIndexed { index, theme ->
                SegmentedButton(
                  shape =
                    SegmentedButtonDefaults.itemShape(index = index, count = THEME_OPTIONS.size),
                  onCheckedChange = {
                    selectedTheme = theme
                    ThemeSettings.themeOverride.value = theme
                    modelManagerViewModel.saveThemeOverride(theme)
                    setSystemTheme(context = context, theme = theme)
                  },
                  checked = theme == selectedTheme,
                  label = { Text(themeLabel(theme)) },
                )
              }
            }
          }

          HorizontalDivider()

          Card(
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(18.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(Icons.Outlined.Lock, contentDescription = null)
                Text("Offline safeguards", fontWeight = FontWeight.SemiBold)
              }
              OfflineSettingRow("No internet or network-state permission")
              OfflineSettingRow("No analytics, crash reporting, or push messaging")
              OfflineSettingRow("No cloud backup of app data")
              OfflineSettingRow("Models are imported from local storage")
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("About", fontWeight = FontWeight.SemiBold)
            Text(
              "Offline AI Gallery is an independent, offline-focused fork of Google AI Edge " +
                "Gallery. It uses the on-device LiteRT-LM runtime and remains licensed under " +
                "Apache License 2.0.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              "No Google account, Play Services, or Google terms acceptance is required.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          Button(onClick = onDismissed) { Text("Done") }
        }
      }
    }
  }
}

@Composable
private fun OfflineSettingRow(text: String) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      Icons.Outlined.CheckCircle,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(text, style = MaterialTheme.typography.bodySmall)
  }
}

private fun setSystemTheme(context: Context, theme: Theme) {
  val uiModeManager =
    context.applicationContext.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
  uiModeManager.setApplicationNightMode(
    when (theme) {
      Theme.THEME_LIGHT -> UiModeManager.MODE_NIGHT_NO
      Theme.THEME_DARK -> UiModeManager.MODE_NIGHT_YES
      else -> UiModeManager.MODE_NIGHT_AUTO
    }
  )
}

private fun themeLabel(theme: Theme): String {
  return when (theme) {
    Theme.THEME_AUTO -> "System"
    Theme.THEME_LIGHT -> "Light"
    Theme.THEME_DARK -> "Dark"
    else -> "System"
  }
}
