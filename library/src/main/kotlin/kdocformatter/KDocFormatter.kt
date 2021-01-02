package kdocformatter

/** Formatter which can reformat KDoc comments */
class KDocFormatter(private val options: KDocFormattingOptions) {
    /**
     * Reformats the [comment], which follows the given [indent]
     * string
     */
    fun reformatComment(comment: String, indent: String): String {
        val indentSize = getIndentSize(indent)
        val paragraphs = findParagraphs(comment) // Make configurable?
        val lineSeparator = "\n$indent * "
        val blankPrefix = "\n$lineSeparator"

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
            if (paragraph.isPreformatted()) {
                sb.append(text)
                sb.append(lineSeparator)
                continue
            }

            var offset = 0
            val isBlockTag = paragraph.isDocTag()
            while (offset < text.length) {
                val isBeginning = offset == 0
                var width = options.lineWidth - indentSize - 3
                if (isBlockTag && !isBeginning) {
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

    private fun findParagraphs(comment: String): ParagraphList {
        val lines = comment.split("\n")
        val rawText = StringBuilder()
        for (l in lines) {
            val trimmed = l.trim()
            val lineWithIndentation = trimmed.removePrefix("/**").removePrefix(("*"))
            val line = lineWithIndentation.trim()
            if (trimmed.endsWith("*/")) {
                rawText.append(line.removeSuffix("*/").removeSuffix("/").trim())
                break
            } else if (line.startsWith("@") && !rawText.endsWith("\n")) {
                // KDoc block tag, must be on its own line.
                rawText.append('\n')
            } else if (lineWithIndentation.startsWith("    ")) { // markdown preformatted text
                if (!rawText.endsWith("\n")) {
                    rawText.append("\n")
                }
                rawText.append(lineWithIndentation.substring(1).trimEnd())
                rawText.append("\n")
                continue
            }
            if (line.isEmpty()) {
                if (rawText.isNotEmpty()) {
                    rawText.append('\n')
                }
            } else {
                rawText.append(line).append(' ')
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

    private class Paragraph(val text: String) {
        var separate = true
        fun isDocTag() = text.startsWith("@")
        fun isPreformatted() = text.startsWith("    ")
        var hangingIndent = if (isDocTag()) "    " else ""
    }

    private class ParagraphList(val paragraphs: List<Paragraph>) : Iterable<Paragraph> {
        init {
            computeSeparators()
        }

        fun isSingleParagraph() = paragraphs.size == 1

        fun computeSeparators() {
            var prev: Paragraph? = null
            for (paragraph in paragraphs) {
                paragraph.separate = when {
                    prev == null -> false
                    // Don't separate kdoc tags, except for the first one
                    paragraph.isDocTag() -> !prev.isDocTag()
                    paragraph.isPreformatted() -> !prev.isPreformatted()
                    else -> true
                }
                prev = paragraph
            }
        }

        override fun iterator(): Iterator<Paragraph> = paragraphs.iterator()
    }
}
