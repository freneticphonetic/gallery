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

package com.google.ai.edge.gallery.ui.modelmanager

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.ModelDownloadStatusType
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.gallery.data.supportModelBenchmark
import com.google.ai.edge.gallery.proto.ImportedModel
import com.google.ai.edge.gallery.ui.common.TaskIcon
import com.google.ai.edge.gallery.ui.common.humanReadableSize
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalModelManager(
  viewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  onModelSelected: (Task, Model) -> Unit,
  onBenchmarkClicked: (Model) -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val taskSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var selectedUri by remember { mutableStateOf<Uri?>(null) }
  var selectedImportInfo by remember { mutableStateOf<ImportedModel?>(null) }
  var showImportDialog by remember { mutableStateOf(false) }
  var showImportingDialog by remember { mutableStateOf(false) }
  var validationError by remember { mutableStateOf<String?>(null) }
  var modelForTaskChoice by remember { mutableStateOf<Model?>(null) }
  var taskChoices by remember { mutableStateOf<List<Task>>(emptyList()) }
  var modelToDelete by remember { mutableStateOf<Model?>(null) }

  val localModels =
    viewModel
      .getAllModels()
      .filter { model ->
        model.parentModelName.isNullOrBlank() &&
          uiState.modelDownloadStatus[model.name]?.status == ModelDownloadStatusType.SUCCEEDED
      }
      .sortedBy { it.displayName.ifBlank { it.name }.lowercase() }

  fun openModel(model: Model) {
    val matchingTasks = uiState.tasks.filter { task -> task.models.any { it.name == model.name } }
    when (matchingTasks.size) {
      0 -> scope.launch { snackbarHostState.showSnackbar("No compatible tool is available") }
      1 -> onModelSelected(matchingTasks.first(), model)
      else -> {
        modelForTaskChoice = model
        taskChoices = matchingTasks
      }
    }
  }

  val filePicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        val fileName = getDisplayName(context = context, uri = uri)
        if (fileName == null || !isSupportedLocalModel(fileName)) {
          validationError = "Choose a .litertlm or .task model file."
        } else if (fileName.lowercase().contains("-web")) {
          validationError = "Web-hosted model bundles are not supported in the offline app."
        } else {
          selectedUri = uri
          showImportDialog = true
        }
      }
    }

  BackHandler(onBack = navigateUp)

  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Local models", style = MaterialTheme.typography.titleMedium)
            Text(
              text =
                if (localModels.size == 1) "1 model on this device"
                else "${localModels.size} models on this device",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        actions = {
          IconButton(onClick = navigateUp) {
            Icon(Icons.Rounded.Close, contentDescription = "Close")
          }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { filePicker.launch(arrayOf("application/octet-stream", "*/*")) },
        icon = { Icon(Icons.Outlined.FolderOpen, contentDescription = null) },
        text = { Text("Import model") },
      )
    },
  ) { innerPadding ->
    if (localModels.isEmpty()) {
      EmptyModelLibrary(
        contentPadding = innerPadding,
        onImport = { filePicker.launch(arrayOf("application/octet-stream", "*/*")) },
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item(key = "local_only_note") {
          Card(
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(18.dp),
          ) {
            Text(
              text = "Only files stored on this device appear here. Downloads and cloud catalogs are disabled.",
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }

        items(localModels, key = { it.name }) { model ->
          LocalModelCard(
            model = model,
            compatibleTasks =
              uiState.tasks.filter { task -> task.models.any { it.name == model.name } },
            sizeInBytes =
              uiState.modelDownloadStatus[model.name]?.totalBytes?.takeIf { it > 0 }
                ?: model.sizeInBytes,
            onOpen = { openModel(model) },
            onBenchmark = { onBenchmarkClicked(model) },
            onDelete = { modelToDelete = model },
          )
        }
      }
    }
  }

  if (taskChoices.isNotEmpty()) {
    ModalBottomSheet(
      onDismissRequest = {
        taskChoices = emptyList()
        modelForTaskChoice = null
      },
      sheetState = taskSheetState,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text("Choose a tool", style = MaterialTheme.typography.titleLarge)
        taskChoices.forEach { task ->
          Card(
            onClick = {
              modelForTaskChoice?.let { onModelSelected(task, it) }
              taskChoices = emptyList()
              modelForTaskChoice = null
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              TaskIcon(task = task, width = 36.dp)
              Text(task.label, style = MaterialTheme.typography.titleMedium)
            }
          }
        }
      }
    }
  }

  if (showImportDialog) {
    selectedUri?.let { uri ->
      ModelImportDialog(
        uri = uri,
        onDismiss = {
          showImportDialog = false
          selectedUri = null
        },
        onDone = { info ->
          selectedImportInfo = info
          showImportDialog = false
          showImportingDialog = true
        },
      )
    }
  }

  if (showImportingDialog) {
    val uri = selectedUri
    val info = selectedImportInfo
    if (uri != null && info != null) {
      ModelImportingDialog(
        uri = uri,
        info = info,
        onDismiss = {
          showImportingDialog = false
          selectedUri = null
          selectedImportInfo = null
        },
        onDone = { importedInfo ->
          viewModel.addImportedLlmModel(importedInfo)
          showImportingDialog = false
          selectedUri = null
          selectedImportInfo = null
          scope.launch { snackbarHostState.showSnackbar("Model imported") }
        },
      )
    }
  }

  validationError?.let { error ->
    AlertDialog(
      onDismissRequest = { validationError = null },
      icon = { Icon(Icons.Rounded.Error, contentDescription = null) },
      title = { Text("Unsupported file") },
      text = { Text(error) },
      confirmButton = {
        Button(onClick = { validationError = null }) { Text("OK") }
      },
    )
  }

  modelToDelete?.let { model ->
    AlertDialog(
      onDismissRequest = { modelToDelete = null },
      title = { Text("Remove local model?") },
      text = {
        Text("This deletes ${model.displayName.ifBlank { model.name }} from the app's local storage.")
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteModel(model)
            modelToDelete = null
          }
        ) {
          Text("Remove")
        }
      },
      dismissButton = {
        TextButton(onClick = { modelToDelete = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun EmptyModelLibrary(contentPadding: PaddingValues, onImport: () -> Unit) {
  Box(
    modifier = Modifier.fillMaxSize().padding(contentPadding).padding(32.dp),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        Icons.Outlined.FolderOpen,
        contentDescription = null,
        modifier = Modifier.size(52.dp),
        tint = MaterialTheme.colorScheme.primary,
      )
      Text("No local models yet", style = MaterialTheme.typography.headlineSmall)
      Text(
        text = "Choose a .litertlm or .task file already stored on this device.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(onClick = onImport) {
        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Choose a file")
      }
    }
  }
}

@Composable
private fun LocalModelCard(
  model: Model,
  compatibleTasks: List<Task>,
  sizeInBytes: Long,
  onOpen: () -> Unit,
  onBenchmark: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
          Icons.Outlined.Memory,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = model.displayName.ifBlank { model.name },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          if (sizeInBytes > 0) {
            Text(
              text = sizeInBytes.humanReadableSize(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Text(
        text =
          if (compatibleTasks.isEmpty()) "No compatible tools"
          else "Works with ${compatibleTasks.joinToString { it.label }}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (model.imported) {
          IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Remove model")
          }
        }
        if (model.supportModelBenchmark) {
          OutlinedButton(onClick = onBenchmark) { Text("Benchmark") }
        }
        FilledTonalButton(onClick = onOpen, enabled = compatibleTasks.isNotEmpty()) {
          Text("Open")
        }
      }
    }
  }
}

private fun isSupportedLocalModel(fileName: String): Boolean {
  val normalizedName = fileName.lowercase()
  return normalizedName.endsWith(".litertlm") || normalizedName.endsWith(".task")
}

private fun getDisplayName(context: android.content.Context, uri: Uri): String? {
  context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
    if (cursor.moveToFirst()) {
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (nameIndex >= 0) return cursor.getString(nameIndex)
    }
  }
  return uri.lastPathSegment
}
