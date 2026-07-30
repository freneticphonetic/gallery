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

import com.google.ai.edge.gallery.data.BuiltInTaskId
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenTest {
  @Test
  fun chatAdditionsAndPromptLabInputsArePrefilled() {
    val taskIds =
      listOf(
        BuiltInTaskId.LLM_ASK_IMAGE,
        BuiltInTaskId.LLM_ASK_AUDIO,
        BuiltInTaskId.LLM_AGENT_CHAT,
        BuiltInTaskId.LLM_PROMPT_LAB,
        BuiltInTaskId.LOCAL_IMAGE_GENERATOR,
      )

    taskIds.forEach { taskId ->
      assertEquals(HomePromptDelivery.PREFILL, homePromptDelivery(taskId))
    }
  }

  @Test
  fun conversationalAndActionInputsAreSubmitted() {
    val taskIds =
      listOf(
        BuiltInTaskId.LLM_CHAT,
        BuiltInTaskId.LLM_MOBILE_ACTIONS,
        BuiltInTaskId.LLM_TINY_GARDEN,
      )

    taskIds.forEach { taskId ->
      assertEquals(HomePromptDelivery.SUBMIT, homePromptDelivery(taskId))
    }
  }
}
