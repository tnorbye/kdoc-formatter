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

import java.util.ArrayDeque

/**
 * Simple lexer which analyzes Kotlin source code and finds the KDoc comments within it. It
 * basically looks for "/**" and "*/", but is also aware of strings, quoted strings, function name
 * literals, block comments and nested block comments etc such that it doesn't accidentally pick up
 * strings, comments or function names that look like comments but aren't.
 */
class KotlinLexer(private val source: String) {
  /** Returns a list of (start,end) offsets of KDocs in the document. */
  fun findComments(
      includeDocComments: Boolean,
      includeBlockComments: Boolean,
      includeLineComments: Boolean
  ): List<Pair<Int, Int>> {
    val tokens: MutableMap<Int, Int> = LinkedHashMap() // order matters
    val length = source.length
    var state = STATE_INITIAL
    var offset = 0
    var blockCommentDepth = 0
    var braceDepth = 0
    val stack = ArrayDeque<NestingContext>()
    loop@ while (offset < length) {
      val c = source[offset]
      when (state) {
        STATE_INITIAL -> {
          if (c == '/') {
            state = STATE_SLASH
            offset++
            continue
          } else if (c == '"') {
            state = STATE_STRING_DOUBLE_QUOTE
            // Look for triple-quoted strings
            if (source.startsWith("\"\"\"", offset)) {
              state = STATE_STRING_TRIPLE_DOUBLE_QUOTE
              offset += 3
              continue
            }
          } else if (c == '\'') {
            state = STATE_STRING_SINGLE_QUOTE
          } else if (c == '`') {
            offset = source.indexOf('`', offset + 1)
            if (offset == -1) {
              break
            }
          } else if (c == '{') {
            braceDepth++
          } else if (c == '}') {
            braceDepth--
            var last = stack.peekLast()
            if (last != null && last.depth == braceDepth) {
              last = stack.removeLast()
              state = last.state
            }
          }
          offset++
          continue
        }
        STATE_SLASH -> {
          if (c == '/') {
            state = STATE_LINE_COMMENT
            tokens[offset - 1] = LINE_COMMENT
          } else if (c == '*') {
            state = STATE_BLOCK_COMMENT
            blockCommentDepth++
            if (offset < source.length - 1 && source[offset + 1] == '*') {
              tokens[offset - 1] = KDOC_COMMENT
              offset++
            } else {
              tokens[offset - 1] = BLOCK_COMMENT
            }
          } else {
            state = STATE_INITIAL
            continue
          }
          offset++
          continue
        }
        STATE_LINE_COMMENT -> {
          if (c == '\n') {
            // Scan ahead to see if there are more line comments following.
            // We want to treat a block of line comments as a coherent unit
            // flowing together.
            var peek = offset + 1
            while (peek < length) {
              val d = source[peek]
              if (d == '\n') {
                break
              } else if (!d.isWhitespace()) {
                if (d == '/' && peek < length - 1 && source[peek + 1] == '/') {
                  offset = peek + 2
                  continue@loop
                }
                break
              }
              peek++
            }

            state = STATE_INITIAL
            tokens[offset] = PLAIN_TEXT
          }
          offset++
          continue
        }
        STATE_BLOCK_COMMENT -> {
          if (c == '*' && offset < source.length - 1 && source[offset + 1] == '/') {
            blockCommentDepth--
            if (blockCommentDepth == 0) {
              state = STATE_INITIAL
              offset += 2
              tokens[offset] = PLAIN_TEXT
              continue
            }
          } else if (c == '/' && offset < source.length - 1 && source[offset + 1] == '*') {
            offset++
            blockCommentDepth++
          }
          offset++
          continue
        }
        STATE_STRING_DOUBLE_QUOTE -> {
          if (c == '\\') {
            offset += 2
            continue
          } else if (c == '"') {
            state = STATE_INITIAL
            offset++
            continue
          } else if (c == '$' && source.startsWith("\${", offset)) {
            offset += 2
            stack.addLast(NestingContext(braceDepth, STATE_STRING_DOUBLE_QUOTE))
            braceDepth++
            state = STATE_INITIAL
            continue
          }
          offset++
          continue
        }
        STATE_STRING_SINGLE_QUOTE -> {
          if (c == '\\') {
            offset += 2
            continue
          } else if (c == '\'') {
            state = STATE_INITIAL
            offset++
            continue
          }
          offset++
          continue
        }
        STATE_STRING_TRIPLE_DOUBLE_QUOTE -> {
          if (c == '"' && source.startsWith("\"\"\"", offset)) {
            offset += 3
            state = STATE_INITIAL
            continue
          } else if (c == '$' && source.startsWith("\${", offset)) {
            offset += 2
            stack.addLast(NestingContext(braceDepth, STATE_STRING_TRIPLE_DOUBLE_QUOTE))
            braceDepth++
            state = STATE_INITIAL
            continue
          }
          offset++
          continue
        }
        else -> assert(false) { state }
      }
    }

    var start = 0
    var nextIsComment = false
    val regions = mutableListOf<Pair<Int, Int>>()
    for ((end, tokenType) in tokens.entries) {
      if (nextIsComment) {
        regions.add(Pair(start, end))
      }
      nextIsComment =
          includeDocComments && tokenType == KDOC_COMMENT ||
              includeBlockComments && tokenType == BLOCK_COMMENT ||
              includeLineComments && tokenType == LINE_COMMENT
      start = end
    }

    return regions
  }

