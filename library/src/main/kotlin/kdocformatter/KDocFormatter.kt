package kdocformatter

import kotlin.math.max
import kotlin.math.min

/** Formatter which can reformat KDoc comments */
class KDocFormatter(private val options: KDocFormattingOptions) {
    /**
     * Reformats the [comment], which follows the given [indent] string
     */
    fun reformatComment(comment: String, indent: String): String {
        val indentSize = getIndentSize(indent, options)
        val paragraphs = findParagraphs(comment) // Make configurable?
        val lineSeparator = "\n$indent * "

        // Collapse single line?
        if (options.collapseSingleLine && paragraphs.isSingleParagraph()) {
            // Does the text fit on a single line?
            val trimmed = paragraphs.first().text.trim()
            // Subtract out space for "/** " and " */" and the indent:
            val width = min(options.maxLineWidth - indentSize - 7, options.maxCommentWidth)
            if (trimmed.length < width) {
                return "/** $trimmed */"
            }
        }

        val sb = StringBuilder()
        sb.append("/**")
        sb.append(lineSeparator)

        for (paragraph in paragraphs) {
            if (paragraph.separate) {
                // Remove trailing spaces which can happen when we
                // have a paragraph separator
                if (sb.endsWith("* ")) {
                    sb.setLength(sb.length - 1)
                }
                sb.append(lineSeparator)
            }
            val text = paragraph.text
            if (paragraph.preformatted) {
                sb.append(text)
                sb.append(lineSeparator)
                continue
            }

            val lines = paragraph.reflow(min(options.maxCommentWidth, options.maxLineWidth - indentSize - 3), options)
            var first = true
            val hangingIndent = paragraph.hangingIndent
            for (line in lines) {
                if (first) {
                    first = false
                } else {
                    sb.append(hangingIndent)
                }
                sb.append(line)
                sb.append(lineSeparator)
            }
        }
        if (sb.endsWith("* ")) {
            sb.setLength(sb.length - 2)
        }
        sb.append("*/")

        return sb.toString()
    }

    private fun StringBuilder.appendFirstNewline(): StringBuilder {
        if (length > 0 && !endsWith("\n")) {
            append("\n")
        }
        return this
    }

    private fun findParagraphs(comment: String): ParagraphList {
        val lines = comment.removePrefix("/**").removeSuffix("*/").trim().split("\n")
        val rawText = StringBuilder()

        fun lineContent(line: String): String {
            val trimmed = line.trim()
            return when {
                trimmed.startsWith("* ") -> trimmed.substring(2)
                trimmed.startsWith("*") -> trimmed.substring(1)
                else -> line
            }
        }

        fun addLines(
            lines: List<String>,
            i: Int,
            includeEnd: Boolean,
            preformatted: Boolean,
            until: (Int, String, String) -> Boolean
        ): Int {
            val separator = if (preformatted) "\n" else " "
            if (preformatted) {
                rawText.appendFirstNewline()
            }
            var j = i
            while (j < lines.size) {
                val l = lines[j]
                val lineWithIndentation = lineContent(l)
                val lineWithoutIndentation = lineWithIndentation.trim()

                if (!includeEnd) {
                    if (j > i && until(j, lineWithoutIndentation, lineWithIndentation)) {
                        return j
                    }
                }

                if (preformatted) {
                    rawText.append(lineWithIndentation).append(separator)
                } else {
                    rawText.append(lineWithoutIndentation.collapseSpaces()).append(separator)
                }

                if (includeEnd) {
                    if (j > i && until(j, lineWithoutIndentation, lineWithIndentation)) {
                        return j + 1
                    }
                }

                j++
            }

            return j
        }

        var i = 0
        while (i < lines.size) {
            val l = lines[i++]
            val lineWithIndentation = lineContent(l)
            val lineWithoutIndentation = lineWithIndentation.trim()
            if (lineWithoutIndentation.startsWith("```")) {
                i = addLines(lines, i - 1, includeEnd = true, preformatted = true) { _, _, s ->
                    s.startsWith("```")
                }
                continue
            } else if (lineWithoutIndentation.startsWith("<pre>", ignoreCase = true)) {
                i = addLines(lines, i - 1, includeEnd = true, preformatted = true) { _, _, s ->
                    s.startsWith("</pre>", ignoreCase = true)
                }
                continue
            } else if (lineWithIndentation.startsWith("    ")) { // markdown preformatted text
                i = addLines(lines, i - 1, includeEnd = true, preformatted = true) { _, _, s ->
                    !s.startsWith(" ")
                }
                rawText.append('\n')
                continue
            }

            if (lineWithoutIndentation.isListItem() || lineWithoutIndentation.isKDocTag()) {
                rawText.appendFirstNewline()
                i = addLines(lines, i - 1, includeEnd = false, preformatted = false) { _, w, s ->
                    s.isBlank() || w.isListItem() || s.isKDocTag()
                }
                rawText.append('\n')
                continue
            }

            if (lineWithoutIndentation.isEmpty()) {
                if (rawText.isNotEmpty()) {
                    rawText.append('\n')
                }
            } else {
                if (options.collapseSpaces) {
                    rawText.append(lineWithoutIndentation.collapseSpaces())
                } else {
                    rawText.append(lineWithoutIndentation)
                }
                rawText.append(' ')
            }
        }

        val paragraphs = mutableListOf<Paragraph>()
        for (line in rawText.toString().split("\n")) {
            if (line.isBlank()) {
                continue
            }

            val paragraph = Paragraph(line, options)
            paragraphs.add(paragraph)
        }

        return ParagraphList(paragraphs, options)
    }

    companion object {
        /**
         * Attempt to preserve the caret position across reformatting.
         * Returns the delta in the new comment.
         */
        fun findSamePosition(comment: String, delta: Int, reformattedComment: String): Int {
            fun nextSignificantChar(s: String, from: Int): Int {
                var curr = from
                while (curr < s.length) {
                    val c = s[curr]
                    if (c.isWhitespace() || c == '*') {
                        curr++
                    } else {
                        break
                    }
                }
                return curr
            }

            var offset = 0
            var reformattedOffset = 0
            while (offset < delta && reformattedOffset < reformattedComment.length) {
                offset = nextSignificantChar(comment, offset)
                reformattedOffset = nextSignificantChar(reformattedComment, reformattedOffset)
                offset++
                reformattedOffset++
            }
            return max(0, reformattedOffset - 1)
        }
    }
}
