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
import kdocformatter.KDocFormattingOptions
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class EditorConfigsTest {
  companion object {
    @TempDir @JvmField var temporaryFolder: File? = null
  }

  class ConfigFile(val relativePath: String, @Language("EditorConfig") val contents: String)

  private fun createFileTree(vararg files: ConfigFile): File {
    val root = temporaryFolder!!
    root.deleteRecursively()
    for (file in files) {
      val target = File(root, file.relativePath)
      target.parentFile?.mkdirs()
      target.writeText(file.contents)
    }
    return root
  }

  @Test
  fun testBasics() {
    EditorConfigs.root = null
    val fileTree =
        createFileTree(
            ConfigFile(
                "root/.editorconfig",
                ";comment\nroot = true\n[*]\nmax_line_length=150\ntab_width = 10"),
            ConfigFile(
                "root/sub1/sub2/.editorconfig",
                "[*]\nindent_size = 6\n[*.md]\nmax_line_length = 40\n[*.kt]\nmax_line_length = 60"),
            ConfigFile(
                "root/sub1/sub3/.editorconfig",
                "[*]\nindent_size = 6\n[{*.java,*.kt}]\nmax_line_length=120\n[*.md]\nmax_line_length = 80\n; max_line_length = 110"))
    val file1 = File(fileTree, "root/sub1/sub2/sub3/sub4/foo.kt")
    val options1 = EditorConfigs.getOptions(file1)
    assertEquals(60, options1.maxLineWidth)
    assertEquals(40, options1.maxCommentWidth)

    val file2 = File(fileTree, "root/sub1/sub2/foo.kt")
    val options2 = EditorConfigs.getOptions(file2)
    assertEquals(60, options2.maxLineWidth)
    assertEquals(40, options2.maxCommentWidth)

    val file3 = File(fileTree, "root/sub1/foo.kt")
    val options3 = EditorConfigs.getOptions(file3)
    assertEquals(150, options3.maxLineWidth)
    assertEquals(KDocFormattingOptions().maxCommentWidth, options3.maxCommentWidth)

    val file4 = File(fileTree, "root/sub1/sub3/foo.kt")
    val options4 = EditorConfigs.getOptions(file4)
    assertEquals(120, options4.maxLineWidth)
    assertEquals(80, options4.maxCommentWidth)
    assertEquals(6, options4.hangingIndent)
  }

  @Test
  fun testFallback() {
    val fallback = KDocFormattingOptions()
    fallback.maxLineWidth = 80
    fallback.maxCommentWidth = 50
    EditorConfigs.root = fallback

    val fileTree =
        createFileTree(
            ConfigFile(
                "root/.editorconfig", "root = true\n[*]\nmax_line_length=150\ntab_width = 10"))
    val file1 = File(fileTree, "root/sub1/sub2/sub3/sub4/foo.kt")
    val options1 = EditorConfigs.getOptions(file1)
    assertEquals(150, options1.maxLineWidth)
    assertEquals(50, options1.maxCommentWidth)
  }

  @Test
  fun testUnset() {
    val fallback = KDocFormattingOptions()
    fallback.maxLineWidth = 80
    fallback.maxCommentWidth = 50
    EditorConfigs.root = fallback

    val fileTree =
        createFileTree(
            ConfigFile(
                "root/.editorconfig", "root = true\n[*]\nmax_line_length=150\ntab_width = 10"),
            ConfigFile(
                "root/sub1/sub2/.editorconfig",
                "[*]\nindent_size = 6\n[*.kt]\nmax_line_length = unset"))
    val file = File(fileTree, "root/sub1/sub2/sub3/sub4/foo.kt")
    val options = EditorConfigs.getOptions(file)
    assertEquals(
        80, options.maxLineWidth) // go to fallback since set to unset in closest editor config
  }

  @Test
  fun testStopAtRoot() {
    val fallback = KDocFormattingOptions()
    fallback.maxLineWidth = 80
    fallback.maxCommentWidth = 50
    EditorConfigs.root = fallback

    val fileTree =
        createFileTree(
            ConfigFile(
                "root/.editorconfig", "root = true\n[*]\nmax_line_length=150\ntab_width = 10"),
            ConfigFile("root/sub1/sub2/.editorconfig", "root = true\n[*]\nindent_size = 6\n"))
    val file = File(fileTree, "root/sub1/sub2/sub3/sub4/foo.kt")
    val options = EditorConfigs.getOptions(file)
    assertEquals(80, options.maxLineWidth) // go to fallback since stops at local root
  }
}
