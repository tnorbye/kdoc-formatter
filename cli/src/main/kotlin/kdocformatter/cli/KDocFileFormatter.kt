package kdocformatter.cli

import java.io.File
import kdocformatter.EditorConfigs
import kdocformatter.KDocFormatter

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

        return if (file.path.endsWith(".kt") && options.filter.includes(file) ||
                options.includeMd && file.path.endsWith(".md") && options.filter.includes(file)
        ) {
            val original = file.readText()
            val reformatted = reformatFile(file, original)
            if (reformatted != original) {
                if (options.dryRun) {
                    println(file.path)
                } else {
                    file.writeText(reformatted)
                }
                1
            } else {
                0
            }
        } else {
            0
        }
    }

    fun reformatFile(file: File?, source: String): String {
        if (file != null && file.path.endsWith(".md")) {
            val formattingOptions = file.let { EditorConfigs.getOptions(it) }
            val formatter = KDocFormatter(formattingOptions)
            return formatter.reformatMarkdown(source)
        }

        val sb = StringBuilder()
        val tokens = tokenizeKotlin(source)
        val formattingOptions =
            file?.let { EditorConfigs.getOptions(it) } ?: options.formattingOptions
        val formatter = KDocFormatter(formattingOptions)
        val filter = options.filter
        var nextIsComment = false
        var start = 0
        for ((end, tokenType) in tokens.entries) {
            if (nextIsComment && (file == null || filter.overlaps(file, source, start, end))) {
                val comment = source.substring(start, end)
                val indent = getIndent(source, start)
                val formatted = formatter.reformatComment(comment, indent)
                sb.append(formatted)
            } else {
                val segment = source.substring(start, end)
                sb.append(segment)
            }
            nextIsComment = tokenType == KDOC_COMMENT
            start = end
        }
        sb.append(source.substring(start, source.length))

        return sb.toString()
    }

    private fun getIndent(source: String, start: Int): String {
        var i = start - 1
        while (i >= 0 && source[i] != '\n') {
            i--
        }
        return source.substring(i + 1, start)
    }

    private fun tokenizeKotlin(source: String): Map<Int, Int> {
        val tokens: MutableMap<Int, Int> = LinkedHashMap() // order matters
        val length = source.length
        var state = STATE_INITIAL
        var offset = 0
        var blockCommentDepth = 0
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
                    }
                    offset++
                    continue
                }
                STATE_SLASH -> {
                    if (c == '/') {
                        state = STATE_LINE_COMMENT
                        tokens[offset - 1] = COMMENT
                    } else if (c == '*') {
                        state = STATE_BLOCK_COMMENT
                        blockCommentDepth++
                        if (offset < source.length - 1 && source[offset + 1] == '*') {
                            tokens[offset - 1] = KDOC_COMMENT
                            offset++
                        } else {
                            tokens[offset - 1] = COMMENT
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
                    } else if (c == '/' && offset < source.length - 1 && source[offset + 1] == '*'
                    ) {
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

        return tokens
    }

    companion object {
        private const val PLAIN_TEXT = 1
        private const val COMMENT = 2
        private const val KDOC_COMMENT = 3

        private const val STATE_INITIAL = 1
        private const val STATE_SLASH = 2
        private const val STATE_LINE_COMMENT = 3
        private const val STATE_BLOCK_COMMENT = 4
        private const val STATE_STRING_DOUBLE_QUOTE = 5
        private const val STATE_STRING_SINGLE_QUOTE = 6
        private const val STATE_STRING_TRIPLE_DOUBLE_QUOTE = 7
    }
}
