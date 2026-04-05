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

package com.hartagis.edgear.server

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.hartagis.edgear.data.DataStoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "LocalServerManager"

/** Status of the local inference server */
sealed class ServerStatus {
  object Stopped : ServerStatus()
  data class Running(val port: Int, val model: String) : ServerStatus()
  data class Error(val message: String) : ServerStatus()
}

/** Settings for the local inference server */
data class ServerSettings(
  val enabled: Boolean = false,
  val port: Int = 8080,
  val modelPath: String = "",
)

@HiltViewModel
class LocalServerManager @Inject constructor(
  @ApplicationContext private val context: Context,
  private val dataStoreRepository: DataStoreRepository,
) : ViewModel() {
  private var server: LocalInferenceServer? = null
  private var initialized = false

  private val _status = MutableStateFlow<ServerStatus>(ServerStatus.Stopped)
  val status: StateFlow<ServerStatus> = _status.asStateFlow()

  /** Current server settings */
  private var _settings = ServerSettings()
  val settings get() = _settings

  /** Initialize with saved settings */
  fun init() {
    if (initialized) return
    initialized = true
    _settings = ServerSettings(
      enabled = dataStoreRepository.getLocalServerEnabled(),
      port = dataStoreRepository.getLocalServerPort(),
      modelPath = dataStoreRepository.getLocalServerModelPath(),
    )

    if (_settings.enabled && _settings.modelPath.isNotEmpty()) {
      startServer()
    }
  }

  fun updateSettings(newSettings: ServerSettings) {
    _settings = newSettings
    dataStoreRepository.saveLocalServerSettings(
      enabled = newSettings.enabled,
      port = newSettings.port,
      modelPath = newSettings.modelPath,
    )

    if (newSettings.enabled) {
      if (server?.isRunning != true) {
        startServer()
      } else {
        val currentPort = server?.serverPort ?: 0
        if (currentPort != newSettings.port) {
          stopServer()
          startServer()
        }
      }
    } else {
      stopServer()
    }
  }

  private fun startServer() {
    if (server?.isRunning == true) return

    server = LocalInferenceServer(context, _settings.port)

    if (_settings.modelPath.isNotEmpty()) {
      val result = server!!.loadModel(_settings.modelPath)
      result.onSuccess {
        server!!.startServer()
        _status.value = ServerStatus.Running(_settings.port, _settings.modelPath)
        Log.i(TAG, "Server started on port ${_settings.port} with model ${_settings.modelPath}")
      }.onFailure { error ->
        _status.value = ServerStatus.Error("Failed to load model: ${error.message}")
        Log.e(TAG, "Failed to load model", error)
      }
    } else {
      server!!.startServer()
      _status.value = ServerStatus.Running(_settings.port, "No model loaded")
      Log.i(TAG, "Server started on port ${_settings.port} (no model)")
    }
  }

  fun stopServer() {
    server?.stopServer()
    server = null
    _status.value = ServerStatus.Stopped
    Log.i(TAG, "Server stopped")
  }

  override fun onCleared() {
    super.onCleared()
    stopServer()
  }
}
