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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hartagis.edgear.data.BuiltInTaskId
import com.hartagis.edgear.data.Task
import com.hartagis.edgear.ui.common.chat.ChatMessageText
import com.hartagis.edgear.ui.common.chat.ChatSide
import com.hartagis.edgear.ui.common.chat.SendMessageTrigger
import com.hartagis.edgear.ui.llmchat.LlmChatScreen
import com.hartagis.edgear.ui.modelmanager.ModelInitializationStatusType
import com.hartagis.edgear.ui.modelmanager.ModelManagerViewModel

/** Quick-prompt chips for the Hermes empty state. */
private val HERMES_CHIPS = listOf(
  Triple("📋", "Todo-uri", "Arată-mi lista mea de todo-uri"),
  Triple("➕", "Todo nou", "Creează un todo: cumpără pâine"),
  Triple("📝", "Notă nouă", "Salvează o notă: idee pentru proiectul nou"),
  Triple("🧠", "Memorie", "Salvează în memorie: preferințele mele de lucru"),
  Triple("⏰", "Reminder", "Setează un reminder: meeting mâine la 10 dimineața"),
)

@Composable
fun HermesScreen(
  task: Task,
  modelManagerViewModel: ModelManagerViewModel,
  navigateUp: () -> Unit,
  hermesTools: HermesTools,
) {
  val context = LocalContext.current
  hermesTools.context = context

  var sendMessageTrigger by remember { mutableStateOf<SendMessageTrigger?>(null) }

  LlmChatScreen(
    modelManagerViewModel = modelManagerViewModel,
    navigateUp = navigateUp,
    taskId = BuiltInTaskId.HERMES_CHAT,
    onFirstToken = { },
    onGenerateResponseDone = {
      // Clean up any tool results (images, webviews)
      hermesTools.resultImageToShow = null
      hermesTools.resultWebviewToShow = null
    },
    onSkillClicked = {
      // Hermes has built-in tools, no separate skill manager needed
    },
    showImagePicker = true,
    showAudioPicker = true,
    allowEditingSystemPrompt = true,
    emptyStateComposable = { model ->
      val modelManagerUiState by modelManagerViewModel.uiState.collectAsState()
      val modelInitializationStatus = modelManagerUiState.modelInitializationStatus[model.name]
      val isReady = modelInitializationStatus?.status == ModelInitializationStatusType.INITIALIZED

      Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
          true,
          enter = fadeIn(animationSpec = tween(200)),
          exit = fadeOut(animationSpec = tween(200)),
        ) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
              modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp)
                .padding(bottom = 48.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Text(
                "Bună!",
                style = MaterialTheme.typography.headlineSmall,
              )
              Text(
                "Sunt Hermes 🤖",
                style = MaterialTheme.typography.headlineLarge.copy(
                  fontWeight = FontWeight.Medium,
                  brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF85B1F8), Color(0xFF3174F1)),
                  ),
                ),
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
              )
              Text(
                "Asistentul tău personal AI care rulează 100% pe telefon, fără internet.\n\n" +
                  "Pot să te ajut cu:\n" +
                  "📋 Lista de todo-uri\n" +
                  "📝 Notițe și note rapide\n" +
                  "🧠 Memorie pe termen lung\n" +
                  "⏰ Memento-uri și programări",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }
          }
        }

        // Quick-prompt chips at bottom
        Row(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          for ((icon, label, prompt) in HERMES_CHIPS) {
            FilledTonalButton(
              enabled = isReady,
              onClick = {
                sendMessageTrigger = SendMessageTrigger(
                  model = model,
                  messages = listOf(ChatMessageText(content = prompt, side = ChatSide.USER)),
                )
              },
              contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
              Text(icon, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(label)
            }
          }
        }
      }
    },
    sendMessageTrigger = sendMessageTrigger,
  )
}
