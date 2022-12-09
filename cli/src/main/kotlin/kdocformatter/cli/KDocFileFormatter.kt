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
import kdocformatter.CommentType
import kdocformatter.FormattingTask
import kdocformatter.KDocFormatter
import kdocformatter.KDocFormattingOptions
import kdocformatter.computeIndents
import kdocformatter.getIndentSize
import kdocformatter.isBlockComment
import kotlin.math.min

/**
 * This class attempts to iterate over an entire Kotlin source file
 * and reformat all the KDocs it finds within. This is based on some
 * light-weight lexical analysis to identify comments.
 */
class KDocFileFormatter(private val options: KDocFileFormattingOptions) {
  init {
    EditorConfigs.root = options.formattingOptions
  }

  /** Formats the given file or directory recursively. */
  fun formatFile(file: File): Int {
    if (file.isDirectory) {
      val name = file.name
      if (name.startsWith(".") && name != "." && name != "../") {
        // Skip .git and friends
        return 0
      }
      val files = file.listFiles() ?: return 0
      var count = 0
      for (f in files) {
        count += formatFile(f)
      }
      return count
    }

    val formatter = file.getFormatter() ?: return 0
    if (file.isFile && options.filter.includes(file)) {
      val original = file.readText()
      val reformatted = reformat(file, original, formatter)
      if (reformatted != original) {
        if (options.dryRun) {
          println(file.path)
        } else {
          if (options.verbose) {
            println(file.path)
          }
          file.writeText(reformatted)
        }
        return 1
      }
    }
    return 0
  }

  private fun File.getFormatter(): ((String, KDocFormattingOptions, File?) -> String)? {
    return if (path.endsWith(".kt")) {
      ::reformatKotlinFile
    } else if (options.includeMd && (path.endsWith(".md") || path.endsWith(".md.html"))) {
      ::reformatMarkdownFile
    } else {
      null
    }
  }

  fun reformatFile(file: File, source: String): String {
    return reformat(file, source, file.getFormatter())
  }

  fun reformatSource(source: String, extension: String): String {
    val name = "path${if (extension.startsWith('.')) "" else "."}$extension"
    val file = File(name)
    return reformat(file, source, file.getFormatter())
  }

  private fun reformat(
      file: File?,
      source: String,
      reformatter: ((String, KDocFormattingOptions, File?) -> String)? = file?.getFormatter()
  ): String {
    reformatter ?: return source
    val formattingOptions =
        (file?.let { EditorConfigs.getOptions(it) } ?: options.formattingOptions).copy()
    // Override editor config with any options explicitly passed on the command
    // line
    options.overrideOptions(formattingOptions)
    return reformatter(source, formattingOptions, file)
  }

  /** Reformats the given Markdown file. */
  private fun reformatMarkdownFile(
      source: String,
      formattingOptions: KDocFormattingOptions,
      // Here such that this function has signature (String, KDocFormattingOptions, File?)
      @Suppress("UNUSED_PARAMETER") unused: File? = null
  ): String {
    // Just leverage the comment machinery here -- convert the markdown into
    // a kdoc comment, reformat that, and then uncomment it. Note that this
    // adds 3 characters to the line requirements (" * " on each line after the
    // opening "/**") so we duplicate the options and pre-add 3 to account for
    // this
    val formatter = KDocFormatter(formattingOptions.copy().apply { maxLineWidth += 3 })
    val comment = "/**\n" + source.split("\n").joinToString(separator = "\n") { " * $it" } + "\n*/"
    val reformattedComment =
        " " +
            formatter
                .reformatComment(comment, "")
                .trim()
                .removePrefix("/**")
                .removeSuffix("*/")
                .trim()
    val reformatted =
        reformattedComment.split("\n").joinToString(separator = "\n") {
          if (it.startsWith(" * ")) {
            it.substring(3)
          } else if (it.startsWith(" *")) {
            ""
          } else if (it.startsWith("* ")) {
            it.substring(2)
          } else {
            it
          }
        }
    return reformatted
  }

