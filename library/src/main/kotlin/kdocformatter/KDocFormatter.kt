package kdocformatter

import java.util.regex.Pattern
import kotlin.math.max

/** Formatter which can reformat KDoc comments */
class KDocFormatter(private val options: KDocFormattingOptions) {
    /**
     * Reformats the [comment], which follows the given [indent] string
     */
    fun reformatComment(comment: String, indent: String): String {
        val indentSize = getIndentSize(indent)
        val paragraphs = findParagraphs(comment) // Make configurable?
        val lineSeparator = "\n$indent * "

        // Collapse single line?
        if (options.collapseSingleLine && paragraphs.isSingleParagraph()) {
            // Does the text fit on a single line?
            val trimmed = paragraphs.first().text.trim()
            // Subtract out space for "/** " and " */" and the indent:
            val width = options.lineWidth - indentSize - 7
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

            var offset = 0
            val isBlockTag = paragraph.isDocTag()
            val isListItem = paragraph.isListItem()
            while (offset < text.length) {
                val isBeginning = offset == 0
                var width = options.lineWidth - indentSize - 3
                if (options.hangingIndents && (isBlockTag || isListItem) && !isBeginning) {
                    sb.append(paragraph.hangingIndent)
                    width -= getIndentSize(paragraph.hangingIndent)
                }
                while (offset < text.length && text[offset].isWhitespace()) {
                    offset++
                }
                var last = offset + width
                if (last > text.length) {
                    val remainder = text.substring(offset).trim()
                    if (remainder.isNotEmpty()) {
                        sb.append(remainder)
                        sb.append(lineSeparator)
                    }
                    break
                } else {
                    while (last >= offset && last < text.length && !text[last].isWhitespace()) {
                        last--
                    }
                    if (last <= offset) {
                        // Couldn't break; search forwards
                        last = offset + width
                        while (last < text.length) {
                            if (text[last].isWhitespace()) {
                                break
                            }
                            last++
                        }
                    }

                    sb.append(text.substring(offset, last).trim())
                    offset = last
                    sb.append(lineSeparator)
                }
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
        val lines = comment.split("\n")
        val rawText = StringBuilder()
        var inPreformat = false
        for (l in lines) {
            val trimmed = l.trim()
            val lineWithIndentation = trimmed.removePrefix("/**").removePrefix(("*"))
            val line = lineWithIndentation.trim()
            if (line.startsWith("```")) {
                inPreformat = !inPreformat
                if (!inPreformat) {
                    rawText.append(lineWithIndentation.substring(1).trimEnd()).append("\n")
                    continue
                }
            } else if (line.startsWith("<pre>", ignoreCase = true)) {
                inPreformat = true
            } else if (line.startsWith("</pre>", ignoreCase = true)) {
                inPreformat = false
                rawText.append(lineWithIndentation.substring(1).trimEnd()).append("\n")
                continue
            }

            if (inPreformat) {
                rawText.appendFirstNewline()
                rawText.append(lineWithIndentation.substring(1).trimEnd()).append("\n")
                continue
            }

            if (line.isListItem()) {
                rawText.appendFirstNewline()
                rawText.append(line).append(' ')
                continue
            }

            if (trimmed.endsWith("*/")) {
                rawText.append(line.removeSuffix("*/").removeSuffix("/").trim())
                break
            } else if (line.startsWith("@") && !rawText.endsWith("\n")) {
                // KDoc block tag, must be on its own line.
                rawText.append('\n')
            } else if (lineWithIndentation.startsWith("    ")) { // markdown preformatted text
                rawText.appendFirstNewline()
                rawText.append(lineWithIndentation.substring(1).trimEnd()).append("\n")
                continue
            }
            if (line.isEmpty()) {
                if (rawText.isNotEmpty()) {
                    rawText.append('\n')
                }
            } else {
                if (options.collapseSpaces) {
                    rawText.append(line.collapseSpaces())
                } else {
                    rawText.append(line)
                }
                rawText.append(' ')
            }
        }

        val paragraphs = mutableListOf<Paragraph>()
        for (line in rawText.toString().split("\n")) {
            if (line.isBlank()) {
                continue
            }
            val paragraph = Paragraph(line)
            paragraphs.add(paragraph)
        }

        return ParagraphList(paragraphs)
    }

    private fun getIndent(width: Int): String {
        val sb = StringBuilder()
        for (i in 0 until width) {
            sb.append(' ')
        }
        return sb.toString()
    }

    private fun getIndentSize(indent: String): Int {
        var size = 0
        for (c in indent) {
            if (c == '\t')
                size += options.tabWidth
            else
                size++
        }
        return size
    }

    private class Paragraph(val text: String) {
        fun isDocTag() = text.startsWith("@")
        fun isListItem() = listItem
        var separate = true
        val listItem = text.isListItem()
        var preformatted = text.startsWith("    ")
        var hangingIndent = if (isDocTag()) "    " else ""
        override fun toString(): String = text
    }

    private inner class ParagraphList(val paragraphs: List<Paragraph>) : Iterable<Paragraph> {
        init {
            var prev: Paragraph? = null
            var inPreformat = false
            for (paragraph in paragraphs) {
                paragraph.preformatted = inPreformat || paragraph.preformatted
                paragraph.separate = when {
                    prev == null -> false
                    // Don't separate kdoc tags, except for the first one
                    paragraph.isDocTag() -> !prev.isDocTag()
                    paragraph.preformatted -> !prev.preformatted
                    paragraph.listItem -> false
                    else -> true
                }
                if (paragraph.listItem) {
                    paragraph.hangingIndent = getIndent(paragraph.text.indexOf(' ') + 1)
                }
                if (paragraph.text.startsWith("```")) {
                    if (!inPreformat) {
                        paragraph.preformatted = true
                    }
                    inPreformat = !inPreformat
                } else if (paragraph.text.startsWith("<pre>", ignoreCase = true)) {
                    paragraph.preformatted = true
                    inPreformat = true
                } else if (paragraph.text.startsWith("</pre>", ignoreCase = true)) {
                    paragraph.preformatted = true
                    inPreformat = false
                }
                prev = paragraph
            }
        }

        fun isSingleParagraph() = paragraphs.size == 1
        override fun iterator(): Iterator<Paragraph> = paragraphs.iterator()
    }

    companion object {
        private val numberPattern = Pattern.compile("^\\d+\\. ")

        private fun String.isListItem(): Boolean {
            return startsWith("- ") || startsWith("* ") || startsWith("+ ") ||
                firstOrNull()?.isDigit() == true && numberPattern.matcher(this).find()
        }

        private fun String.collapseSpaces(): String {
            if (indexOf("  ") == -1) {
                return this
            }
            val sb = StringBuilder()
            var prev: Char = this[0]
            for (i in 1 until length) {
                if (prev == ' ') {
                    if (this[i] == ' ') {
                        continue
                    }
                }
                sb.append(this[i])
                prev = this[i]
            }
            return sb.toString()
        }

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
