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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalStorageTest {
  @get:Rule val tmp = TemporaryFolder()

  @Test fun formatBytes_commonValues() {
    assertEquals("0 B", LocalStorage.formatBytes(0))
    assertEquals("0 B", LocalStorage.formatBytes(-5))
    assertEquals("512 B", LocalStorage.formatBytes(512))
    assertEquals("1.0 KB", LocalStorage.formatBytes(1024))
    assertEquals("1.5 MB", LocalStorage.formatBytes((1.5 * 1024 * 1024).toLong()))
    assertEquals("2.0 GB", LocalStorage.formatBytes(2L * 1024 * 1024 * 1024))
  }

  @Test fun sizeOf_missingPath_isZero() {
    assertEquals(0L, LocalStorage.sizeOf(File(tmp.root, "nope")))
  }

  @Test fun sizeOf_sumsNestedFiles() {
    File(tmp.root, "a.bin").writeBytes(ByteArray(100))
    val sub = tmp.newFolder("sub")
    File(sub, "b.bin").writeBytes(ByteArray(50))
    assertEquals(150L, LocalStorage.sizeOf(tmp.root))
  }

  @Test fun clearDirectory_removesContentsButKeepsFolder() {
    File(tmp.root, "a.bin").writeBytes(ByteArray(100))
    File(tmp.newFolder("sub"), "b.bin").writeBytes(ByteArray(50))
    val freed = LocalStorage.clearDirectory(tmp.root)
    assertEquals(150L, freed)
    assertTrue(tmp.root.exists())
    assertEquals(0, tmp.root.listFiles()!!.size)
  }
}