  /**
   * Reformats the given Kotlin source file contents using the given
   * [formattingOptions]. The corresponding [file] can be consulted in
   * case we want to limit filtering to particular lines (for example
   * modified git lines)
   */
  private fun reformatKotlinFile(
      source: String,
      formattingOptions: KDocFormattingOptions,
      file: File? = null
  ): String {
    val sb = StringBuilder()
    val lexer = KotlinLexer(source)
    val tokens =
        lexer.findComments(options.kdocComments, options.blockComments, options.lineComments)
    val formatter = KDocFormatter(formattingOptions)
    val filter = options.filter
    var prev = 0
    for ((start, end) in tokens) {
      if (file == null || filter.overlaps(file, source, start, end)) {
        val comment = source.substring(start, end)
        if (skipComment(prev, comment)) {
          continue
        }

        // Include all the non-comments between previous comment end and here
        val segment = source.substring(prev, start)
        sb.append(segment)
        prev = end

        val formatted =
            format(sb, file, source, start, end, comment, lexer, formatter, formattingOptions)
        sb.append(formatted)
      }
    }
    sb.append(source.substring(prev, source.length))

    return sb.toString()
  }

  private fun skipComment(prev: Int, comment: String): Boolean {
    // Let's leave license notices alone.
    if (prev == 0 &&
        comment.isBlockComment() &&
        (comment.contains("license", ignoreCase = true) ||
            comment.contains("copyright", ignoreCase = true))) {
      return true
    }

    // Leave IntelliJ suppression comments alone
    if (comment.startsWith("//noinspection ")) {
      return true
    }

    return false
  }

  private fun format(
      sb: StringBuilder,
      file: File?,
      source: String,
      start: Int,
      end: Int,
      comment: String,
      lexer: KotlinLexer,
      formatter: KDocFormatter,
      formattingOptions: KDocFormattingOptions
  ): String {
    val originalIndent = getIndent(source, start)
    val suffix = !originalIndent.all { it.isWhitespace() }
    val (indent, secondaryIndent) =
        computeIndents(start, { offset -> source[offset] }, source.length)

    val formatted =
        try {
          val task = FormattingTask(formattingOptions, comment, indent, secondaryIndent)
          if (task.type == CommentType.KDOC) {
            task.orderedParameterNames = lexer.getParameterNames(end) ?: emptyList()
          }
          val formatted = formatter.reformatComment(task)

          // If it's the suffix of a line, see if it can fit there even when indented
          // with the previous code
          var addNewline = false
          val firstLineRemaining =
              min(
                  formattingOptions.maxCommentWidth,
                  formattingOptions.maxLineWidth -
                      getIndentSize(task.initialIndent, formattingOptions))
          val firstLine =
              formatted.substring(
                  0, formatted.indexOf('\n').let { if (it == -1) formatted.length else it })
          val reformatted =
              if (suffix && task.type == CommentType.KDOC && formatted.contains("\n")) {
                addNewline = true
                formatted
              } else if (suffix && firstLineRemaining >= firstLine.length) {
                formatted
              } else if (suffix) {
                addNewline = true
                formatted
              } else {
                formatted
              }
          if (addNewline) {
            // Remove trailing whitespace on the line (e.g. separator between code and
            // /** */)
            while (sb.isNotEmpty() && sb[sb.length - 1].isWhitespace()) {
              sb.setLength(sb.length - 1)
            }
            sb.append('\n').append(secondaryIndent)
          }
          reformatted
        } catch (error: Throwable) {
          System.err.println("Failed formatting comment in $file:\n\"\"\"\n$comment\n\"\"\"")
          throw error
        }
    return formatted
  }

  private fun getIndent(source: String, start: Int): String {
    var i = start - 1
    while (i >= 0 && source[i] != '\n') {
      i--
    }
    return source.substring(i + 1, start)
  }
}
