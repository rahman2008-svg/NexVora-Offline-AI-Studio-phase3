/*
 * Copyright 2026 NexVora Lab's Ofc
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

package com.nexvora.offlineai.data.db

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Snapshot of one model used to refresh the stored metadata. */
data class ModelSnapshot(
  val name: String,
  val displayName: String,
  val sizeBytes: Long,
  val runtime: String,
  val minDeviceMemoryGb: Int?,
  val isInstalled: Boolean,
)

/** Remembers model facts (installed state, size, last use) in Room. */
@Singleton
class ModelMetadataRepository @Inject constructor(private val dao: ModelMetadataDao) {
  fun observeAll(): Flow<List<ModelMetadataEntity>> = dao.observeAll()

  suspend fun getAll(): List<ModelMetadataEntity> = dao.getAll()

  /** Inserts new models and refreshes existing ones, keeping `firstSeenAt` and `lastUsedAt`. */
  suspend fun sync(snapshots: List<ModelSnapshot>, now: Long = System.currentTimeMillis()) {
    val existing = dao.getAll().associateBy { it.name }
    dao.upsertAll(merge(snapshots, existing, now))
  }

  suspend fun markUsed(name: String, now: Long = System.currentTimeMillis()) {
    dao.markUsed(name, now)
  }

  suspend fun clearAll() {
    dao.deleteAll()
  }

  companion object {
    /** Pure merge logic, unit-tested. */
    fun merge(
      snapshots: List<ModelSnapshot>,
      existing: Map<String, ModelMetadataEntity>,
      now: Long,
    ): List<ModelMetadataEntity> =
      snapshots.map { s ->
        val old = existing[s.name]
        ModelMetadataEntity(
          name = s.name,
          displayName = s.displayName,
          sizeBytes = s.sizeBytes,
          runtime = s.runtime,
          minDeviceMemoryGb = s.minDeviceMemoryGb,
          isInstalled = s.isInstalled,
          firstSeenAt = old?.firstSeenAt ?: now,
          lastSeenAt = now,
          lastUsedAt = old?.lastUsedAt,
        )
      }
  }
}
