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
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.llamatik.library.platform.StableDiffusionBridge
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val OUTPUT_DIRECTORY = "LocalImageGenerator"
private val SUPPORTED_MODEL_EXTENSIONS = setOf("ckpt", "gguf", "safetensors")

data class LocalImageModelInfo(
  val uri: Uri,
  val displayName: String,
  val sizeBytes: Long,
)

data class LocalImageGenerationRequest(
  val prompt: String,
  val negativePrompt: String?,
  val width: Int,
  val height: Int,
  val steps: Int,
  val cfgScale: Float,
  val seed: Long,
)

data class LocalGeneratedImage(
  val file: File,
  val width: Int,
  val height: Int,
)

internal fun validateGenerationRequest(request: LocalImageGenerationRequest): String? {
  if (request.prompt.isBlank()) return "Enter a prompt first."
  if (request.width !in 256..768 || request.height !in 256..768) {
    return "Image dimensions must be between 256 and 768 pixels."
  }
  if (request.width % 64 != 0 || request.height % 64 != 0) {
    return "Image dimensions must be multiples of 64."
  }
  if (request.steps !in 1..40) return "Steps must be between 1 and 40."
  if (request.cfgScale !in 1f..20f) return "Guidance must be between 1 and 20."
  return null
}

internal fun rgbaToArgbPixels(rgba: ByteArray): IntArray {
  require(rgba.size % 4 == 0) { "RGBA byte count must be divisible by four." }
  return IntArray(rgba.size / 4) { pixelIndex ->
    val byteIndex = pixelIndex * 4
    val red = rgba[byteIndex].toInt() and 0xff
    val green = rgba[byteIndex + 1].toInt() and 0xff
    val blue = rgba[byteIndex + 2].toInt() and 0xff
    val alpha = rgba[byteIndex + 3].toInt() and 0xff
    (alpha shl 24) or (red shl 16) or (green shl 8) or blue
  }
}

/**
 * Host-controlled bridge to the bundled stable-diffusion.cpp runtime.
 *
 * The selected model stays in its original Storage Access Framework location. A persisted,
 * read-only descriptor is held open while the native runtime uses `/proc/self/fd/<fd>`, avoiding a
 * multi-gigabyte duplicate in app storage.
 */
@Singleton
class LocalImageGeneratorEngine
@Inject
constructor(@param:ApplicationContext private val context: Context) {
  private val nativeLock = Any()
  private var modelDescriptor: ParcelFileDescriptor? = null
  private var loadedModelUri: Uri? = null

  fun inspectModel(uri: Uri): Result<LocalImageModelInfo> = runCatching { queryModelInfo(uri) }

  fun loadModel(uri: Uri, threads: Int): Result<LocalImageModelInfo> =
    runCatching {
      synchronized(nativeLock) {
        val modelInfo = queryModelInfo(uri)
        validateModelName(modelInfo.displayName)
        releaseLocked()

        val descriptor =
          context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("The selected model could not be opened for reading.")
        try {
          val descriptorPath = "/proc/self/fd/${descriptor.fd}"
          val initialized =
            StableDiffusionBridge.initModel(
              modelPath = descriptorPath,
              threads = threads.coerceIn(1, 8),
            )
          if (!initialized) {
            error(
              "The runtime could not load this checkpoint. Choose a supported single-file " +
                ".safetensors, .ckpt, or .gguf model."
            )
          }
          modelDescriptor = descriptor
          loadedModelUri = uri
          modelInfo
        } catch (error: Throwable) {
          descriptor.close()
          throw error
        }
      }
    }

  fun generate(request: LocalImageGenerationRequest): Result<LocalGeneratedImage> =
    runCatching {
      synchronized(nativeLock) {
        validateGenerationRequest(request)?.let { error(it) }
        check(modelDescriptor != null && loadedModelUri != null) {
          "Load a local image model before generating."
        }

        val expectedByteCount = request.width * request.height * 4
        val rgba =
          StableDiffusionBridge.txt2img(
            prompt = request.prompt.trim(),
            negativePrompt = request.negativePrompt?.trim()?.takeIf { it.isNotEmpty() },
            width = request.width,
            height = request.height,
            steps = request.steps,
            cfgScale = request.cfgScale,
            seed = request.seed,
          )
        check(rgba.size == expectedByteCount) {
          "Image generation returned no pixels. The checkpoint may be incompatible or the device " +
            "may have run out of memory."
        }

        val bitmap =
          Bitmap.createBitmap(
            rgbaToArgbPixels(rgba),
            request.width,
            request.height,
            Bitmap.Config.ARGB_8888,
          )
        try {
          val outputDirectory =
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir,
                OUTPUT_DIRECTORY,
              )
              .apply { check(exists() || mkdirs()) { "Could not create the image output folder." } }
          val outputFile = File(outputDirectory, "local_image_${System.currentTimeMillis()}.png")
          FileOutputStream(outputFile).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
              "Could not encode the generated image."
            }
          }
          LocalGeneratedImage(
            file = outputFile,
            width = request.width,
            height = request.height,
          )
        } finally {
          bitmap.recycle()
        }
      }
    }

  fun exportImage(source: File, destination: Uri): Result<Unit> =
    runCatching {
      check(source.isFile) { "The generated image is no longer available." }
      val output =
        context.contentResolver.openOutputStream(destination, "wt")
          ?: error("The selected destination could not be opened.")
      source.inputStream().use { input -> output.use { input.copyTo(it) } }
    }

  fun release() {
    synchronized(nativeLock) { releaseLocked() }
  }

  private fun releaseLocked() {
    if (modelDescriptor != null) {
      StableDiffusionBridge.release()
    }
    modelDescriptor?.close()
    modelDescriptor = null
    loadedModelUri = null
  }

  private fun queryModelInfo(uri: Uri): LocalImageModelInfo {
    var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Local image model"
    var sizeBytes = -1L
    context.contentResolver
      .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
      ?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
            displayName = cursor.getString(nameIndex)
          }
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
            sizeBytes = cursor.getLong(sizeIndex)
          }
        }
      }
    if (sizeBytes < 0) {
      context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
        sizeBytes = descriptor.statSize
      }
    }
    return LocalImageModelInfo(uri = uri, displayName = displayName, sizeBytes = sizeBytes)
  }

  private fun validateModelName(displayName: String) {
    val extension = displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    if (extension.isNotEmpty() && extension !in SUPPORTED_MODEL_EXTENSIONS) {
      error("Choose a .safetensors, .ckpt, or .gguf checkpoint.")
    }
  }
}