  /**
   * Attempts to compute the parameter names for the Kotlin function following a comment. If it's
   * followed by something else (e.g a class, a property, a parameter, etc) it will return null.
   *
   * Note that this is based on some simple string analysis of the source code so it might not
   * always be correct. However, when kdoc-formatter runs from within the IDE, it's using the real
   * compiler's AST to populate the parameter map (and when kdoc-formatter is integrated in other
   * formatting tools with a full AST that could be used instead). This code errs on the side of
   * caution, but does try to handle a variety of scenarios:
   * * Block and line comments within the signature
   * * Annotations (potentially with values such as strings which are ignored (though here, if
   *   you're using complex string substitution it could get confused)
   * * Types including type wildcards, default values including function pointers etc -- basically
   *   strings that can contain commas and parentheses etc that could potentially confuse the code
   *   trying to pick out the signatures
   * * Name literals (using back ticks)
   */
  fun getParameterNames(start: Int): List<String>? {
    var i = start
    val length = source.length

    fun skipSpace() {
      while (i < length && source[i].isWhitespace()) {
        i++
      }
    }

    fun nextToken(matchParens: Boolean = false): String? {
      skipSpace()
      val from = i
      if (source.startsWith("/*", i)) {
        if (source.startsWith("/**", i)) {
          return null
        }
        // skip to end
        i = source.indexOf("*/", i + 2)
        if (i == -1) {
          i = length
        } else {
          i++
        }
        return ""
      } else if (source.startsWith("//", i)) {
        i = source.indexOf('\n', i)
        if (i == -1) {
          i = length
        } else {
          i++
        }
        return ""
      } else if (source.startsWith("`", i)) {
        val end = source.indexOf('`', i + 1)
        if (end != -1) {
          i = end + 1 // include ``'s
          return source.substring(from, i)
        }
      } else if (source.startsWith("'", i) || source.startsWith("\"", i)) {
        val open = source[i]
        var end = i + 1
        while (end < length) {
          val c = source[end++]
          if (c == open) {
            break
          } else if (c == '\\') {
            end++
          }
        }
        i = end
        return source.substring(from, i)
      } else if (source.startsWith("@", i)) { // Annotation in signature
        // Skip to whitespace or 0 parenthesis balance, e.g. across "@JvmStatic" or
        // "@Suppress("foo", "bar")"
        var parens = 0
        while (i < length) {
          val c = source[i]
          if (c.isWhitespace() && parens == 0) {
            break
          } else if (c == '(') {
            parens++
          } else if (c == ')') {
            parens--
          }
          i++
        }
        return source.substring(from, i)
      } else if (source.startsWith("(", i) && matchParens) { // Annotation in signature
        // Skip to whitespace or 0 parenthesis balance, e.g. across "@JvmStatic" or
        // "@Suppress("foo", "bar")"
        var parens = 0
        while (i < length) {
          val c = source[i]
          if (c == '(') {
            parens++
          } else if (c == ')') {
            parens--
            if (parens == 0) {
              i++
              break
            }
          }
          i++
        }
        return source.substring(from, i)
      }

      if (i == length) {
        return ""
      }
      if (source[i].isJavaIdentifierPart()) {
        while (i < length) {
          val c = source[i]
          if (c.isJavaIdentifierPart()) {
            i++
          } else if (c == '<') {
            // E.g. EnumMap<Foo, Bar> should be a single token
            var balance = 0
            while (i < length) {
              val d = source[i]
              if (d == '>') {
                balance--
                if (balance == 0) {
                  i++
                  break
                }
              } else if (d == '<') {
                balance++
              }
              i++
            }
            continue
          } else {
            break
          }
        }
      } else {
        i++
      }
      return source.substring(from, i)
    }

    while (i < length) {
      skipSpace()
      val token = nextToken() ?: return null
      if (token == "fun") {
        // Find beginning of parameter list
        while (i < length) {
          val name = nextToken(matchParens = false) ?: return null
          if (name == "(") {
            break
          }
        }
        val parameters = mutableListOf<String>()

        while (i < length) {
          // Name is last symbol before ":"
          var prev = nextToken() ?: return null
          if (prev == ")") { // empty list
            return parameters
          }
          while (i < length) {
            val name = nextToken() ?: return null
            if (name == ":") {
              parameters.add(prev.removeSurrounding("`"))
              break
            }
            prev = name
          }

          // Skip over type and default value
          while (i < length) {
            val next = nextToken() ?: return null
            if (next == "," || next == ")") {
              break
            }
          }
          if (source[i - 1] == ')') {
            break
          } else {
            assert(source[i - 1] == ',')
          }
        }
        return parameters
      } else if (token == "class" ||
          token == "object" ||
          token == "interface" ||
          token == "val" ||
          token == "var") {
        return null
      } // else: could be other modifiers, "public", etc

      if (token.contains("{")) { // could happen with lamdas as default value expressions
        return null
      }
    }

    return null
  }

  companion object {
    private const val PLAIN_TEXT = 1
    private const val LINE_COMMENT = 2
    private const val BLOCK_COMMENT = 3
    private const val KDOC_COMMENT = 4

    private const val STATE_INITIAL = 1
    private const val STATE_SLASH = 2
    private const val STATE_LINE_COMMENT = 3
    private const val STATE_BLOCK_COMMENT = 4
    private const val STATE_STRING_DOUBLE_QUOTE = 5
    private const val STATE_STRING_SINGLE_QUOTE = 6
    private const val STATE_STRING_TRIPLE_DOUBLE_QUOTE = 7
  }

  class NestingContext(var depth: Int, var state: Int)
}
