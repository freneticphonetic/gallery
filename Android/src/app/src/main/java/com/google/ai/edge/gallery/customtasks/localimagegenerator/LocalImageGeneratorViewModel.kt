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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREFERENCES_NAME = "local_image_generator"
private const val MODEL_URI_KEY = "model_uri"
private const val MODEL_NAME_KEY = "model_name"
private const val MODEL_SIZE_KEY = "model_size"

enum class LocalImageRuntimeState {
  NO_MODEL,
  MODEL_SELECTED,
  LOADING_MODEL,
  READY,
  GENERATING,
}

val LocalImageRuntimeState.isBusy: Boolean
  get() = this == LocalImageRuntimeState.LOADING_MODEL || this == LocalImageRuntimeState.GENERATING

enum class LocalImageCanvas(val label: String, val width: Int, val height: Int) {
  SQUARE(label = "Square", width = 512, height = 512),
  PORTRAIT(label = "Portrait", width = 384, height = 512),
  LANDSCAPE(label = "Landscape", width = 512, height = 384),
}

data class LocalImageGeneratorUiState(
  val runtimeState: LocalImageRuntimeState = LocalImageRuntimeState.NO_MODEL,
  val modelUri: Uri? = null,
  val modelName: String = "",
  val modelSizeBytes: Long = -1L,
  val prompt: String = "",
  val negativePrompt: String = "",
  val canvas: LocalImageCanvas = LocalImageCanvas.SQUARE,
  val steps: Int = 12,
  val cfgScale: Float = 7f,
  val seedText: String = "",
  val generatedImagePath: String? = null,
  val generatedImageWidth: Int? = null,
  val generatedImageHeight: Int? = null,
  val generationElapsedMs: Long? = null,
  val errorMessage: String = "",
  val notice: String = "",
)

