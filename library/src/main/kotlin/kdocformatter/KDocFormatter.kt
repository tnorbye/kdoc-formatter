package kdocformatter

/** Formatter which can reformat KDoc comments */
class KDocFormatter(private val options: KDocFormattingOptions) {
    fun reformatComment(comment: String, indent: String): String {
        val lines = comment.split("\n")
        val rawText = StringBuilder()
        for (l in lines) {
            val trimmed = l.trim()
            val line = trimmed.removePrefix("/**").removePrefix(("*")).trim()
            if (trimmed.endsWith("*/")) {
                rawText.append(line.removeSuffix("*/").removeSuffix("/").trim())
                break
            } else if (line.startsWith("@") && !rawText.endsWith("\n")) {
                // KDoc block tag, must be on its own line.
                rawText.append('\n')
            }
            if (line.isEmpty()) {
                if (rawText.isNotEmpty()) {
                    rawText.append('\n')
                }
            } else {
                rawText.append(line).append(' ')
            }
        }

        val paragraphs = rawText.toString().split("\n")
        val blockTags = mutableSetOf<String>()

        if (options.collapseSingleLine && paragraphs.size == 1) {
            // Does the text fit on a single line?
            val trimmed = paragraphs.first().trim()
            // Subtract out space for "/** " and " */" and the indent:
            val width = options.lineWidth - getIndentSize(indent) - 7
            if (trimmed.length < width) {
                return "/** $trimmed */"
            }
        }

        val blankLineBeforeParagraph = mutableMapOf<String, Boolean>()
        var prevWasBlockTag = false
        for (paragraph in paragraphs) {
            if (paragraph.startsWith("@")) {
                blankLineBeforeParagraph[paragraph] = !prevWasBlockTag
                prevWasBlockTag = true
                blockTags.add(paragraph)
            } else {
                blankLineBeforeParagraph[paragraph] = true
            }
        }
        blankLineBeforeParagraph[paragraphs.first()] = false

        val sb = StringBuilder()
        val separator = "\n$indent * "
        val blankSuffix = "\n$separator"
        sb.append("/**")
        sb.append(separator)

        for (paragraph in paragraphs) {
            if (blankLineBeforeParagraph[paragraph] == true &&
                // collapse several blank lines
                !sb.endsWith(blankSuffix)
            ) {
                // Remove trailing spaces which can happen when we
                // have a paragraph separator
                if (sb.endsWith("* ")) {
                    sb.setLength(sb.length - 1)
                }
                sb.append(separator)
            }
            var offset = 0
            val isBlockTag = blockTags.contains(paragraph)
            while (offset < paragraph.length) {
                val isBeginning = offset == 0
                var width = options.lineWidth - getIndentSize(indent) - 3
                if (isBlockTag && !isBeginning) {
                    width -= 4
                    sb.append("    ")
                }
                while (offset < paragraph.length && paragraph[offset].isWhitespace()) {
                    offset++
                }
                var last = offset + width
                if (last > paragraph.length) {
                    val remainder = paragraph.substring(offset).trim()
                    if (remainder.isNotEmpty()) {
                        sb.append(remainder)
                        sb.append(separator)
                    }
                    break
                } else {
                    while (last >= offset && last < paragraph.length && !paragraph[last].isWhitespace()) {
                        last--
                    }
                    if (last <= offset) {
                        // Couldn't break; search forwards
                        last = offset + width
                        while (last < paragraph.length) {
                            if (paragraph[last].isWhitespace()) {
                                break
                            }
                            last++
                        }
                    }

                    sb.append(paragraph.substring(offset, last).trim())
                    offset = last
                    sb.append(separator)
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
}
