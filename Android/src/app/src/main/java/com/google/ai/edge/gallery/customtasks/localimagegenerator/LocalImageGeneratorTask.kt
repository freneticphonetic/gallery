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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.runtime.Composable
import com.google.ai.edge.gallery.customtasks.common.CustomTask
import com.google.ai.edge.gallery.customtasks.common.CustomTaskData
import com.google.ai.edge.gallery.data.BuiltInTaskId
import com.google.ai.edge.gallery.data.Category
import com.google.ai.edge.gallery.data.Model
import com.google.ai.edge.gallery.data.Task
import com.google.ai.edge.litertlm.Contents
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

private const val RUNTIME_MODEL_NAME = "Local image runtime"

private object LocalImageRuntimeHandle

class LocalImageGeneratorTask
@Inject
constructor(private val engine: LocalImageGeneratorEngine) : CustomTask {
  override val task =
    Task(
      id = BuiltInTaskId.LOCAL_IMAGE_GENERATOR,
      label = "Image Generator",
      description =
        "Generate images entirely on device from a local Stable Diffusion checkpoint. No prompt, " +
          "model, or image is sent over a network.",
      shortDescription = "Generate images fully offline",
      sourceCodeUrl =
        "https://github.com/freneticphonetic/gallery/tree/main/Android/src/app/src/main/java/" +
          "com/google/ai/edge/gallery/customtasks/localimagegenerator",
      category = Category.LLM,
      icon = Icons.Outlined.AutoAwesome,
      models =
        mutableListOf(
          Model(
            name = RUNTIME_MODEL_NAME,
            displayName = "stable-diffusion.cpp",
            info =
              "CPU-first local image runtime. Select a compatible checkpoint inside the tool.",
            localFileRelativeDirPathOverride = "local_image_generator_runtime",
            downloadFileName = "runtime",
            bestForTaskIds = listOf(BuiltInTaskId.LOCAL_IMAGE_GENERATOR),
            showBenchmarkButton = false,
            showRunAgainButton = false,
          )
        ),
      handleModelConfigChangesInTask = true,
      experimental = true,
      newFeature = true,
      useThemeColor = true,
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    systemInstruction: Contents?,
    onDone: (String) -> Unit,
  ) {
    model.instance = LocalImageRuntimeHandle
    onDone("")
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    engine.release()
    model.instance = null
    onDone()
  }

  @Composable
  override fun MainScreen(data: Any) {
    LocalImageGeneratorScreen(data = data as CustomTaskData)
  }
}
