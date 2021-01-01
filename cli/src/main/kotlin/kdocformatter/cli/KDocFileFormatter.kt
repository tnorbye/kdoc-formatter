package kdocformatter.cli

import kdocformatter.KDocFormatter
import kdocformatter.KDocFormattingOptions

/**
 * This class attempts to iterate over an entire Kotlin source file
 * and reformat all the KDocs it finds within. This is based on some
 * light-weight lexical analysis to identify comments.
 */
class KDocFileFormatter(private val options: KDocFormattingOptions) {
    fun reformatFile(source: String): String {
        val sb = StringBuilder()
        val styles = tokenizeKotlin(source)
        val formatter = KDocFormatter(options)

        sb.clear()
        var nextIsComment = false
        var prev = 0
        for ((start, style) in styles.entries) {
            if (nextIsComment) {
                val comment = source.substring(prev, start)
                val indent = getIndent(source, prev)
                val rewritten = formatter.reformatComment(comment, indent)
                sb.append(rewritten)
                nextIsComment = false
            } else {
                val segment = source.substring(prev, start)
                sb.append(segment)
            }
            if (style == STYLE_KDOC_COMMENT) {
                nextIsComment = true
            }
            prev = start
        }
        sb.append(source.substring(prev, source.length))

        return sb.toString()
    }

    private fun getIndent(source: String, start: Int): String {
        var i = start - 1
        while (i >= 0 && source[i] != '\n') {
            i--
        }
        return source.substring(i + 1, start)
    }

    // Extracted from LintSyntaxHighlighter. The purpose here is to
    // figure out where the comments are and not be confused about seeing
    // comment line constructs in literal strings, to properly ignore
    // escapes, etc.
    private fun tokenizeKotlin(source: String): Map<Int, Int> {
        val styles: MutableMap<Int, Int> = LinkedHashMap()

        val length = source.length
        var state = STATE_INITIAL
        var offset = 0
        while (offset < length) {
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
                    } else if (Character.isDigit(c)) {
                        state = STATE_NUMBER
                    } else if (Character.isJavaIdentifierStart(c)) {
                        state = STATE_IDENTIFIER
                    }
                    offset++
                    continue
                }
                STATE_NUMBER -> {
                    if (Character.isDigit(c) ||
                        c == '.' || c == '_' || c == 'l' || c == 'L' || c == 'x' || c == 'X' ||
                        c in 'a'..'f' ||
                        c in 'A'..'F'
                    ) {
                        offset++
                        continue
                    }
                    state = STATE_INITIAL
                    continue
                }
                STATE_IDENTIFIER -> {
                    if (Character.isJavaIdentifierPart(c)) {
                        offset++
                        continue
                    }
                    state = STATE_INITIAL
                    continue
                }
                STATE_SLASH -> {
                    if (c == '/') {
                        state = STATE_LINE_COMMENT
                        styles[offset - 1] = STYLE_COMMENT
                    } else if (c == '*') {
                        state = STATE_BLOCK_COMMENT
                        if (offset < source.length - 1 && source[offset + 1] == '*') {
                            styles[offset - 1] = STYLE_KDOC_COMMENT
                            offset++
                        } else {
                            styles[offset - 1] = STYLE_COMMENT
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
                        state = STATE_INITIAL
                        styles[offset] = STYLE_PLAIN_TEXT
                    }
                    offset++
                    continue
                }
                STATE_BLOCK_COMMENT -> {
                    if (c == '*' && offset < source.length - 1 && source[offset + 1] == '/') {
                        state = STATE_INITIAL
                        offset += 2
                        styles[offset] = STYLE_PLAIN_TEXT
                        continue
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
                        styles[offset] = STYLE_PLAIN_TEXT
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
                    }
                    offset++
                    continue
                }
                else -> assert(false) { state }
            }
        }

        return styles
    }

    companion object {
        private const val STYLE_PLAIN_TEXT = 1
        private const val STYLE_COMMENT = 2
        private const val STYLE_KDOC_COMMENT = 3

        private const val STATE_INITIAL = 1
        private const val STATE_SLASH = 2
        private const val STATE_LINE_COMMENT = 3
        private const val STATE_BLOCK_COMMENT = 4
        private const val STATE_STRING_DOUBLE_QUOTE = 5
        private const val STATE_STRING_SINGLE_QUOTE = 6
        private const val STATE_STRING_TRIPLE_DOUBLE_QUOTE = 7
        private const val STATE_NUMBER = 8
        private const val STATE_IDENTIFIER = 9
    }
}
