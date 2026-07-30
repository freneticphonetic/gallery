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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.ai.edge.gallery.R
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.ui.common.TaskIcon
import com.google.ai.edge.gallery.ui.common.chat.ChatHistorySideSheetContent
import com.google.ai.edge.gallery.ui.llmchat.LlmChatViewModel
import com.google.ai.edge.gallery.ui.modelmanager.ModelManagerViewModel
import kotlinx.coroutines.launch

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
  val initialSessionId: String? = null,
)

internal fun homePromptDelivery(taskId: String): HomePromptDelivery =
  when (taskId) {
    BuiltInTaskId.LLM_ASK_IMAGE,
    BuiltInTaskId.LLM_ASK_AUDIO,
    BuiltInTaskId.LLM_AGENT_CHAT,
    BuiltInTaskId.LLM_PROMPT_LAB,
    BuiltInTaskId.LOCAL_IMAGE_GENERATOR -> HomePromptDelivery.PREFILL
    else -> HomePromptDelivery.SUBMIT
  }

/** The compact new-chat workspace for the offline application. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  modelManagerViewModel: ModelManagerViewModel,
  onLaunch: (HomeLaunchRequest) -> Unit,
  onModelsClicked: () -> Unit,
  modifier: Modifier = Modifier,
  historyViewModel: LlmChatViewModel = hiltViewModel(),
) {
  val uiState by modelManagerViewModel.uiState.collectAsState()
  val historySessions by historyViewModel.historySessions.collectAsState()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  var showSettingsDialog by remember { mutableStateOf(false) }
  var showInputPicker by remember { mutableStateOf(false) }
  var prompt by rememberSaveable { mutableStateOf("") }

  fun readyModels(task: Task?): List<Model> =
    task?.models
      ?.filter {
        uiState.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED
      }
      .orEmpty()

  fun preferredReadyModel(task: Task?): Model? {
    val models = readyModels(task)
    return uiState.selectedModel.takeIf { selected ->
      models.any { it.name == selected.name }
    } ?: models.firstOrNull()
  }

  val localModelCount =
    uiState.tasks
      .flatMap { it.models }
      .distinctBy { it.name }
      .count {
        uiState.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED
      }

  val chatTask =
    listOf(BuiltInTaskId.LLM_CHAT, BuiltInTaskId.LLM_AGENT_CHAT)
      .firstNotNullOfOrNull { preferredId ->
        uiState.tasks.find { it.id == preferredId && readyModels(it).isNotEmpty() }
      }
      ?: uiState.tasks.find { it.id == BuiltInTaskId.LLM_CHAT }
      ?: uiState.tasks.find { it.id == BuiltInTaskId.LLM_AGENT_CHAT }
  val chatModel = preferredReadyModel(chatTask)
  val audioTask = uiState.tasks.find { it.id == BuiltInTaskId.LLM_ASK_AUDIO }

  fun launchTask(
    task: Task,
    delivery: HomePromptDelivery,
    startAudioRecording: Boolean = false,
  ) {
    val model = preferredReadyModel(task)
    showInputPicker = false
    if (model == null) {
      onModelsClicked()
      return
    }

    focusManager.clearFocus()
    onLaunch(
      HomeLaunchRequest(
        task = task,
        model = model,
        text = prompt.trim().takeIf { it.isNotEmpty() },
        delivery = delivery,
        startAudioRecording = startAudioRecording,
      )
    )
  }

  fun sendPrompt() {
    val task = chatTask ?: return
    val model = chatModel ?: return
    val text = prompt.trim()
    if (text.isEmpty()) return

    focusManager.clearFocus()
    onLaunch(
      HomeLaunchRequest(
        task = task,
        model = model,
        text = text,
        delivery = HomePromptDelivery.SUBMIT,
      )
    )
    prompt = ""
  }

  fun openSavedChat(sessionId: String) {
    val session = historySessions.firstOrNull { it.sessionId == sessionId }
    val task = session?.let { saved -> uiState.tasks.find { it.id == saved.taskId } }
    val models = readyModels(task)
    val model =
      session?.let { saved -> models.find { it.name == saved.originalModel } }
        ?: preferredReadyModel(task)

    scope.launch { drawerState.close() }
    if (task == null || model == null) {
      onModelsClicked()
      return
    }

    onLaunch(
      HomeLaunchRequest(
        task = task,
        model = model,
        initialSessionId = sessionId,
      )
    )
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = true,
    drawerContent = {
      ModalDrawerSheet {
        ChatHistorySideSheetContent(
          history = historySessions,
          onHistoryItemClicked = ::openSavedChat,
          onHistoryItemDeleted = { sessionId ->
            historyViewModel.deleteSession(sessionId, context)
          },
          onHistoryItemsDeleteAll = {
            historyViewModel.clearAllSessions(context)
            scope.launch { drawerState.close() }
          },
          onNewChatClicked = {
            prompt = ""
            focusManager.clearFocus()
            scope.launch { drawerState.close() }
          },
          onDismissed = { scope.launch { drawerState.close() } },
        )
      }
    },
  ) {
    Scaffold(
      modifier = modifier,
      topBar = {
        CenterAlignedTopAppBar(
          navigationIcon = {
            IconButton(onClick = { scope.launch { drawerState.open() } }) {
              Icon(Icons.Rounded.Menu, contentDescription = "Open chats")
            }
          },
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
          model = chatModel,
          prompt = prompt,
          enabled = !uiState.loadingModels && chatModel != null,
          onPromptChanged = { prompt = it },
          onAdd = { showInputPicker = true },
          onVoiceInput = {
            if (audioTask != null) {
              launchTask(
                task = audioTask,
                delivery = HomePromptDelivery.PREFILL,
                startAudioRecording = true,
              )
            } else {
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
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        CompactDeviceStatus(
          localModelCount = localModelCount,
          loading = uiState.loadingModels,
          onModelsClicked = onModelsClicked,
        )

        if (uiState.loadingModelsError.isNotBlank()) {
          Card(
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = "Local models could not be loaded",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
              )
              TextButton(onClick = modelManagerViewModel::loadLocalModels) { Text("Try again") }
            }
          }
        }

        NewChatEmptyState(
          task = chatTask,
          model = chatModel,
          localModelCount = localModelCount,
          loading = uiState.loadingModels,
          onModelsClicked = onModelsClicked,
          modifier = Modifier.fillMaxWidth().weight(1f),
        )
      }
    }
  }

  if (showInputPicker) {
    ChatInputPicker(
      tasks = uiState.tasks,
      readyModels = ::readyModels,
      onTaskSelected = { task, delivery -> launchTask(task, delivery) },
      onModelsClicked = {
        showInputPicker = false
        onModelsClicked()
      },
      onDismissed = { showInputPicker = false },
    )
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
  model: Model?,
  prompt: String,
  enabled: Boolean,
  onPromptChanged: (String) -> Unit,
  onAdd: () -> Unit,
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
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 7.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "New chat",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        Text(
          text =
            model?.displayName?.ifBlank { model.name }
              ?: "No compatible model",
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
          IconButton(onClick = onAdd) {
            Icon(Icons.Outlined.Add, contentDescription = "Add to chat or open tools")
          }
          TextField(
            value = prompt,
            enabled = enabled,
            onValueChange = onPromptChanged,
            minLines = 1,
            maxLines = 4,
            placeholder = {
              Text(if (enabled) "Message Chat" else "Import a model to begin")
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
            Icon(Icons.Rounded.Mic, contentDescription = "Record with Audio")
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
private fun CompactDeviceStatus(
  localModelCount: Int,
  loading: Boolean,
  onModelsClicked: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth().clickable(onClick = onModelsClicked),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Icon(
        imageVector = Icons.Outlined.Lock,
        contentDescription = null,
        modifier = Modifier.size(22.dp),
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Offline by design",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text =
            when {
              loading -> "Checking local models…"
              localModelCount == 0 -> "No local models yet"
              localModelCount == 1 -> "1 local model ready"
              else -> "$localModelCount local models ready"
            },
          style = MaterialTheme.typography.bodySmall,
        )
      }
      Text(
        text = if (localModelCount == 0) "Import" else "Models",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
    }
  }
}

@Composable
private fun NewChatEmptyState(
  task: Task?,
  model: Model?,
  localModelCount: Int,
  loading: Boolean,
  onModelsClicked: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(horizontal = 32.dp),
    ) {
      task?.let { TaskIcon(task = it, width = 52.dp) }
      Text(
        text = "New chat",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text =
          when {
            loading -> "Finding models stored on this device…"
            localModelCount == 0 -> "Import a local model to begin."
            model == null -> "Choose a compatible model for Chat."
            else -> "Start with a message, image, audio recording, or local skill."
          },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (!loading && localModelCount > 0 && model == null) {
        TextButton(onClick = onModelsClicked) { Text("Choose model") }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputPicker(
  tasks: List<Task>,
  readyModels: (Task?) -> List<Model>,
  onTaskSelected: (Task, HomePromptDelivery) -> Unit,
  onModelsClicked: () -> Unit,
  onDismissed: () -> Unit,
) {
  val imageTask = tasks.find { it.id == BuiltInTaskId.LLM_ASK_IMAGE }
  val audioTask = tasks.find { it.id == BuiltInTaskId.LLM_ASK_AUDIO }
  val skillsTask = tasks.find { it.id == BuiltInTaskId.LLM_AGENT_CHAT }
  val promptLabTask = tasks.find { it.id == BuiltInTaskId.LLM_PROMPT_LAB }
  val imageGeneratorTask = tasks.find { it.id == BuiltInTaskId.LOCAL_IMAGE_GENERATOR }
  val mobileActionsTask = tasks.find { it.id == BuiltInTaskId.LLM_MOBILE_ACTIONS }
  val tinyGardenTask = tasks.find { it.id == BuiltInTaskId.LLM_TINY_GARDEN }

  ModalBottomSheet(onDismissRequest = onDismissed) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .navigationBarsPadding()
          .padding(bottom = 12.dp)
    ) {
      Text(
        text = "Add to chat",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
      )
      Text(
        text = "Choose an input or local capability.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
      )

      imageTask?.let {
        ChatInputPickerItem(
          task = it,
          label = "Image",
          description = "Ask about a photo",
          ready = readyModels(it).isNotEmpty(),
          onClick = { onTaskSelected(it, homePromptDelivery(it.id)) },
        )
      }
      audioTask?.let {
        ChatInputPickerItem(
          task = it,
          label = "Audio",
          description = "Transcribe or translate a recording",
          ready = readyModels(it).isNotEmpty(),
          onClick = { onTaskSelected(it, homePromptDelivery(it.id)) },
        )
      }
      skillsTask?.let {
        ChatInputPickerItem(
          task = it,
          label = "Skills",
          description = "Use local skills in chat",
          ready = readyModels(it).isNotEmpty(),
          onClick = { onTaskSelected(it, homePromptDelivery(it.id)) },
        )
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
      Text(
        text = "More tools",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
      )

      imageGeneratorTask?.let {
        ChatInputPickerItem(
          task = it,
          label = it.label,
          description = it.shortDescription.ifBlank { it.description },
          ready = readyModels(it).isNotEmpty(),
          onClick = { onTaskSelected(it, homePromptDelivery(it.id)) },
        )
      }
      promptLabTask?.let {
        ChatInputPickerItem(
          task = it,
          label = it.label,
          description = it.shortDescription.ifBlank { it.description },
          ready = readyModels(it).isNotEmpty(),
          onClick = { onTaskSelected(it, homePromptDelivery(it.id)) },
        )
      }
      mobileActionsTask?.let {
        ChatInputPickerItem(
          task = it,
          label = it.label,
          description = it.shortDescription.ifBlank { it.description },
          ready = readyModels(it).isNotEmpty(),
          onClick = { onTaskSelected(it, homePromptDelivery(it.id)) },
        )
      }
      tinyGardenTask?.let {
        ChatInputPickerItem(
          task = it,
          label = it.label,
          description = it.shortDescription.ifBlank { it.description },
          ready = readyModels(it).isNotEmpty(),
          onClick = { onTaskSelected(it, homePromptDelivery(it.id)) },
        )
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
      ListItem(
        headlineContent = { Text("Local models", fontWeight = FontWeight.Medium) },
        supportingContent = { Text("Import, configure, or remove models") },
        leadingContent = {
          Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(32.dp))
        },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onModelsClicked),
      )
    }
  }
}

@Composable
private fun ChatInputPickerItem(
  task: Task,
  label: String,
  description: String,
  ready: Boolean,
  onClick: () -> Unit,
) {
  ListItem(
    headlineContent = { Text(label, fontWeight = FontWeight.Medium) },
    supportingContent = { Text(if (ready) description else "Needs a compatible model") },
    leadingContent = { TaskIcon(task = task, width = 42.dp) },
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
  )
}
