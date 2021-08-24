package kdocformatter

class ParagraphListBuilder(
    comment: String,
    private val options: KDocFormattingOptions
) {
    private val lineComment: Boolean = comment.startsWith("//")
    private val paragraphs: MutableList<Paragraph> = mutableListOf()
    private val lines = if (lineComment)
        comment.split("\n")
    else
        comment.removePrefix("/**").removeSuffix("*/").trim().split("\n")

    private fun lineContent(line: String): String {
        val trimmed = line.trim()
        return when {
            lineComment && trimmed.startsWith("// ") -> trimmed.substring(3)
            lineComment && trimmed.startsWith("//") -> trimmed.substring(2)
            trimmed.startsWith("* ") -> trimmed.substring(2)
            trimmed.startsWith("*") -> trimmed.substring(1)
            else -> line
        }
    }

    private fun closeParagraph(): Paragraph {
        val text = paragraph.text
        when {
            text.isKDocTag() -> {
                paragraph.doc = true
                paragraph.hanging = true
            }
            text.isTodo() -> {
                paragraph.hanging = true
            }
            text.isListItem() -> paragraph.hanging = true
            text.startsWith("    ") -> paragraph.preformatted = true
        }
        if (!paragraph.isEmpty() || paragraph.allowEmpty) {
            paragraphs.add(paragraph)
        }
        return paragraph
    }

    private fun newParagraph(): Paragraph {
        closeParagraph()
        val prev = paragraph
        paragraph = Paragraph(options)
        prev.next = paragraph
        paragraph.prev = prev
        return paragraph
    }

    private var paragraph = Paragraph(options)

    private fun appendText(s: String): ParagraphListBuilder {
        paragraph.content.append(s)
        return this
    }

    private fun addLines(
        i: Int,
        includeEnd: Boolean = true,
        until: (Int, String, String) -> Boolean = { _, _, _ -> true },
        customize: (Int, Paragraph) -> Unit = { _, _ -> },
        shouldBreak: (String, String) -> Boolean = { _, _ -> false },
        separator: String = " ",
    ): Int {
        var j = i
        while (j < lines.size) {
            val l = lines[j]
            val lineWithIndentation = lineContent(l)
            val lineWithoutIndentation = lineWithIndentation.trim()

            if (!includeEnd) {
                if (j > i && until(j, lineWithoutIndentation, lineWithIndentation)) {
                    stripTrailingBlankLines()
                    return j
                }
            }

            if (shouldBreak(lineWithoutIndentation, lineWithIndentation)) {
                val p = newParagraph()
                customize(j, p)
            }

            if (lineWithIndentation.startsWith("> ")) { // Look for quoted && too?
                appendText(lineWithoutIndentation.substring(2).collapseSpaces())
            } else {
                appendText(lineWithoutIndentation.collapseSpaces())
            }
            appendText(separator)
            customize(j, paragraph)
            if (includeEnd) {
                if (j > i && until(j, lineWithoutIndentation, lineWithIndentation)) {
                    stripTrailingBlankLines()
                    return j + 1
                }
            }

            j++
        }

        stripTrailingBlankLines()
        newParagraph()

        return j
    }

    private fun addPreformatted(
        i: Int,
        until: (String) -> Boolean = { true }
    ): Int {
        newParagraph()
        var j = i
        while (j < lines.size) {
            val l = lines[j]
            val lineWithIndentation = lineContent(l)
            appendText(lineWithIndentation)
            paragraph.preformatted = true
            paragraph.allowEmpty = true
            newParagraph()
            if (j > i && until(lineWithIndentation)) {
                stripTrailingBlankLines()
                return j + 1
            }
            j++
        }
        stripTrailingBlankLines()
        newParagraph()
        return j
    }

    private fun stripTrailingBlankLines() {
        for (p in paragraphs.size - 1 downTo 0) {
            val paragraph = paragraphs[p]
            if (!paragraph.isEmpty()) {
                break
            }
            paragraphs.removeAt(p)
        }
    }

    fun scan(): ParagraphList {
        var i = 0
        while (i < lines.size) {
            val l = lines[i++]
            val lineWithIndentation = lineContent(l)
            val lineWithoutIndentation = lineWithIndentation.trim()

            // Look for special cases that indicates this line is a block element (which
            // begins a new paragraph)
            if (lineWithIndentation.startsWith("-") &&
                lineWithIndentation.containsOnly('-', '|', ' ')
            ) {
                // Horizontal rule or table row
                // ----------------
                //
                // cell1 | cell2
                // ------|------
                // cell3 | cell4
                newParagraph().block = true
                appendText(lineWithIndentation)
                newParagraph().block = true
            } else if (lineWithIndentation.startsWith("=") &&
                lineWithIndentation.containsOnly('=', ' ')
            ) {
                // Header
                // ======
                newParagraph().block = true
                appendText(lineWithIndentation)
                newParagraph().block = true
            } else if (lineWithIndentation.startsWith("#")) {
                // ## Header
                newParagraph().block = true
                appendText(lineWithIndentation)
                newParagraph().block = true
            } else if (lineWithIndentation.startsWith("*") &&
                lineWithIndentation.containsOnly('*', ' ')
            ) {
                // Horizontal rule:
                // *******
                // * * *
                newParagraph().block = true
                appendText(lineWithIndentation)
                newParagraph().block = true
            } else if (lineWithoutIndentation.startsWith("```")) {
                i = addPreformatted(i - 1) { it.startsWith("```") }
            } else if (lineWithoutIndentation.startsWith("<pre>", ignoreCase = true)) {
                i = addPreformatted(i - 1) { it.startsWith("</pre>", ignoreCase = true) }
            } else if (lineWithIndentation.startsWith("    ")) { // markdown preformatted text
                i = addPreformatted(i - 1) { !it.startsWith(" ") }
            } else if (lineWithIndentation.startsWith("> ")) {
                val paragraph = newParagraph()
                paragraph.quoted = true
                paragraph.block = false
                i = addLines(
                    i - 1,
                    until = { _, w, s -> s.isBlank() || w.isListItem() || s.isKDocTag() || s.isTodo() },
                    customize = { _, p -> p.quoted = true }
                )
                newParagraph()
            } else if (lineWithoutIndentation.equals("<ul>", true) ||
                lineWithoutIndentation.equals("<ol>", true)
            ) {
                newParagraph().block = true
                appendText(lineWithoutIndentation)
                newParagraph().hanging = true
                i = addLines(
                    i, includeEnd = true,
                    until = { _, w, _ ->
                        w.equals("</ul>", true) ||
                            w.equals("</ol>", true)
                    },
                    customize = { _, p -> p.block = true },
                    shouldBreak = { w, _ ->
                        w.startsWith("<li>", true) ||
                            w.startsWith("</ul>", true) ||
                            w.startsWith("</ol>", true)
                    }
                )
                newParagraph()
            } else if (lineWithoutIndentation.isListItem() || lineWithoutIndentation.isKDocTag() || lineWithoutIndentation.isTodo()) {
                newParagraph().hanging = true
                val start = i
                i = addLines(
                    i - 1, includeEnd = false,
                    until = { j: Int, w: String, s: String ->
                        // See if it's a line continuation
                        if (s.isBlank() && j < lines.size - 1 &&
                            lineContent(lines[j + 1]).startsWith(" ")
                        ) {
                            false
                        } else {
                            s.isBlank() || w.isListItem() || s.isKDocTag() || s.isTodo()
                        }
                    },
                    shouldBreak = { w, _ ->
                        w.isBlank()
                    },
                    customize = { j, p ->
                        if (lineContent(lines[j]).isBlank() && j >= start) {
                            p.hanging = true
                            p.continuation = true
                        }
                    }
                )
                newParagraph()
            } else if (lineWithoutIndentation.isEmpty()) {
                newParagraph().separate = true
            } else if (lineWithoutIndentation.isTodo()) {
                newParagraph().hanging = true
                appendText(lineWithoutIndentation).appendText(" ")
            } else {
                // Some common HTML block tags
                if (lineWithoutIndentation.startsWith("<p>", true) ||
                    lineWithoutIndentation.startsWith("<p/>", true) ||
                    lineWithoutIndentation.startsWith("<h1>", true) ||
                    lineWithoutIndentation.startsWith("<h2>", true) ||
                    lineWithoutIndentation.startsWith("<h3>", true) ||
                    lineWithoutIndentation.startsWith("<h4>", true) ||
                    lineWithoutIndentation.startsWith("<table>", true) ||
                    lineWithoutIndentation.startsWith("<tr>", true) ||
                    lineWithoutIndentation.startsWith("<div>", true)
                ) {
                    val prevEmpty = paragraph.isEmpty()
                    newParagraph().block = true
                    if (lineWithoutIndentation.equals("<p>", true) || lineWithoutIndentation.equals("<p/>", true) ||
                        options.convertMarkup && lineWithoutIndentation.equals("</p>", true)
                    ) {
                        if (options.convertMarkup) {
                            // Replace <p> with a blank line
                            if (!prevEmpty) {
                                paragraph.separate = true
                            }
                        } else {
                            appendText(lineWithIndentation)
                            newParagraph().block = true
                        }
                        continue
                    } else if (lineWithoutIndentation.endsWith("</h1>", true) ||
                        lineWithoutIndentation.endsWith("</h2>", true) ||
                        lineWithoutIndentation.endsWith("</h3>", true) ||
                        lineWithoutIndentation.endsWith("</h4>", true)
                    ) {
                        if (lineWithoutIndentation.startsWith("<h", true) && options.convertMarkup &&
                            paragraph.isEmpty()
                        ) {
                            paragraph.separate = true
                            val count = lineWithoutIndentation[lineWithoutIndentation.length - 2] - '0'
                            for (j in 0 until count.coerceAtLeast(0).coerceAtMost(8)) {
                                appendText("#")
                            }
                            appendText(" ")
                            appendText(lineWithoutIndentation.substring(4, lineWithoutIndentation.length - 5))
                        } else if (options.collapseSpaces) {
                            appendText(lineWithoutIndentation.collapseSpaces())
                        } else {
                            appendText(lineWithoutIndentation)
                        }
                        newParagraph().block = true
                        continue
                    }
                }
                if (options.collapseSpaces) {
                    appendText(lineWithoutIndentation.collapseSpaces())
                } else {
                    appendText(lineWithoutIndentation)
                }
                appendText(" ")
            }
        }

        closeParagraph()
        arrange()
        if (!lineComment) {
            punctuate()
        }

        return ParagraphList(paragraphs)
    }

    /**
     * Make a pass over the paragraphs and make sure that we (for
     * example) place blank lines around preformatted text
     */
    private fun arrange() {
        var prev: Paragraph? = null
        for (paragraph in paragraphs) {
            paragraph.cleanup()
            val text = paragraph.text
            paragraph.separate = when {
                prev == null -> false
                paragraph.separate -> true
                // Don't separate kdoc tags, except for the first one
                paragraph.doc -> !prev.doc
                text.isTodo() && !prev.text.isTodo() -> true
                text.startsWith("#") -> true // header
                // Set preformatted paragraphs off (but not <pre> tags where it's implicit)
                paragraph.preformatted -> !prev.preformatted && !text.startsWith("<pre", true)
                !paragraph.preformatted && prev.preformatted && prev.text.startsWith("</pre>", true) -> false
                paragraph.continuation -> true
                paragraph.hanging -> false
                paragraph.quoted -> false
                text.startsWith("#") || text.startsWith("<h", true) -> true
                else -> !paragraph.block && !paragraph.isEmpty()
            }

            if (paragraph.hanging) {
                if (paragraph.doc || text.startsWith("<li>", true) || text.isTodo()) {
                    paragraph.hangingIndent = getIndent(options.hangingIndent)
                } else if (paragraph.continuation && paragraph.prev != null) {
                    paragraph.hangingIndent = paragraph.prev!!.hangingIndent
                    // Dedent to match hanging indent
                    val s = paragraph.text.trimStart()
                    paragraph.content.clear()
                    paragraph.content.append(s)
                } else {
                    paragraph.hangingIndent = getIndent(text.indexOf(' ') + 1)
                }
            }
            prev = paragraph
        }

        // Trim blank lines from the end
        for (i in paragraphs.size - 1 downTo 0) {
            if (paragraphs[i].isEmpty()) {
                paragraphs.removeAt(i)
                if (i > 0) {
                    paragraphs[i - 1].next = null
                }
            } else {
                break
            }
        }
    }

    private fun punctuate() {
        if (!options.addPunctuation) {
            return
        }
        val last = paragraphs.last()
        if (last.preformatted || last.doc || last.hanging && !last.continuation || last.isEmpty()) {
            return
        }
        val text = last.content
        for (i in text.length - 1 downTo 0) {
            val c = text[i]
            if (c.isWhitespace()) {
                continue
            }
            if ((c.isLetterOrDigit() || c.isCloseSquareBracket()) && text[0].isUpperCaseLetter()) {
                text.setLength(i + 1)
                text.append('.')
            }
            break
        }
    }
}

fun String.containsOnly(vararg s: Char): Boolean {
    for (c in this) {
        if (s.none { it == c }) {
            return false
        }
    }
    return true
}

fun Char.isUpperCaseLetter() = isLetter() && isUpperCase()

fun Char.isCloseSquareBracket() = this == ']'
