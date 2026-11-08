/*
 * Copyright 2025 Google LLC
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

package com.nexvora.offlineai

import android.app.Application
import android.os.Process
import android.util.Log
import com.nexvora.offlineai.data.DataStoreRepository
import com.nexvora.offlineai.data.db.LegacyChatImporter
import com.nexvora.offlineai.notifications.NotificationScheduleManager
import com.nexvora.offlineai.services.tts.TtsMemoryTrimCallbacks
import com.nexvora.offlineai.ui.theme.ThemeSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class GalleryApplication : Application() {

  @Inject lateinit var dataStoreRepository: DataStoreRepository
  @Inject lateinit var notificationScheduleManager: NotificationScheduleManager
  @Inject lateinit var ttsMemoryTrimCallbacks: TtsMemoryTrimCallbacks
  @Inject lateinit var legacyChatImporter: LegacyChatImporter

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override fun onCreate() {
    // androidx.pdf parses PDFs in an isolated service process, which also instantiates this
    // Application. Isolated processes have no access to storage or most system services, so skip all app initialization in them.
    if (Process.isIsolated()) {
      return
    }
    super.onCreate()
    registerComponentCallbacks(ttsMemoryTrimCallbacks)
    // Initialize the notification schedule manager to load the scheduled notifications from the
    // disk.
    notificationScheduleManager.initialize()

    // Load saved theme.
    ThemeSettings.themeOverride.value = dataStoreRepository.readTheme()

    // Move chat history of older builds into the Room database (runs once).
    appScope.launch {
      try {
        legacyChatImporter.importIfNeeded()
      } catch (e: Exception) {
        Log.w("GalleryApplication", "Legacy chat import failed: ${e.message}", e)
      }
    }
  }
}
