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

package com.google.ai.edge.gallery.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.tos.TosViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

/**
 * Local-only model action.
 *
 * The upstream component bundled download progress, OAuth, browser tabs, notification permission,
 * gated-model agreements, and AICore access into one button. Offline Gallery only exposes models
 * that are already present on the device, so the control can honestly be either “Open” or
 * “Unavailable offline.”
 */
@Composable
fun DownloadAndTryButton(
  @Suppress("UNUSED_PARAMETER") task: Task?,
  model: Model,
  enabled: Boolean,
  downloadStatus: ModelDownloadStatusType?,
  @Suppress("UNUSED_PARAMETER") downloadProgress: Float,
  @Suppress("UNUSED_PARAMETER") modelManagerViewModel: ModelManagerViewModel,
  onClicked: () -> Unit,
  modifier: Modifier = Modifier,
  @Suppress("UNUSED_PARAMETER") tosViewModel: TosViewModel = hiltViewModel(),
  modifierWhenExpanded: Modifier = Modifier,
  compact: Boolean = false,
  canShowTryIt: Boolean = true,
  downloadButtonBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
  val isAvailable =
    downloadStatus == ModelDownloadStatusType.SUCCEEDED ||
      model.localFileRelativeDirPathOverride.isNotEmpty() ||
      model.localModelFilePathOverride.isNotEmpty()

  if (isAvailable && canShowTryIt) {
    Button(
      onClick = onClicked,
      enabled = enabled,
      modifier = modifier.then(if (compact) Modifier else modifierWhenExpanded).height(42.dp),
      colors =
        ButtonDefaults.buttonColors(
          containerColor = downloadButtonBackgroundColor,
          contentColor = MaterialTheme.colorScheme.onSurface,
        ),
      contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
      Text(
        text = if (compact) "Open" else "Open model",
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 14.sp, stepSize = 1.sp),
      )
      Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
    }
  } else {
    OutlinedButton(
      onClick = {},
      enabled = false,
      modifier = modifier.then(if (compact) Modifier else modifierWhenExpanded).height(42.dp),
      contentPadding = PaddingValues(horizontal = 14.dp),
    ) {
      Icon(Icons.Outlined.FolderOpen, contentDescription = null)
      if (!compact) {
        Text("Local file required")
      }
    }
  }
}
