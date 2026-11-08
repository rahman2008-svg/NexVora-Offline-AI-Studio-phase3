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

package com.nexvora.offlineai.ui

import com.nexvora.offlineai.ui.nexvora.hub.CompatLevel
import com.nexvora.offlineai.ui.nexvora.hub.ModelCompatibility
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelCompatibilityTest {
  private val gb = 1_000_000_000L

  @Test fun nominalGb_roundsUp_soSixGbPhoneCountsAsSix() {
    assertEquals(6, ModelCompatibility.nominalDeviceGb(5_600_000_000L))
    assertEquals(8, ModelCompatibility.nominalDeviceGb(7_400_000_000L))
    assertEquals(0, ModelCompatibility.nominalDeviceGb(0))
  }

  @Test fun enoughMemory_isOk() {
    val c = ModelCompatibility.evaluate(6, 5_600_000_000L, 2 * gb, 20 * gb, installed = false)
    assertEquals(CompatLevel.OK, c.level)
    assertEquals(6, c.requiredGb)
    assertEquals(6, c.deviceGb)
  }

  @Test fun tooLittleMemory_isLowMemory() {
    val c = ModelCompatibility.evaluate(8, 5_600_000_000L, 2 * gb, 20 * gb, installed = true)
    assertEquals(CompatLevel.LOW_MEMORY, c.level)
  }

  @Test fun notEnoughStorage_winsWhenNotInstalled() {
    val c = ModelCompatibility.evaluate(4, 8 * gb, 3 * gb, 1 * gb, installed = false)
    assertEquals(CompatLevel.NOT_ENOUGH_STORAGE, c.level)
  }

  @Test fun storageIgnoredWhenAlreadyInstalled() {
    val c = ModelCompatibility.evaluate(4, 8 * gb, 3 * gb, 0, installed = true)
    assertEquals(CompatLevel.OK, c.level)
  }

  @Test fun unknownRequirement_isUnknown() {
    val c = ModelCompatibility.evaluate(null, 8 * gb, 1 * gb, 10 * gb, installed = false)
    assertEquals(CompatLevel.UNKNOWN, c.level)
  }
}
