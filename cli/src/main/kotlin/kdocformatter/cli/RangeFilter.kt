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

/** Filter to decide whether given text regions should be included. */
open class RangeFilter {
  /**
   * Return true if the range in [file] containing the contents [source]
   * overlaps the range from [startOffset] inclusive to [endOffset]
   * exclusive.
   */
  open fun overlaps(file: File, source: String, startOffset: Int, endOffset: Int): Boolean = true

  /**
   * Returns true if the given file might include ranges that can return
   * true from [overlaps].
   */
  open fun includes(file: File) = true

  /**
   * Returns true if this filter is completely empty so nothing will
   * match.
   */
  open fun isEmpty(): Boolean = false
}
