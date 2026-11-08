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

package com.nexvora.offlineai.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.nexvora.offlineai.AppLifecycleProvider
import com.nexvora.offlineai.BenchmarkResultsSerializer
import com.nexvora.offlineai.BuildConfig
import com.nexvora.offlineai.CutoutsSerializer
import com.nexvora.offlineai.GalleryLifecycleProvider
import com.nexvora.offlineai.SettingsSerializer
import com.nexvora.offlineai.SkillsSerializer
import com.nexvora.offlineai.UserDataSerializer
import com.nexvora.offlineai.data.ChatSessionRepository
import com.nexvora.offlineai.data.DataStoreRepository
import com.nexvora.offlineai.data.DefaultDataStoreRepository
import com.nexvora.offlineai.data.DefaultDownloadRepository
import com.nexvora.offlineai.data.DefaultNetworkStateProvider
import com.nexvora.offlineai.data.DownloadRepository
import com.nexvora.offlineai.data.LiteRtLmModelDownloader
import com.nexvora.offlineai.data.ModelDownloader
import com.nexvora.offlineai.data.NetworkStateProvider
import com.nexvora.offlineai.data.RuntimeType
import com.nexvora.offlineai.data.RuntimeTypeKey
import com.nexvora.offlineai.data.db.RoomChatSessionRepository
import com.nexvora.offlineai.proto.BenchmarkResults
import com.nexvora.offlineai.proto.CutoutCollection
import com.nexvora.offlineai.proto.Settings
import com.nexvora.offlineai.proto.Skills
import com.nexvora.offlineai.proto.UserData
import com.nexvora.offlineai.runtime.aicore.AICoreModelDownloader
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {

  // Provides the SettingsSerializer
  @Provides
  @Singleton
  fun provideSettingsSerializer(): Serializer<Settings> {
    return SettingsSerializer
  }

  // Provides the CutoutSerializer
  @Provides
  @Singleton
  fun provideCutoutSerializer(): Serializer<CutoutCollection> {
    return CutoutsSerializer
  }

  // Provides the UserDataSerializer
  @Provides
  @Singleton
  fun provideUserDataSerializer(): Serializer<UserData> {
    return UserDataSerializer
  }

  // Provides the BenchmarkResultsSerializer
  @Provides
  @Singleton
  fun provideBenchmarkResultsSerializer(): Serializer<BenchmarkResults> {
    return BenchmarkResultsSerializer
  }

  // Provides the SkillsSerializer
  @Provides
  @Singleton
  fun provideSkillsSerializer(): Serializer<Skills> {
    return SkillsSerializer
  }

  // Provides DataStore<Settings>
  @Provides
  @Singleton
  fun provideSettingsDataStore(
    @ApplicationContext context: Context,
    settingsSerializer: Serializer<Settings>,
  ): DataStore<Settings> {
    return DataStoreFactory.create(
      serializer = settingsSerializer,
      produceFile = { context.dataStoreFile("settings.pb") },
    )
  }

  // Provides DataStore<CutoutCollection>
  @Provides
  @Singleton
  fun provideCutoutsDataStore(
    @ApplicationContext context: Context,
    cutoutsSerializer: Serializer<CutoutCollection>,
  ): DataStore<CutoutCollection> {
    return DataStoreFactory.create(
      serializer = cutoutsSerializer,
      produceFile = { context.dataStoreFile("cutouts.pb") },
    )
  }

  // Provides DataStore<UserData>
  @Provides
  @Singleton
  fun provideUserDataDataStore(
    @ApplicationContext context: Context,
    userDataSerializer: Serializer<UserData>,
  ): DataStore<UserData> {
    return DataStoreFactory.create(
      serializer = userDataSerializer,
      produceFile = { context.dataStoreFile("user_data.pb") },
    )
  }

  // Provides DataStore<BenchmarkResults>
  @Provides
  @Singleton
  fun provideBenchmarkResultsDataStore(
    @ApplicationContext context: Context,
    benchmarkResultsSerializer: Serializer<BenchmarkResults>,
  ): DataStore<BenchmarkResults> {
    return DataStoreFactory.create(
      serializer = benchmarkResultsSerializer,
      produceFile = { context.dataStoreFile("benchmark_results.pb") },
    )
  }

  // Provides DataStore<Skills>
  @Provides
  @Singleton
  fun provideSkillsDataStore(
    @ApplicationContext context: Context,
    skillsSerializer: Serializer<Skills>,
  ): DataStore<Skills> {
    return DataStoreFactory.create(
      serializer = skillsSerializer,
      produceFile = { context.dataStoreFile("skills.pb") },
    )
  }

  // Provides AppLifecycleProvider
  @Provides
  @Singleton
  fun provideAppLifecycleProvider(): AppLifecycleProvider {
    return GalleryLifecycleProvider()
  }

  // Provides DataStoreRepository
  @Provides
  @Singleton
  fun provideDataStoreRepository(
    dataStore: DataStore<Settings>,
    userDataDataStore: DataStore<UserData>,
    cutoutsDataStore: DataStore<CutoutCollection>,
    benchmarkResultsStore: DataStore<BenchmarkResults>,
    skillsDataStore: DataStore<Skills>,
  ): DataStoreRepository {
    return DefaultDataStoreRepository(
      dataStore,
      userDataDataStore,
      cutoutsDataStore,
      benchmarkResultsStore,
      skillsDataStore,
    )
  }

  // Provides ChatSessionRepository. NexVora stores chat history in Room (see data/db).
  @Provides
  @Singleton
  fun provideChatSessionRepository(impl: RoomChatSessionRepository): ChatSessionRepository = impl

  // Provides DownloadRepository
  @Provides
  @Singleton
  fun provideDownloadRepository(
    @ApplicationContext context: Context,
    lifecycleProvider: AppLifecycleProvider,
  ): DownloadRepository {
    return DefaultDownloadRepository(context, lifecycleProvider)
  }

  @Provides
  @Singleton
  fun provideNetworkStateProvider(@ApplicationContext context: Context): NetworkStateProvider {
    return DefaultNetworkStateProvider(context)
  }

  @Provides
  @Singleton
  @IntoMap
  @RuntimeTypeKey(RuntimeType.UNKNOWN)
  fun provideUnknownModelDownloader(
    @ApplicationContext context: Context,
    downloadRepository: DownloadRepository,
  ): ModelDownloader {
    return LiteRtLmModelDownloader(context, downloadRepository)
  }

  @Provides
  @Singleton
  @IntoMap
  @RuntimeTypeKey(RuntimeType.LITERT_LM)
  fun provideLiteRtLmModelDownloader(
    @ApplicationContext context: Context,
    downloadRepository: DownloadRepository,
  ): ModelDownloader {
    return LiteRtLmModelDownloader(context, downloadRepository)
  }

  @Provides
  @Singleton
  @IntoMap
  @RuntimeTypeKey(RuntimeType.AICORE)
  fun provideAICoreModelDownloader(@ApplicationContext context: Context): ModelDownloader {
    return AICoreModelDownloader(context)
  }

  @Provides
  @Singleton
  fun provideMoshi(): Moshi {
    return Moshi.Builder().build()
  }
}
