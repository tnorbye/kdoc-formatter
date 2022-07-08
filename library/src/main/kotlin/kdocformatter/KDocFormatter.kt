package kdocformatter

import kotlin.math.min

/** Formatter which can reformat KDoc comments. */
class KDocFormatter(private val options: KDocFormattingOptions) {
    /**
     * Reformats the [comment], which follows the given [indent] string.
     */
    fun reformatComment(comment: String, indent: String): String {
        val lineComment = comment.startsWith("//")
        val indentSize = getIndentSize(indent, options)
        val paragraphs = ParagraphListBuilder(comment, options).scan()
        val lineSeparator =
            if (lineComment) {
                "\n$indent// "
            } else {
                "\n$indent * "
            }

        // Collapse single line? If alternate is turned on, use the opposite of the
        // setting
        val collapseLine = options.collapseSingleLine.let { if (options.alternate) !it else it }
        if (paragraphs.isSingleParagraph() && collapseLine && !lineComment) {
            // Does the text fit on a single line?
            val trimmed = paragraphs.first().text.trim()
            // Subtract out space for "/** " and " */" and the indent:
            val width = min(options.maxLineWidth - indentSize - 7, options.maxCommentWidth)
            if (trimmed.length < width) {
                return "/** $trimmed */"
            }
        }

        val sb = StringBuilder()

        if (!lineComment) {
            sb.append("/**")
            sb.append(lineSeparator)
        } else {
            sb.append("// ")
        }

        for (paragraph in paragraphs) {
            if (paragraph.separate) {
                // Remove trailing spaces which can happen when we
                // have a paragraph separator
                stripTrailingSpaces(lineComment, sb)
                sb.append(lineSeparator)
            }
            val text = paragraph.text
            if (paragraph.preformatted) {
                sb.append(text)
                // Remove trailing spaces which can happen when we
                // have an empty line in a preformatted paragraph.
                stripTrailingSpaces(lineComment, sb)
                sb.append(lineSeparator)
                continue
            }

            val maxLineWidth =
                min(options.maxCommentWidth, options.maxLineWidth - indentSize - 3) -
                    if (paragraph.quoted) 2 else 0

            val lines = paragraph.reflow(maxLineWidth, options)
            var first = true
            val hangingIndent = paragraph.hangingIndent
            for (line in lines) {
                sb.append(paragraph.indent)
                if (first && !paragraph.continuation) {
                    first = false
                } else {
                    sb.append(hangingIndent)
                }
                if (paragraph.quoted) {
                    sb.append("> ")
                }
                if (line.isEmpty()) {
                    // Remove trailing spaces which can happen when we
                    // have a paragraph separator
                    stripTrailingSpaces(lineComment, sb)
                } else {
                    sb.append(line)
                }
                sb.append(lineSeparator)
            }
        }
        if (!lineComment) {
            if (sb.endsWith("* ")) {
                sb.setLength(sb.length - 2)
            }
            sb.append("*/")
        } else if (sb.endsWith(lineSeparator)) {
            sb.removeSuffix(lineSeparator)
        }

        if (lineComment) {
            return sb.trim().removeSuffix("//").trim().toString()
        } else {
            return sb.toString()
        }
    }

    private fun stripTrailingSpaces(lineComment: Boolean, sb: StringBuilder) {
        if (!lineComment && sb.endsWith("* ")) {
            sb.setLength(sb.length - 1)
        } else if (lineComment && sb.endsWith("// ")) {
            sb.setLength(sb.length - 1)
        }
    }

    /** Reformats a markdown document. */
    fun reformatMarkdown(md: String): String {
        // Just leverage the comment machinery here -- convert the markdown into a
        // kdoc comment, reformat that, and then uncomment it
        val comment = "/**\n" + md.split("\n").joinToString(separator = "\n") { " * $it" } + "\n*/"
        val reformattedComment =
            " " + reformatComment(comment, "").trim().removePrefix("/**").removeSuffix("*/").trim()
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
}
