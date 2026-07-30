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

package com.google.ai.edge.gallery.customtasks.localimagegenerator

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.ai.edge.gallery.customtasks.common.CustomTaskData
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LocalImageGeneratorScreen(
  data: CustomTaskData,
  viewModel: LocalImageGeneratorViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val busy = uiState.runtimeState.isBusy
  var showAdvanced by rememberSaveable { mutableStateOf(false) }
  val initialPrompt = data.initialDraft ?: data.initialQuery
  val pickModelLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      uri?.let(viewModel::selectModel)
    }
  val saveImageLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
      uri?.let(viewModel::exportLatest)
    }

  LaunchedEffect(Unit) {
    data.setTopBarVisible(true)
    viewModel.prefillPrompt(initialPrompt)
    viewModel.loadSelectedModel()
  }
  LaunchedEffect(busy) {
    data.setAppBarControlsDisabled(busy)
    data.setCustomNavigateUpCallback(
      if (busy) {
        {
          viewModel.explainBusyNavigation()
        }
      } else {
        null
      }
    )
  }
  DisposableEffect(Unit) {
    onDispose {
      data.setAppBarControlsDisabled(false)
      data.setCustomNavigateUpCallback(null)
    }
  }

  Column(
    modifier =
      Modifier.fillMaxSize()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = data.bottomPadding + 20.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
      text = "Create an image without a server or network connection.",
      style = MaterialTheme.typography.bodyLarge,
    )
    Text(
      text =
        "Choose a compatible single-file Stable Diffusion checkpoint. Gallery reads it in place " +
          "and does not copy it into the app.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    ModelCard(
      uiState = uiState,
      onChooseModel = { pickModelLauncher.launch(arrayOf("*/*")) },
      onLoadModel = viewModel::loadSelectedModel,
    )

    if (busy) {
      Card(
        colors =
          CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          )
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text =
              if (uiState.runtimeState == LocalImageRuntimeState.LOADING_MODEL) {
                "Loading model"
              } else {
                "Generating on device"
              },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          Text(
            text = uiState.notice,
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }
    } else if (uiState.notice.isNotBlank()) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
      ) {
        Text(
          text = uiState.notice,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
      }
    }

    if (uiState.errorMessage.isNotBlank()) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
      ) {
        Text(
          text = uiState.errorMessage,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
      }
    }

    OutlinedTextField(
      value = uiState.prompt,
      onValueChange = viewModel::setPrompt,
      enabled = !busy,
      label = { Text("Prompt") },
      placeholder = { Text("A cinematic still of…") },
      minLines = 3,
      maxLines = 7,
      modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
      value = uiState.negativePrompt,
      onValueChange = viewModel::setNegativePrompt,
      enabled = !busy,
      label = { Text("Negative prompt (optional)") },
      placeholder = { Text("blurry, distorted, low detail") },
      minLines = 2,
      maxLines = 4,
      modifier = Modifier.fillMaxWidth(),
    )

    TextButton(enabled = !busy, onClick = { showAdvanced = !showAdvanced }) {
      Text(if (showAdvanced) "Hide generation controls" else "Generation controls")
    }
    if (showAdvanced) {
      GenerationControls(
        uiState = uiState,
        enabled = !busy,
        onCanvasChanged = viewModel::setCanvas,
        onStepsChanged = viewModel::setSteps,
        onCfgScaleChanged = viewModel::setCfgScale,
        onSeedChanged = viewModel::setSeedText,
      )
    }

    Button(
      onClick = viewModel::generate,
      enabled =
        uiState.runtimeState == LocalImageRuntimeState.READY && uiState.prompt.isNotBlank(),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Icon(Icons.Outlined.ImageIcon, contentDescription = null)
      Text("Generate locally", modifier = Modifier.padding(start = 8.dp))
    }

    GeneratedImageCard(
      imagePath = uiState.generatedImagePath,
      width = uiState.generatedImageWidth ?: uiState.canvas.width,
      height = uiState.generatedImageHeight ?: uiState.canvas.height,
      elapsedMs = uiState.generationElapsedMs,
      onSave = { imagePath ->
        val suggestedName = File(imagePath).name
        saveImageLauncher.launch(suggestedName)
      },
    )
  }
}

