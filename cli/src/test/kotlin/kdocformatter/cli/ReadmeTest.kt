/*
 * Copyright (c) Tor Norbye.
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

package kdocformatter.cli

import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Keeps the README in sync with the CLI, including its version (from version.properties). */
class ReadmeTest {
  private val readme: String by lazy {
    // Gradle runs tests from the module directory; IDEs may use the repository root.
    val file = listOf(File("../README.md"), File("README.md")).first { it.isFile }
    file.readText()
  }

  @Test
  fun testUsageBlockMatchesCli() {
    val start = readme.indexOf("```\n\$ kdoc-formatter\n")
    assertTrue(start >= 0, "README has no usage block starting with \$ kdoc-formatter")
    val bodyStart = readme.indexOf('\n', readme.indexOf('\n', start) + 1) + 1
    val bodyEnd = readme.indexOf("```", bodyStart)
    assertEquals(
        KDocFileFormattingOptions.usage().trimEnd(),
        readme.substring(bodyStart, bodyEnd).trimEnd(),
        "Update the README's Command Usage block with the output of `kdoc-formatter`")
  }
}
