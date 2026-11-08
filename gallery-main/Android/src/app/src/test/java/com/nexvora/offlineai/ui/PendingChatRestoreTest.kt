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

import com.nexvora.offlineai.ui.nexvora.PendingChatRestore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingChatRestoreTest {
  @After fun reset() = PendingChatRestore.clear()

  @Test fun request_isReturnedUntilCleared() {
    PendingChatRestore.request("abc", now = 1_000L)
    assertEquals("abc", PendingChatRestore.peek(now = 2_000L))
    PendingChatRestore.clear()
    assertNull(PendingChatRestore.peek(now = 2_000L))
  }

  @Test fun request_expiresAfterTtl() {
    PendingChatRestore.request("abc", now = 1_000L)
    assertEquals("abc", PendingChatRestore.peek(now = 1_000L + PendingChatRestore.TTL_MS))
    assertNull(PendingChatRestore.peek(now = 1_001L + PendingChatRestore.TTL_MS))
  }
}