@Composable
private fun ModelCard(
  uiState: LocalImageGeneratorUiState,
  onChooseModel: () -> Unit,
  onLoadModel: () -> Unit,
) {
  Card {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        text = "Local checkpoint",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      if (uiState.modelUri == null) {
        Text(
          text = "No image model selected",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        Text(text = uiState.modelName, style = MaterialTheme.typography.bodyLarge)
        if (uiState.modelSizeBytes >= 0) {
          Text(
            text = formatBytes(uiState.modelSizeBytes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Text(
          text =
            when (uiState.runtimeState) {
              LocalImageRuntimeState.READY -> "Loaded and ready"
              LocalImageRuntimeState.LOADING_MODEL -> "Loading…"
              LocalImageRuntimeState.GENERATING -> "In use"
              else -> "Selected, not loaded"
            },
          style = MaterialTheme.typography.labelMedium,
          color =
            if (uiState.runtimeState == LocalImageRuntimeState.READY) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = !uiState.runtimeState.isBusy, onClick = onChooseModel) {
          Icon(Icons.Outlined.FolderOpen, contentDescription = null)
          Text(
            text = if (uiState.modelUri == null) "Choose model" else "Change",
            modifier = Modifier.padding(start = 8.dp),
          )
        }
        if (uiState.runtimeState == LocalImageRuntimeState.MODEL_SELECTED) {
          Button(onClick = onLoadModel) { Text("Load") }
        }
      }
    }
  }
}

@Composable
private fun GenerationControls(
  uiState: LocalImageGeneratorUiState,
  enabled: Boolean,
  onCanvasChanged: (LocalImageCanvas) -> Unit,
  onStepsChanged: (Int) -> Unit,
  onCfgScaleChanged: (Float) -> Unit,
  onSeedChanged: (String) -> Unit,
) {
  Card {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Canvas",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LocalImageCanvas.entries.forEach { canvas ->
          FilterChip(
            selected = uiState.canvas == canvas,
            enabled = enabled,
            onClick = { onCanvasChanged(canvas) },
            label = { Text(canvas.label) },
          )
        }
      }

      HorizontalDivider()
      Text(
        text = "Steps: ${uiState.steps}",
        style = MaterialTheme.typography.labelLarge,
      )
      Slider(
        value = uiState.steps.toFloat(),
        enabled = enabled,
        onValueChange = { onStepsChanged(it.roundToInt()) },
        valueRange = 4f..30f,
        steps = 25,
      )
      Text(
        text = "Guidance: ${String.format(Locale.getDefault(), "%.1f", uiState.cfgScale)}",
        style = MaterialTheme.typography.labelLarge,
      )
      Slider(
        value = uiState.cfgScale,
        enabled = enabled,
        onValueChange = onCfgScaleChanged,
        valueRange = 1f..14f,
        steps = 25,
      )
      OutlinedTextField(
        value = uiState.seedText,
        onValueChange = onSeedChanged,
        enabled = enabled,
        label = { Text("Seed") },
        placeholder = { Text("Blank = random") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun GeneratedImageCard(
  imagePath: String?,
  width: Int,
  height: Int,
  elapsedMs: Long?,
  onSave: (String) -> Unit,
) {
  if (imagePath == null) return
  val image =
    remember(imagePath) {
      BitmapFactory.decodeFile(imagePath)?.asImageBitmap()
    } ?: return

  Card {
    Column(
      modifier = Modifier.fillMaxWidth().padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Image(
        bitmap = image,
        contentDescription = "Generated image",
        contentScale = ContentScale.Fit,
        modifier =
          Modifier.fillMaxWidth()
            .aspectRatio(width.toFloat() / height.toFloat()),
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = elapsedMs?.let { "Generated in ${formatDuration(it)}" }.orEmpty(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { onSave(imagePath) }) {
          Icon(Icons.Outlined.Download, contentDescription = null)
          Text("Save PNG", modifier = Modifier.padding(start = 8.dp))
        }
      }
    }
  }
}

internal fun formatBytes(bytes: Long): String {
  if (bytes < 0) return "Unknown size"
  val gibibyte = 1024.0 * 1024.0 * 1024.0
  val mebibyte = 1024.0 * 1024.0
  return if (bytes >= gibibyte) {
    "${String.format(Locale.getDefault(), "%.2f", bytes / gibibyte)} GiB"
  } else {
    "${String.format(Locale.getDefault(), "%.0f", bytes / mebibyte)} MiB"
  }
}