@HiltViewModel
class LocalImageGeneratorViewModel
@Inject
constructor(
  @param:ApplicationContext private val context: Context,
  private val engine: LocalImageGeneratorEngine,
) : ViewModel() {
  private val preferences =
    context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
  private val savedModelUri =
    preferences.getString(MODEL_URI_KEY, null)?.let { uri ->
      runCatching { uri.toUri() }.getOrNull()
    }
  private val _uiState =
    MutableStateFlow(
      LocalImageGeneratorUiState(
        runtimeState =
          if (savedModelUri == null) {
            LocalImageRuntimeState.NO_MODEL
          } else {
            LocalImageRuntimeState.MODEL_SELECTED
          },
        modelUri = savedModelUri,
        modelName = preferences.getString(MODEL_NAME_KEY, "").orEmpty(),
        modelSizeBytes = preferences.getLong(MODEL_SIZE_KEY, -1L),
      )
    )
  val uiState = _uiState.asStateFlow()

  fun prefillPrompt(text: String?) {
    val initialPrompt = text?.trim().orEmpty()
    if (initialPrompt.isEmpty()) return
    _uiState.update { state ->
      if (state.prompt.isBlank()) state.copy(prompt = initialPrompt) else state
    }
  }

  fun setPrompt(value: String) {
    _uiState.update { it.copy(prompt = value, errorMessage = "") }
  }

  fun setNegativePrompt(value: String) {
    _uiState.update { it.copy(negativePrompt = value, errorMessage = "") }
  }

  fun setCanvas(value: LocalImageCanvas) {
    _uiState.update { it.copy(canvas = value) }
  }

  fun setSteps(value: Int) {
    _uiState.update { it.copy(steps = value.coerceIn(1, 40)) }
  }

  fun setCfgScale(value: Float) {
    _uiState.update { it.copy(cfgScale = value.coerceIn(1f, 20f)) }
  }

  fun setSeedText(value: String) {
    if (value.isEmpty() || value == "-" || value.toLongOrNull() != null) {
      _uiState.update { it.copy(seedText = value, errorMessage = "") }
    }
  }

  fun selectModel(uri: Uri) {
    if (uiState.value.runtimeState.isBusy) return
    viewModelScope.launch {
      val permissionResult =
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
      if (permissionResult.isFailure) {
        _uiState.update {
          it.copy(
            errorMessage =
              "Gallery could not keep read access to that file. Choose a checkpoint from local " +
                "device storage.",
            notice = "",
          )
        }
        return@launch
      }

      val infoResult = withContext(Dispatchers.IO) { engine.inspectModel(uri) }
      infoResult
        .onSuccess { info ->
          preferences.edit {
            putString(MODEL_URI_KEY, info.uri.toString())
            putString(MODEL_NAME_KEY, info.displayName)
            putLong(MODEL_SIZE_KEY, info.sizeBytes)
          }
          _uiState.update {
            it.copy(
              runtimeState = LocalImageRuntimeState.MODEL_SELECTED,
              modelUri = info.uri,
              modelName = info.displayName,
              modelSizeBytes = info.sizeBytes,
              errorMessage = "",
              notice = "",
            )
          }
          loadSelectedModel()
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(errorMessage = error.message ?: "The selected model could not be read.")
          }
        }
    }
  }

  fun loadSelectedModel() {
    val state = uiState.value
    val uri = state.modelUri ?: return
    if (
      state.runtimeState == LocalImageRuntimeState.LOADING_MODEL ||
        state.runtimeState == LocalImageRuntimeState.GENERATING ||
        state.runtimeState == LocalImageRuntimeState.READY
    ) {
      return
    }

    _uiState.update {
      it.copy(
        runtimeState = LocalImageRuntimeState.LOADING_MODEL,
        errorMessage = "",
        notice = "Opening the checkpoint directly from local storage…",
      )
    }
    viewModelScope.launch {
      val threads = (Runtime.getRuntime().availableProcessors() - 1).coerceIn(1, 8)
      val result = withContext(Dispatchers.Default) { engine.loadModel(uri, threads) }
      result
        .onSuccess { info ->
          _uiState.update {
            it.copy(
              runtimeState = LocalImageRuntimeState.READY,
              modelName = info.displayName,
              modelSizeBytes = info.sizeBytes,
              errorMessage = "",
              notice = "Model ready. Generation stays on this device.",
            )
          }
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              runtimeState = LocalImageRuntimeState.MODEL_SELECTED,
              errorMessage = error.message ?: "The model could not be loaded.",
              notice = "",
            )
          }
        }
    }
  }

  fun generate() {
    val state = uiState.value
    if (state.runtimeState != LocalImageRuntimeState.READY) return
    val seed =
      when {
        state.seedText.isBlank() -> -1L
        state.seedText.toLongOrNull() != null -> state.seedText.toLong()
        else -> {
          _uiState.update { it.copy(errorMessage = "Seed must be a whole number or left blank.") }
          return
        }
      }
    val request =
      LocalImageGenerationRequest(
        prompt = state.prompt,
        negativePrompt = state.negativePrompt,
        width = state.canvas.width,
        height = state.canvas.height,
        steps = state.steps,
        cfgScale = state.cfgScale,
        seed = seed,
      )
    validateGenerationRequest(request)?.let { message ->
      _uiState.update { it.copy(errorMessage = message) }
      return
    }

    _uiState.update {
      it.copy(
        runtimeState = LocalImageRuntimeState.GENERATING,
        errorMessage = "",
        notice =
          "Generating entirely on device. This may take several minutes; keep Gallery open.",
      )
    }
    viewModelScope.launch {
      val startedAt = SystemClock.elapsedRealtime()
      val result = withContext(Dispatchers.Default) { engine.generate(request) }
      val elapsed = SystemClock.elapsedRealtime() - startedAt
      result
        .onSuccess { image ->
          _uiState.update {
            it.copy(
              runtimeState = LocalImageRuntimeState.READY,
              generatedImagePath = image.file.absolutePath,
              generatedImageWidth = image.width,
              generatedImageHeight = image.height,
              generationElapsedMs = elapsed,
              errorMessage = "",
              notice = "Generated locally in ${formatDuration(elapsed)}.",
            )
          }
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              runtimeState = LocalImageRuntimeState.READY,
              errorMessage = error.message ?: "Image generation failed.",
              notice = "",
            )
          }
        }
    }
  }

  fun exportLatest(destination: Uri) {
    val source = uiState.value.generatedImagePath?.let(::File) ?: return
    viewModelScope.launch {
      val result = withContext(Dispatchers.IO) { engine.exportImage(source, destination) }
      result
        .onSuccess {
          _uiState.update { it.copy(notice = "PNG copy saved.", errorMessage = "") }
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(errorMessage = error.message ?: "The PNG could not be saved.")
          }
        }
    }
  }

  fun explainBusyNavigation() {
    _uiState.update {
      it.copy(
        notice =
          "The current native operation cannot be cancelled safely. Wait for it to finish before " +
            "leaving this screen."
      )
    }
  }
}

internal fun formatDuration(elapsedMs: Long): String {
  val totalSeconds = (elapsedMs / 1000).coerceAtLeast(1)
  val minutes = totalSeconds / 60
  val seconds = totalSeconds % 60
  return if (minutes == 0L) "${seconds}s" else "${minutes}m ${seconds}s"
}
