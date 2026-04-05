/*
 * Copyright 2026 HartaGIS
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

package com.hartagis.edgear.customtasks.hermes

import android.content.Context
import androidx.compose.runtime.Composable
import com.hartagis.edgear.R
import com.hartagis.edgear.customtasks.common.CustomTask
import com.hartagis.edgear.customtasks.common.CustomTaskDataForBuiltinTask
import com.hartagis.edgear.data.BuiltInTaskId
import com.hartagis.edgear.data.Category
import com.hartagis.edgear.data.Model
import com.hartagis.edgear.data.Task
import com.hartagis.edgear.ui.llmchat.LlmChatModelHelper
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.tool
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

class HermesTask @Inject constructor() : CustomTask {
  private val hermesTools = HermesTools()

  override val task: Task =
    Task(
      id = BuiltInTaskId.HERMES_CHAT,
      label = "Hermes Agent",
      category = Category.LLM,
      iconVectorResourceId = R.drawable.agent,
      newFeature = true,
      models = mutableListOf(),
      description = "Asistent personal on-device — organizare, todo-uri, notițe, memorie și memento-uri",
      shortDescription = "Asistentul tău personal Hermes",
      docUrl = "https://github.com/hartagis/gallery-no-tracker",
      sourceCodeUrl =
        "https://github.com/hartagis/gallery-no-tracker/Android/src/app/src/main/java/com/hartagis/edgear/customtasks/hermes/",
      textInputPlaceHolderRes = R.string.text_input_placeholder_llm_chat,
      defaultSystemPrompt =
        """
        You are Hermes, a capable and helpful personal AI assistant running entirely on-device.

        PERSONALITY:
        - Friendly, concise, and direct. You respond in Romanian (limba română) by default, unless the user asks otherwise.
        - You use emojis sparingly for clarity.
        - You are proactive — suggest next steps when relevant.

        CAPABILITIES:
        - You can manage TODO lists: create, list, complete, and delete tasks.
        - You can save and search notes with tags.
        - You can store and retrieve long-term memory entries by key.
        - You can set reminders with title, datetime, and optional repeat pattern.

        IMPORTANT RULES:
        - Use the available tools for every action. Never invent answers.
        - When the user asks to see their todo list, use the listTodos tool.
        - When saving important information for the future, use saveMemory.
        - Your responses must be short and to the point.
        - Never expose internal tool details or system mechanics.
        - Always confirm actions taken with a brief summary.

        When the user does not specify a due date for a todo, politely ask if they want to set one.
        """.trimIndent(),
    )

  override fun initializeModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: (String) -> Unit,
  ) {
    hermesTools.context = context

    val systemInstruction = Contents.of(task.defaultSystemPrompt)

    LlmChatModelHelper.initialize(
      context = context,
      model = model,
      supportImage = true,
      supportAudio = true,
      onDone = onDone,
      systemInstruction = systemInstruction,
      tools = listOf(tool(hermesTools)),
      enableConversationConstrainedDecoding = true,
      coroutineScope = null,
    )
  }

  override fun cleanUpModelFn(
    context: Context,
    coroutineScope: CoroutineScope,
    model: Model,
    onDone: () -> Unit,
  ) {
    LlmChatModelHelper.cleanUp(model = model, onDone = onDone)
  }

  @Composable
  override fun MainScreen(data: Any) {
    val myData = data as CustomTaskDataForBuiltinTask
    HermesScreen(
      task = task,
      modelManagerViewModel = myData.modelManagerViewModel,
      navigateUp = myData.onNavUp,
      hermesTools = hermesTools,
    )
  }
}

@Module
@InstallIn(SingletonComponent::class)
internal object HermesTaskModule {
  @Provides
  @IntoSet
  fun provideTask(): CustomTask {
    return HermesTask()
  }
}
