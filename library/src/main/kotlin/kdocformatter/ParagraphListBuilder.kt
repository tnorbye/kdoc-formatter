package kdocformatter

class ParagraphListBuilder(comment: String, private val options: KDocFormattingOptions) {
    private val lineComment: Boolean = comment.startsWith("//")
    private val paragraphs: MutableList<Paragraph> = mutableListOf()
    private val lines =
        if (lineComment) {
            comment.split("\n")
        } else {
            comment.removePrefix("/**").removeSuffix("*/").trim().split("\n")
        }

    private fun lineContent(line: String): String {
        val trimmed = line.trim()
        return when {
            lineComment && trimmed.startsWith("// ") -> trimmed.substring(3)
            lineComment && trimmed.startsWith("//") -> trimmed.substring(2)
            trimmed.startsWith("* ") -> trimmed.substring(2)
            trimmed.startsWith("*") -> trimmed.substring(1)
            else -> trimmed
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
            text.isDirectiveMarker() -> {
                paragraph.block = true
                paragraph.preformatted = true
            }
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
        separator: String = " "
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

            if (lineWithIndentation.isQuoted()) {
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
        includeEnd: Boolean = true,
        until: (String) -> Boolean = { true }
    ): Int {
        newParagraph()
        var j = i
        while (j < lines.size) {
            val l = lines[j]
            val lineWithIndentation = lineContent(l)
            if (!includeEnd && j > i && until(lineWithIndentation)) {
                stripTrailingBlankLines()
                return j
            }
            appendText(lineWithIndentation)
            paragraph.preformatted = true
            paragraph.allowEmpty = true
            newParagraph()
            if (includeEnd && j > i && until(lineWithIndentation)) {
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

    fun scan(indentSize: Int): ParagraphList {
        var i = 0
        while (i < lines.size) {
            val l = lines[i++]
            val lineWithIndentation = lineContent(l)
            val lineWithoutIndentation = lineWithIndentation.trim()

            fun newParagraph(i: Int): Paragraph {
                val paragraph = this.newParagraph()

                if (i >= 0 && i < lines.size) {
                    if (lines[i] == l) {
                        paragraph.originalIndent =
                            lineWithIndentation.length - lineWithoutIndentation.length
                    } else {
                        // We've looked ahead, e.g. when adding lists etc
                        val line = lineContent(lines[i])
                        val trimmed = line.trim()
                        paragraph.originalIndent = line.length - trimmed.length
                    }
                }
                return paragraph
            }

            if (lineWithIndentation.startsWith("    ") && // markdown preformatted text
                (i == 1 || lineContent(lines[i - 2]).isBlank()) && // we've already ++'ed i above
                    // Make sure it's not just deeply indented inside a different block
                    (paragraph.prev == null ||
                        lineWithIndentation.length - lineWithoutIndentation.length >=
                            paragraph.prev!!.originalIndent + 4)
            ) {
                i = addPreformatted(i - 1, includeEnd = false) { !it.startsWith(" ") }
            } else if (lineWithoutIndentation.startsWith("-") &&
                    lineWithoutIndentation.containsOnly('-', '|', ' ')
            ) {
                // Horizontal rule or table row (or potentially a line under a header)
                // ----------------
                //
                // cell1 | cell2
                // ------|------
                // cell3 | cell4
                newParagraph(i - 1).block = true
                appendText(lineWithoutIndentation)
                newParagraph(i).block = true
            } else if (lineWithoutIndentation.startsWith("=") &&
                    lineWithoutIndentation.containsOnly('=', ' ')
            ) {
                // Header
                // ======
                newParagraph(i - 1).block = true
                appendText(lineWithoutIndentation)
                newParagraph(i).block = true
            } else if (lineWithoutIndentation.startsWith("#")
            ) { // not isHeader() because <h> is handled separately
                // ## Header
                newParagraph(i - 1).block = true
                appendText(lineWithoutIndentation)
                newParagraph(i).block = true
            } else if (lineWithoutIndentation.startsWith("*") &&
                    lineWithoutIndentation.containsOnly('*', ' ')
            ) {
                // Horizontal rule:
                // *******
                // * * *
                newParagraph(i - 1).block = true
                appendText(lineWithoutIndentation)
                newParagraph(i).block = true
            } else if (lineWithoutIndentation.startsWith("```")) {
                i = addPreformatted(i - 1) { it.startsWith("```") }
            } else if (lineWithoutIndentation.startsWith("<pre>", ignoreCase = true)) {
                i = addPreformatted(i - 1) { it.startsWith("</pre>", ignoreCase = true) }
            } else if (lineWithoutIndentation.isQuoted()) {
                i--
                val paragraph = newParagraph(i)
                paragraph.quoted = true
                paragraph.block = false
                i =
                    addLines(
                        i,
                        until = { _, w, _ ->
                            w.isBlank() ||
                                w.isListItem() ||
                                w.isKDocTag() ||
                                w.isTodo() ||
                                w.isDirectiveMarker() ||
                                w.isHeader()
                        },
                        customize = { _, p -> p.quoted = true },
                        includeEnd = false
                    )
                newParagraph(i)
            } else if (lineWithoutIndentation.equals("<ul>", true) ||
                    lineWithoutIndentation.equals("<ol>", true)
            ) {
                newParagraph(i - 1).block = true
                appendText(lineWithoutIndentation)
                newParagraph(i).hanging = true
                i =
                    addLines(
                        i,
                        includeEnd = true,
                        until = { _, w, _ -> w.equals("</ul>", true) || w.equals("</ol>", true) },
                        customize = { _, p -> p.block = true },
                        shouldBreak = { w, _ ->
                            w.startsWith("<li>", true) ||
                                w.startsWith("</ul>", true) ||
                                w.startsWith("</ol>", true)
                        }
                    )
                newParagraph(i)
            } else if (lineWithoutIndentation.isListItem() ||
                    lineWithoutIndentation.isKDocTag() ||
                    lineWithoutIndentation.isTodo()
            ) {
                i--
                newParagraph(i).hanging = true
                val start = i
                i =
                    addLines(
                        i,
                        includeEnd = false,
                        until = { j: Int, w: String, s: String ->
                            // See if it's a line continuation
                            if (s.isBlank() &&
                                    j < lines.size - 1 &&
                                    lineContent(lines[j + 1]).startsWith(" ")
                            ) {
                                false
                            } else {
                                s.isBlank() ||
                                    w.isListItem() ||
                                    w.isQuoted() ||
                                    w.isKDocTag() ||
                                    w.isTodo() ||
                                    s.startsWith("```") ||
                                    w.startsWith("<pre>") ||
                                    w.isDirectiveMarker() ||
                                    w.isHeader() ||
                                    // Not indented by at least two spaces following a blank line?
                                    s.length > 2 &&
                                        (!s[0].isWhitespace() || !s[1].isWhitespace()) &&
                                        j < lines.size - 1 &&
                                        lineContent(lines[j - 1]).isBlank()
                            }
                        },
                        shouldBreak = { w, _ -> w.isBlank() },
                        customize = { j, p ->
                            if (lineContent(lines[j]).isBlank() && j >= start) {
                                p.hanging = true
                                p.continuation = true
                            }
                        }
                    )
                newParagraph(i)
            } else if (lineWithoutIndentation.isEmpty()) {
                newParagraph(i).separate = true
            } else if (lineWithoutIndentation.isTodo()) {
                newParagraph(i - 1).hanging = true
                appendText(lineWithoutIndentation).appendText(" ")
            } else if (lineWithoutIndentation.isDirectiveMarker()) {
                newParagraph(i - 1)
                appendText(lineWithoutIndentation)
                newParagraph(i).block = true
            } else {
                if (lineWithoutIndentation.indexOf('|') != -1 &&
                        paragraph.isEmpty() &&
                        (i < 2 || !lines[i - 2].contains("---"))
                ) {
                    val result = Table.getTable(lines, i - 1, ::lineContent)
                    if (result != null) {
                        val (table, nextRow) = result
                        val content =
                            if (options.alignTableColumns) {
                                // Only considering maxLineWidth here, not maxCommentWidth; we
                                // cannot break table lines, only adjust tabbing, and a padded
                                // table seems more readable
                                // (maxCommentWidth < maxLineWidth is there to prevent long lines
                                // for readability)
                                table.format(options.maxLineWidth - indentSize - 3)
                            } else {
                                table.original()
                            }
                        for (index in content.indices) {
                            val line = content[index]
                            appendText(line)
                            paragraph.separate = index == 0
                            paragraph.block = true
                            paragraph.table = true
                            newParagraph(-1)
                        }
                        i = nextRow
                        newParagraph(i)
                        continue
                    }
                }

                // Some common HTML block tags
                if (lineWithoutIndentation.startsWith("<") &&
                        (lineWithoutIndentation.startsWith("<p>", true) ||
                            lineWithoutIndentation.startsWith("<p/>", true) ||
                            lineWithoutIndentation.startsWith("<h1", true) ||
                            lineWithoutIndentation.startsWith("<h2", true) ||
                            lineWithoutIndentation.startsWith("<h3", true) ||
                            lineWithoutIndentation.startsWith("<h4", true) ||
                            lineWithoutIndentation.startsWith("<table", true) ||
                            lineWithoutIndentation.startsWith("<tr", true) ||
                            lineWithoutIndentation.startsWith("<caption", true) ||
                            lineWithoutIndentation.startsWith("<td", true) ||
                            lineWithoutIndentation.startsWith("<div", true))
                ) {
                    val prevEmpty = i > 1 && lineContent(lines[i - 2]).isBlank()
                    newParagraph(i - 1).block = true
                    if (lineWithoutIndentation.equals("<p>", true) ||
                            lineWithoutIndentation.equals("<p/>", true) ||
                            options.convertMarkup && lineWithoutIndentation.equals("</p>", true)
                    ) {
                        if (options.convertMarkup) {
                            // Replace <p> with a blank line
                            if (!prevEmpty) {
                                paragraph.separate = true
                            }
                        } else {
                            appendText(lineWithoutIndentation)
                            newParagraph(i).block = true
                        }
                        continue
                    } else if (lineWithoutIndentation.endsWith("</h1>", true) ||
                            lineWithoutIndentation.endsWith("</h2>", true) ||
                            lineWithoutIndentation.endsWith("</h3>", true) ||
                            lineWithoutIndentation.endsWith("</h4>", true)
                    ) {
                        if (lineWithoutIndentation.startsWith("<h", true) &&
                                options.convertMarkup &&
                                paragraph.isEmpty()
                        ) {
                            paragraph.separate = true
                            val count =
                                lineWithoutIndentation[lineWithoutIndentation.length - 2] - '0'
                            for (j in 0 until count.coerceAtLeast(0).coerceAtMost(8)) {
                                appendText("#")
                            }
                            appendText(" ")
                            appendText(
                                lineWithoutIndentation.substring(
                                    4,
                                    lineWithoutIndentation.length - 5
                                )
                            )
                        } else if (options.collapseSpaces) {
                            appendText(lineWithoutIndentation.collapseSpaces())
                        } else {
                            appendText(lineWithoutIndentation)
                        }
                        newParagraph(i).block = true
                        continue
                    }
                }
                val text =
                    if (options.collapseSpaces) lineWithoutIndentation.collapseSpaces()
                    else lineWithoutIndentation
                if (options.convertMarkup &&
                        (text.startsWith("<p>", true) || text.startsWith("<p/>", true))
                ) {
                    paragraph.separate = true
                    val stripped = text.substring(text.indexOf('>') + 1)
                    appendText(stripped)
                } else {
                    appendText(text)
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

    private fun docTagRank(tag: String): Int {
        // Canonical kdoc order -- https://kotlinlang.org/docs/kotlin-doc.html#block-tags
        // Full list in Dokka's sources: plugins/base/src/main/kotlin/parsers/Parser.kt
        return when {
            tag.startsWith("@param") -> 0
            tag.startsWith("@return") -> 1
            tag.startsWith("@constructor") -> 2
            tag.startsWith("@receiver") -> 3
            tag.startsWith("@property") -> 4
            tag.startsWith("@throws") -> 5
            tag.startsWith("@exception") -> 6
            tag.startsWith("@sample") -> 7
            tag.startsWith("@see") -> 8
            tag.startsWith("@author") -> 9
            tag.startsWith("@since") -> 10
            tag.startsWith("@suppress") -> 11
            tag.startsWith("@deprecated") -> 12
            else -> 100 // custom tags
        }
    }

    /**
     * Make a pass over the paragraphs and make sure that we (for
     * example) place blank lines around preformatted text.
     */
    private fun arrange() {
        var prev: Paragraph? = null

        if (options.orderDocTags && paragraphs.any { it.doc }) {
            val order = paragraphs.mapIndexed { index, paragraph -> paragraph to index }.toMap()
            val comparator =
                object : Comparator<Paragraph> {
                    override fun compare(p1: Paragraph, p2: Paragraph): Int {
                        val o1 = order[p1]!!
                        val o2 = order[p2]!!
                        if (p1.doc == p2.doc) {
                            if (p1.doc) {
                                // Sort @return after @param etc
                                val r1 = docTagRank(p1.text)
                                val r2 = docTagRank(p2.text)
                                if (r1 != r2) {
                                    return r1 - r2
                                }
                                // Within identical tags, preserve current order. In the future we
                                // could consider sorting parameters to match the order in the
                                // signature.
                                // (Not done now because we don't easily have access to it; it would
                                // require properly parsing Kotlin code, and kdoc-formatter doesn't
                                // have a dependency on the compiler currently.)
                            }
                            return o1 - o2
                        }
                        return if (p1.doc) 1 else -1
                    }
                }
            paragraphs.sortWith(comparator)
        }

        for (paragraph in paragraphs) {
            paragraph.cleanup()
            val text = paragraph.text
            paragraph.separate =
                when {
                    prev == null -> false
                    paragraph.preformatted && prev.preformatted -> false
                    paragraph.table ->
                        paragraph.separate && (!prev.block || prev.text.isKDocTag() || prev.table)
                    paragraph.separate -> true
                    // Don't separate kdoc tags, except for the first one
                    paragraph.doc -> !prev.doc
                    text.isDirectiveMarker() -> false
                    text.isTodo() && !prev.text.isTodo() -> true
                    text.isHeader() -> true
                    // Set preformatted paragraphs off (but not <pre> tags where it's implicit)
                    paragraph.preformatted ->
                        !prev.preformatted &&
                            !text.startsWith("<pre", true) &&
                            (!text.startsWith("```") || !prev.text.isExpectingMore())
                    prev.preformatted && prev.text.startsWith("</pre>", true) -> false
                    paragraph.continuation -> true
                    paragraph.hanging -> false
                    paragraph.quoted -> prev.quoted
                    text.isHeader() -> true
                    text.startsWith("<p>", true) || text.startsWith("<p/>", true) -> true
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

        if (prev == null) {
            return // empty list
        }

        val firstIndent = paragraphs[0].originalIndent
        if (firstIndent > 0) {
            for (paragraph in paragraphs) {
                if (paragraph.originalIndent <= firstIndent) {
                    paragraph.originalIndent = 0
                }
            }
        }

        // Handle nested lists
        var inList = paragraphs.firstOrNull()?.hanging ?: false
        var startIndent = 0
        var levels: MutableSet<Int>? = null
        for (i in 1 until paragraphs.size) {
            val paragraph = paragraphs[i]
            if (!inList) {
                if (paragraph.hanging) {
                    inList = true
                    startIndent = paragraph.originalIndent
                }
            } else {
                if (!paragraph.hanging) {
                    inList = false
                } else {
                    if (paragraph.originalIndent == startIndent) {
                        paragraph.originalIndent = 0
                    } else if (paragraph.originalIndent > 0) {
                        (levels ?: mutableSetOf<Int>().also { levels = it }).add(
                            paragraph.originalIndent
                        )
                    }
                }
            }
        }

        levels?.sorted()?.let { sorted ->
            val assignments = mutableMapOf<Int, Int>()
            for (i in sorted.indices) {
                assignments[sorted[i]] = (i + 1) * options.nestedListIndent
            }
            for (paragraph in paragraphs) {
                if (paragraph.originalIndent > 0) {
                    val assigned = assignments[paragraph.originalIndent] ?: continue
                    paragraph.originalIndent = assigned
                    paragraph.indent = getIndent(paragraph.originalIndent)
                }
            }
        }

        // Remove blank lines between list items and from the end
        for (i in paragraphs.size - 2 downTo 0) {
            if (paragraphs[i].isEmpty() && (!paragraphs[i].preformatted || i == paragraphs.size - 1)
            ) {
                paragraphs.removeAt(i)
                if (i > 0) {
                    paragraphs[i - 1].next = null
                }
            }
        }
    }

    private fun punctuate() {
        if (!options.addPunctuation || paragraphs.isEmpty()) {
            return
        }
        val last = paragraphs.last()
        if (last.preformatted || last.doc || last.hanging && !last.continuation || last.isEmpty()) {
            return
        }

        val text = last.content
        if (!text.startsWithUpperCaseLetter()) {
            return
        }

        for (i in text.length - 1 downTo 0) {
            val c = text[i]
            if (c.isWhitespace()) {
                continue
            }
            if (c.isLetterOrDigit() || c.isCloseSquareBracket()) {
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

fun StringBuilder.startsWithUpperCaseLetter() =
    this.isNotEmpty() && this[0].isUpperCase() && this[0].isLetter()

fun Char.isCloseSquareBracket() = this == ']'
