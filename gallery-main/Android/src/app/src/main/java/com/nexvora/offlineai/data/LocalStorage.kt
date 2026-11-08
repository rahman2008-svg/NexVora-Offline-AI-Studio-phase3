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

import java.io.File
import java.util.Locale

/** Small pure helpers for showing and measuring local storage. */
object LocalStorage {
  /** Formats a byte count for display, e.g. `1.5 GB`. Negative values are shown as `0 B`. */
  fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.size - 1) {
      value /= 1024.0
      unit++
    }
    return if (unit == 0) "${bytes} B" else String.format(Locale.US, "%.1f %s", value, units[unit])
  }

  /** Total size of all files under [file] (or the file itself). Missing paths count as 0. */
  fun sizeOf(file: File): Long {
    if (!file.exists()) return 0L
    if (file.isFile) return file.length()
    return file.listFiles()?.sumOf { sizeOf(it) } ?: 0L
  }

  /** Deletes everything inside [dir] but keeps [dir] itself. Returns the number of bytes freed. */
  fun clearDirectory(dir: File): Long {
    val before = sizeOf(dir)
    dir.listFiles()?.forEach { it.deleteRecursively() }
    return (before - sizeOf(dir)).coerceAtLeast(0L)
  }
}
