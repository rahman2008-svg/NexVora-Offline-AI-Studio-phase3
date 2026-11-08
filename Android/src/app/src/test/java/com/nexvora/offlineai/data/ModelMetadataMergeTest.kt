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

package com.nexvora.offlineai.data

import com.nexvora.offlineai.data.db.ModelMetadataEntity
import com.nexvora.offlineai.data.db.ModelMetadataRepository
import com.nexvora.offlineai.data.db.ModelSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelMetadataMergeTest {
  private fun snap(name: String, installed: Boolean, size: Long = 100) =
    ModelSnapshot(name, "Display $name", size, "LITERT_LM", 6, installed)

  @Test fun newModel_getsFirstSeenAndLastSeenNow() {
    val out = ModelMetadataRepository.merge(listOf(snap("a", true)), emptyMap(), now = 50L)
    assertEquals(1, out.size)
    assertEquals(50L, out[0].firstSeenAt)
    assertEquals(50L, out[0].lastSeenAt)
    assertNull(out[0].lastUsedAt)
    assertEquals(true, out[0].isInstalled)
  }

  @Test fun existingModel_keepsFirstSeenAndLastUsed_andRefreshesFacts() {
    val old = ModelMetadataEntity("a", "Old", 10, "X", null, false, firstSeenAt = 1L, lastSeenAt = 2L, lastUsedAt = 3L)
    val out = ModelMetadataRepository.merge(listOf(snap("a", installed = true, size = 999)), mapOf("a" to old), now = 77L)
    assertEquals(1L, out[0].firstSeenAt)
    assertEquals(77L, out[0].lastSeenAt)
    assertEquals(3L, out[0].lastUsedAt)
    assertEquals(999L, out[0].sizeBytes)
    assertEquals("Display a", out[0].displayName)
    assertEquals(true, out[0].isInstalled)
  }
}
