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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.TaskIcon
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel

enum class HomePromptDelivery {
  SUBMIT,
  PREFILL,
}

data class HomeLaunchRequest(
  val task: Task,
  val model: Model,
  val text: String? = null,
  val delivery: HomePromptDelivery = HomePromptDelivery.SUBMIT,
  val startAudioRecording: Boolean = false,
)

internal fun homePromptDelivery(taskId: String): HomePromptDelivery =
  when (taskId) {
    BuiltInTaskId.LLM_ASK_IMAGE,
    BuiltInTaskId.LLM_ASK_AUDIO,
    BuiltInTaskId.LLM_PROMPT_LAB -> HomePromptDelivery.PREFILL
    else -> HomePromptDelivery.SUBMIT
  }

/** A quiet, composer-first home screen for the offline application. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  modelManagerViewModel: ModelManagerViewModel,
  onLaunch: (HomeLaunchRequest) -> Unit,
  onModelsClicked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by modelManagerViewModel.uiState.collectAsState()
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showToolPicker by remember { mutableStateOf(false) }
  var prompt by rememberSaveable { mutableStateOf("") }
  var selectedTaskId by rememberSaveable { mutableStateOf("") }

  val localModelCount =
    uiState.tasks
      .flatMap { it.models }
      .distinctBy { it.name }
      .count {
        uiState.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED
      }

  fun readyModels(task: Task?): List<Model> =
    task?.models
      ?.filter {
        uiState.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED
      }
      .orEmpty()

  val initialTask =
    listOf(BuiltInTaskId.LLM_AGENT_CHAT, BuiltInTaskId.LLM_CHAT)
      .firstNotNullOfOrNull { preferredId ->
        uiState.tasks.find { it.id == preferredId && readyModels(it).isNotEmpty() }
      }
      ?: uiState.tasks.firstOrNull { readyModels(it).isNotEmpty() }
      ?: uiState.tasks.find { it.id == BuiltInTaskId.LLM_AGENT_CHAT }
      ?: uiState.tasks.find { it.id == BuiltInTaskId.LLM_CHAT }
      ?: uiState.tasks.firstOrNull()

  LaunchedEffect(initialTask?.id, uiState.tasks.map { it.id }) {
    if (uiState.tasks.none { it.id == selectedTaskId }) {
      selectedTaskId = initialTask?.id.orEmpty()
    }
  }

  val selectedTask = uiState.tasks.find { it.id == selectedTaskId } ?: initialTask
  val selectedTaskReadyModels = readyModels(selectedTask)
  val selectedModel =
    uiState.selectedModel.takeIf { selected ->
      selectedTaskReadyModels.any { it.name == selected.name }
    } ?: selectedTaskReadyModels.firstOrNull()

  val audioTask = uiState.tasks.find { it.id == BuiltInTaskId.LLM_ASK_AUDIO }
  val audioReadyModels = readyModels(audioTask)
  val audioModel =
    uiState.selectedModel.takeIf { selected ->
      audioReadyModels.any { it.name == selected.name }
    } ?: audioReadyModels.firstOrNull()
  val focusManager = LocalFocusManager.current

  fun sendPrompt() {
    val task = selectedTask ?: return
    val model = selectedModel ?: return
    val text = prompt.trim()
    if (text.isEmpty()) return

    focusManager.clearFocus()
    onLaunch(
      HomeLaunchRequest(
        task = task,
        model = model,
        text = text,
        delivery = homePromptDelivery(task.id),
      )
    )
    prompt = ""
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Private, on-device AI",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        actions = {
          IconButton(onClick = onModelsClicked) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = "Local models")
          }
          IconButton(onClick = { showSettingsDialog = true }) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
          }
        },
      )
    },
    bottomBar = {
      HomeComposer(
        task = selectedTask,
        model = selectedModel,
        prompt = prompt,
        enabled = !uiState.loadingModels && selectedModel != null,
        onPromptChanged = { prompt = it },
        onChooseTool = { showToolPicker = true },
        onVoiceInput = {
          if (audioTask != null && audioModel != null) {
            focusManager.clearFocus()
            onLaunch(
              HomeLaunchRequest(
                task = audioTask,
                model = audioModel,
                delivery = HomePromptDelivery.PREFILL,
                startAudioRecording = true,
              )
            )
          } else {
            selectedTaskId = audioTask?.id.orEmpty()
            onModelsClicked()
          }
        },
        onSend = ::sendPrompt,
        focusManager = focusManager,
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      OfflineStatusCard(localModelCount = localModelCount)

      if (uiState.loadingModels) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          Text(
            text = "Finding models stored on this device…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (uiState.loadingModelsError.isNotBlank()) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text("Local models could not be loaded", fontWeight = FontWeight.SemiBold)
            Text(
              uiState.loadingModelsError,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = modelManagerViewModel::loadLocalModels) { Text("Try again") }
          }
        }
      }

      if (!uiState.loadingModels && localModelCount == 0) {
        SetupCard(onModelsClicked = onModelsClicked)
      }

      Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        selectedTask?.let { task ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
          ) {
            TaskIcon(task = task, width = 72.dp)
            Text(
              text = task.label,
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = task.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
            if (selectedModel != null) {
              FilledTonalButton(onClick = { showToolPicker = true }) {
                Text(selectedModel.displayName.ifBlank { selectedModel.name })
                Spacer(Modifier.width(6.dp))
                Icon(
                  Icons.Outlined.KeyboardArrowDown,
                  contentDescription = "Choose another tool",
                  modifier = Modifier.size(18.dp),
                )
              }
              Text(
                text =
                  when (homePromptDelivery(task.id)) {
                    HomePromptDelivery.SUBMIT -> "Type below to begin."
                    HomePromptDelivery.PREFILL ->
                      "Type below, then add media or adjust the prompt before sending."
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            } else if (!uiState.loadingModels) {
              Text(
                text = "This tool needs a compatible local model.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Button(onClick = onModelsClicked) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose local model")
              }
            }
          }
        }
        if (selectedTask == null && !uiState.loadingModels) {
          Text(
            text = "No on-device tools are available.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }

  if (showToolPicker) {
    ModalBottomSheet(onDismissRequest = { showToolPicker = false }) {
      Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp)) {
        Text(
          text = "Tools and inputs",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        Text(
          text = "Choose what the composer should do. Everything runs with models on this device.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
        )
        uiState.tasks.forEach { task ->
          val readyCount = readyModels(task).size
          ListItem(
            headlineContent = {
              Text(task.label, fontWeight = FontWeight.Medium)
            },
            supportingContent = {
              Text(
                if (readyCount == 0) {
                  "Needs a compatible model"
                } else if (readyCount == 1) {
                  "${task.shortDescription.ifBlank { task.description }} · 1 model ready"
                } else {
                  "${task.shortDescription.ifBlank { task.description }} · $readyCount models ready"
                }
              )
            },
            leadingContent = { TaskIcon(task = task, width = 42.dp) },
            trailingContent = {
              if (task.id == selectedTask?.id) {
                Icon(Icons.Rounded.Check, contentDescription = "Selected")
              }
            },
            modifier =
              Modifier.fillMaxWidth().clickable {
                selectedTaskId = task.id
                showToolPicker = false
              },
          )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        ListItem(
          headlineContent = { Text("Local models", fontWeight = FontWeight.Medium) },
          supportingContent = { Text("Import, configure, or remove models") },
          leadingContent = {
            Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(32.dp))
          },
          modifier =
            Modifier.fillMaxWidth().clickable {
              showToolPicker = false
              onModelsClicked()
            },
        )
      }
    }
  }

  if (showSettingsDialog) {
    SettingsDialog(
      curThemeOverride = modelManagerViewModel.readThemeOverride(),
      modelManagerViewModel = modelManagerViewModel,
      onDismissed = { showSettingsDialog = false },
    )
  }
}

@Composable
private fun HomeComposer(
  task: Task?,
  model: Model?,
  prompt: String,
  enabled: Boolean,
  onPromptChanged: (String) -> Unit,
  onChooseTool: () -> Unit,
  onVoiceInput: () -> Unit,
  onSend: () -> Unit,
  focusManager: FocusManager,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shadowElevation = 8.dp,
    modifier = Modifier.fillMaxWidth().imePadding(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clickable(onClick = onChooseTool)
            .padding(start = 8.dp, top = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        task?.let {
          TaskIcon(task = it, width = 24.dp)
          Spacer(Modifier.width(8.dp))
        }
        Text(
          text = task?.label ?: "Choose a tool",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Medium,
        )
        Icon(
          Icons.Outlined.KeyboardArrowDown,
          contentDescription = "Choose tool",
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
          text =
            model?.displayName?.ifBlank { model.name }
              ?: if (task == null) "" else "No compatible model",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = onChooseTool) {
            Icon(Icons.Outlined.Add, contentDescription = "Add files or choose a tool")
          }
          TextField(
            value = prompt,
            enabled = enabled,
            onValueChange = onPromptChanged,
            minLines = 1,
            maxLines = 4,
            placeholder = {
              Text(
                if (enabled) "Message ${task?.label ?: "local AI"}"
                else "Import a compatible model to begin"
              )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions =
              KeyboardActions(
                onSend = {
                  if (enabled && prompt.isNotBlank()) {
                    onSend()
                  } else {
                    focusManager.clearFocus()
                  }
                }
              ),
            colors =
              TextFieldDefaults.colors(
                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
              ),
            modifier = Modifier.weight(1f),
          )
          IconButton(onClick = onVoiceInput) {
            Icon(Icons.Rounded.Mic, contentDescription = "Transcribe speech with Audio Scribe")
          }
          IconButton(
            enabled = enabled && prompt.isNotBlank(),
            onClick = onSend,
            colors =
              IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
              ),
          ) {
            Icon(
              Icons.AutoMirrored.Rounded.Send,
              contentDescription = "Send",
              modifier = Modifier.padding(start = 2.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OfflineStatusCard(localModelCount: Int) {
  Card(
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    shape = RoundedCornerShape(24.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Icon(
        imageVector = Icons.Outlined.Lock,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(28.dp),
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = "Offline by design",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text =
            "No network permission. Prompts, media, and model files stay on this device.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
          text =
            if (localModelCount == 1) "1 local model ready"
            else "$localModelCount local models ready",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }
  }
}

@Composable
private fun SetupCard(onModelsClicked: () -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "Add your first model",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = "Import a .litertlm or .task file already stored on this device.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(onClick = onModelsClicked) {
        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Import local model")
      }
    }
  }
}
