/*
 * Copyright (C) 2022 Tor Norbye
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kdocformatter.cli

import java.io.File

class UnionFilter(private val filters: List<RangeFilter>) : RangeFilter() {
  override fun overlaps(file: File, source: String, startOffset: Int, endOffset: Int): Boolean {
    for (filter in filters) {
      if (filter.overlaps(file, source, startOffset, endOffset)) {
        return true
      }
    }
    return false
  }

  override fun includes(file: File): Boolean {
    for (filter in filters) {
      if (filter.includes(file)) {
        return true
      }
    }
    return false
  }

  override fun isEmpty(): Boolean {
    for (filter in filters) {
      if (!filter.isEmpty()) {
        return false
      }
    }
    return true
  }
}
