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

import com.nexvora.offlineai.data.db.SearchQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchQueryTest {
  @Test fun blank_returnsNull() {
    assertNull(SearchQuery.toLikePattern(""))
    assertNull(SearchQuery.toLikePattern("   "))
  }

  @Test fun plain_isWrappedAndTrimmed() {
    assertEquals("%hello%", SearchQuery.toLikePattern("  hello "))
  }

  @Test fun wildcards_areEscaped() {
    assertEquals("%100\\%%", SearchQuery.toLikePattern("100%"))
    assertEquals("%a\\_b%", SearchQuery.toLikePattern("a_b"))
    assertEquals("%c\\\\d%", SearchQuery.toLikePattern("c\\d"))
  }

  @Test fun banglaText_isKept() {
    assertEquals("%বাংলা%", SearchQuery.toLikePattern("বাংলা"))
  }
}
