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

package com.nexvora.offlineai.ui.nexvora.hub

import kotlin.math.ceil

enum class CompatLevel {
  /** Memory and storage look sufficient. */
  OK,
  /** The model needs more RAM than this device has. It may fail to load. */
  LOW_MEMORY,
  /** Not enough free storage to download the model. */
  NOT_ENOUGH_STORAGE,
  /** The model does not declare a RAM requirement. */
  UNKNOWN,
}

data class Compatibility(val level: CompatLevel, val requiredGb: Int?, val deviceGb: Int)

/** Estimates whether a model can run on this device. It is a hint, never a guarantee. */
object ModelCompatibility {
  /** A phone sold as "6 GB" reports about 5.5 GB of usable RAM, so round decimal GB up. */
  fun nominalDeviceGb(totalMemBytes: Long): Int =
    if (totalMemBytes <= 0L) 0 else ceil(totalMemBytes / 1_000_000_000.0).toInt()

  fun evaluate(
    minDeviceMemoryGb: Int?,
    deviceTotalMemBytes: Long,
    modelBytes: Long,
    freeStorageBytes: Long,
    installed: Boolean,
  ): Compatibility {
    val deviceGb = nominalDeviceGb(deviceTotalMemBytes)
    if (!installed && modelBytes > 0 && freeStorageBytes in 0 until modelBytes) {
      return Compatibility(CompatLevel.NOT_ENOUGH_STORAGE, minDeviceMemoryGb, deviceGb)
    }
    if (minDeviceMemoryGb == null) {
      return Compatibility(CompatLevel.UNKNOWN, null, deviceGb)
    }
    val level = if (deviceGb >= minDeviceMemoryGb) CompatLevel.OK else CompatLevel.LOW_MEMORY
    return Compatibility(level, minDeviceMemoryGb, deviceGb)
  }
}
