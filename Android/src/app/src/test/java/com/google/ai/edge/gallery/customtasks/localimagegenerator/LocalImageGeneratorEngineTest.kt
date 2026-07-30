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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalImageGeneratorEngineTest {
  @Test
  fun validateGenerationRequest_acceptsBoundedRequest() {
    assertNull(validateGenerationRequest(validRequest()))
  }

  @Test
  fun validateGenerationRequest_rejectsBlankPrompt() {
    assertEquals(
      "Enter a prompt first.",
      validateGenerationRequest(validRequest().copy(prompt = "  ")),
    )
  }

  @Test
  fun validateGenerationRequest_rejectsNonAlignedCanvas() {
    assertEquals(
      "Image dimensions must be multiples of 64.",
      validateGenerationRequest(validRequest().copy(width = 500)),
    )
  }

  @Test
  fun rgbaToArgbPixels_preservesChannels() {
    val pixels =
      rgbaToArgbPixels(
        byteArrayOf(
          0x11,
          0x22,
          0x33,
          0x44,
          0xff.toByte(),
          0x80.toByte(),
          0x00,
          0xff.toByte(),
        )
      )

    assertArrayEquals(intArrayOf(0x44112233, 0xffff8000.toInt()), pixels)
  }

  private fun validRequest() =
    LocalImageGenerationRequest(
      prompt = "A film still",
      negativePrompt = null,
      width = 512,
      height = 512,
      steps = 12,
      cfgScale = 7f,
      seed = -1L,
    )
}
