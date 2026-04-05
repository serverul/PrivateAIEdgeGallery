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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hartagis.edgear.MainActivity
import com.hartagis.edgear.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val CHANNEL_ID = "inference_server_channel"
private const val NOTIFICATION_ID = 10001

@AndroidEntryPoint
class InferenceServerService : Service() {

  @Inject lateinit var serverManager: LocalServerManager

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_START -> {
        serverManager.init()
        updateNotification()
        startForegroundWithCompat()
      }
      ACTION_STOP -> {
        serverManager.stopServer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
      }
    }
    return START_STICKY
  }

  private fun startForegroundWithCompat() {
    val notification = buildNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun updateNotification() {
    val notification = buildNotification()
    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
  }

  private fun buildNotification(): Notification {
    val title = if (serverManager.isServerRunning) "AI Server Active" else "AI Server Starting..."
    val port = serverManager.currentPort

    val openIntent = Intent(this, MainActivity::class.java).let { i ->
      PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    val stopIntent = Intent(this, InferenceServerService::class.java).apply {
      action = ACTION_STOP
    }.let { i ->
      PendingIntent.getService(this, 1, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(title)
      .setContentText("Running on port $port")
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentIntent(openIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
      .setOngoing(true)
      .build()
  }

  private fun createNotificationChannel() {
    val channel = NotificationChannel(
      CHANNEL_ID,
      "Inference Server",
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = "Shows when local AI server is running"
      setShowBadge(false)
    }
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
  }

  override fun onDestroy() {
    super.onDestroy()
    serverManager.stopServer()
  }

  companion object {
    const val ACTION_START = "com.hartagis.edgear.server.START"
    const val ACTION_STOP = "com.hartagis.edgear.server.STOP"
  }
}
