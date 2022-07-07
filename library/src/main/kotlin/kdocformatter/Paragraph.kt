package kdocformatter

import kotlin.math.min

class Paragraph(private val options: KDocFormattingOptions) {
    var content = StringBuilder()
    val text get() = content.toString()
    var prev: Paragraph? = null
    var next: Paragraph? = null

    /** If true, this paragraph should be preceded by a blank line. */
    var separate = false

    /**
     * If true, this paragraph is a continuation of the previous
     * paragraph (so should be indented with the hanging indent,
     * including line 1)
     */
    var continuation = false

    /**
     * Whether this paragraph is allowed to be empty. Paragraphs are
     * normally merged if this is not set. This allows the line breaker
     * to call [ParagraphListBuilder.newParagraph] repeatedly without
     * introducing more than one new paragraph. But for preformatted
     * text we do want to be able to express repeated blank lines.
     */
    var allowEmpty = false

    /** Is this paragraph preformatted? */
    var preformatted = false

    /**
     * Is this paragraph a block paragraph? If so, it must start on its
     * own line
     */
    var block = false

    /** Is this paragraph specifying a kdoc tag like @param? */
    var doc = false

    /**
     * Is this line quoted? (In the future make this an int such that we
     * can support additional levels.)
     */
    var quoted = false

    /**
     * Should this paragraph use a hanging indent? (Implies [block] as
     * well).
     */
    var hanging = false
        set(value) {
            block = true
            field = value
        }

    var originalIndent = 0

    // The indent to use for all lines in the paragraph.
    var indent = ""

    // The indent to use for all lines in the paragraph if [hanging] is true,
    // or the second and subsequent lines if [hanging] is false
    var hangingIndent = ""

    fun isEmpty(): Boolean {
        return content.isEmpty()
    }

    fun cleanup() {
        if (preformatted || !options.convertMarkup) return
        val cleaned = convertTags(text)
        content.clear()
        content.append(cleaned)
    }

    private fun convertTags(s: String): String {
        if (s.none { it == '<' || it == '&' || it == '{' }) return s

        val sb = StringBuilder(s.length)
        var i = 0
        val n = s.length
        while (i < n) {
            val c = s[i++]
            if (c == '<') {
                if (s.startsWith("b>", i, true) || s.startsWith("/b>", i, true)) {
                    // "<b>" or </b> -> "**"
                    sb.append('*').append('*')
                    if (s[i] == '/') i++
                    i += 2
                    continue
                }
                if (s.startsWith("i>", i, true) || s.startsWith("/i>", i, true)) {
                    // "<i>" or </i> -> "*"
                    sb.append('*')
                    if (s[i] == '/') i++
                    i += 2
                    continue
                }
                if (s.startsWith("em>", i, true) || s.startsWith("/em>", i, true)) {
                    // "<em>" or </em> -> "_"
                    sb.append('_')
                    if (s[i] == '/') i++
                    i += 3
                    continue
                }
            } else if (c == '&') {
                if (s.startsWith("lt;", i, true)) { // "&lt;" -> "<"
                    sb.append('<')
                    i += 3
                    continue
                }
                if (s.startsWith("gt;", i, true)) { // "&gt;" -> ">"
                    sb.append('>')
                    i += 3
                    continue
                }
            } else if (c == '{') {
                if (s.startsWith("@link", i, true)) {
                    // {@link} or {@linkplain}
                    sb.append('[')
                    var curr = i + 5
                    while (curr < n) {
                        val ch = s[curr++]
                        if (ch.isWhitespace()) {
                            break
                        }
                        if (ch == '}') {
                            curr--
                            break
                        }
                    }
                    var skip = false
                    while (curr < n) {
                        val ch = s[curr]
                        if (ch == '}') {
                            sb.append(']')
                            curr++
                            break
                        } else if (ch == '(') {
                            skip = true
                        } else if (!skip) {
                            if (ch == '#') {
                                if (!sb.endsWith('[')) {
                                    sb.append('.')
                                }
                            } else {
                                sb.append(ch)
                            }
                        }
                        curr++
                    }
                    i = curr
                    continue
                }
            }
            sb.append(c)
        }

        return sb.toString()
    }

    fun reflow(maxLineWidth: Int, options: KDocFormattingOptions): List<String> {
        val maxLineWidth = maxLineWidth - getIndentSize(indent, options)
        val hangingIndentSize = getIndentSize(hangingIndent, options) - if (quoted) 2 else 0 // "> "
        if (text.length < (maxLineWidth - hangingIndentSize)) {
            return listOf(text.collapseSpaces())
        }
        // Split text into words
        val words = computeWords()

        // See divide & conquer algorithm listed here: https://xxyxyz.org/line-breaking/
        if (words.size == 1) {
            return listOf(words[0])
        }
        val lines = reflowOptimal(maxLineWidth, words)
        if (lines.size <= 2 || options.alternate || !options.optimal) {
            // Just 2 lines? We prefer long+short instead of half+half:
            return reflowGreedy(maxLineWidth, options, words)
        } else {
            // We could just return [lines] here, but the straightforward algorithm
            // doesn't do a great job with short paragraphs where the last line
            // is short; it over-corrects and shortens everything else in order
            // to balance out the last line.

            val maxLine: (String) -> Int = {
                // Ignore lines that are unbreakable
                if (it.indexOf(' ') == -1) {
                    0
                } else {
                    it.length
                }
            }
            val longestLine = lines.maxOf(maxLine)
            if (hangingIndentSize > 0 && words[0].length < maxLineWidth) {
                // Fill first line greedily since it's wider then reflow the rest optimally
                var i = 0
                val firstLine = StringBuilder()
                while (i < words.size) {
                    val word = words[i]
                    val newEnd = firstLine.length + word.length
                    if (newEnd == maxLineWidth) {
                        firstLine.append(word)
                        i++
                        break
                    } else if (newEnd > maxLineWidth) {
                        break
                    }
                    firstLine.append(word).append(' ')
                    i++
                }
                if (i > 0) {
                    val remainingWords = words.subList(i, words.size)
                    val remainingLines = reflowOptimal(maxLineWidth - hangingIndentSize, remainingWords)
                    return listOf(firstLine.toString().trim()) + remainingLines
                }

                return reflowOptimal(maxLineWidth - hangingIndentSize, words)
            }
            var lastWord = words.size - 1
            while (true) {
                // We can afford to do this because we're only repeating it for a single line's
                // worth of words and because comments tend to be relatively short anyway
                val newLines = reflowOptimal(maxLineWidth, words.subList(0, lastWord))
                if (newLines.size < lines.size) {
                    val newLongestLine = newLines.maxOf(maxLine)
                    if (newLongestLine > longestLine) {
                        return newLines + words.subList(lastWord, words.size).joinToString(" ")
                    }
                    break
                }
                lastWord--
            }

            return lines
        }
    }

    private fun computeWords(): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }.map { it.trim() }
        // See if any of the words should never be broken up. We do that for list separators
        // and a few others.
        // We never want to put "1." at the beginning of a line as an overflow
        val combined = ArrayList<String>(words.size)
        combined.add(words[0])
        var prev = ""
        var insideSquareBrackets = false
        for (i in 1 until words.size) {
            val word = words[i]

            if (prev.startsWith("[")) insideSquareBrackets = true
            if (prev.contains("]")) insideSquareBrackets = false

            // Can we start a new line with this without interpreting it
            // in a special way?
            if (word.startsWith("#") ||
                word.startsWith("-") ||
                word == "```" ||
                word.isListItem() && !word.equals("<li>", true) ||
                insideSquareBrackets
            ) {
                // Combine with previous word with a single space; the line breaking algorithm
                // won't know that it's more than one word.
                val joined = "$prev $word"
                combined.removeLast()
                combined.add(joined)
                prev = joined
            } else {
                combined.add(word)
                prev = word
            }
        }
        return combined
    }

    private data class Quadruple(val i0: Int, val j0: Int, val i1: Int, val j1: Int)

    private fun reflowOptimal(maxLineWidth: Int, words: List<String>): List<String> {
        val count = words.size
        val lines = ArrayList<String>()

        val offsets = ArrayList<Int>()
        offsets.add(0)

        for (boxWidth in words.map { it.length }.toList()) {
            offsets.add(offsets.last() + min(boxWidth, maxLineWidth))
        }

        val big = 10 shl 20
        val minimum = IntArray(count + 1) { big }
        val breaks = IntArray(count + 1)
        minimum[0] = 0

        fun cost(i: Int, j: Int): Int {
            val width = offsets[j] - offsets[i] + j - i - 1
            return if (width <= maxLineWidth) {
                val squared = (maxLineWidth - width) * (maxLineWidth - width)
                minimum[i] + squared
            } else {
                big
            }
        }

        fun search(pi0: Int, pj0: Int, pi1: Int, pj1: Int) {
            val stack = java.util.ArrayDeque<Quadruple>()
            stack.add(Quadruple(pi0, pj0, pi1, pj1))

            while (stack.isNotEmpty()) {
                val (i0, j0, i1, j1) = stack.removeLast()
                if (j0 < j1) {
                    val j = (j0 + j1) / 2

                    for (i in i0 until i1) {
                        val c = cost(i, j)
                        if (c <= minimum[j]) {
                            minimum[j] = c
                            breaks[j] = i
                        }
                    }
                    stack.add(Quadruple(breaks[j], j + 1, i1, j1))
                    stack.add(Quadruple(i0, j0, breaks[j] + 1, j))
                }
            }
        }

        var n = count + 1
        var i = 0
        var offset = 0

        while (true) {
            val r = min(n, 1 shl (i + 1))
            val edge = (1 shl i) + offset
            search(0 + offset, edge, edge, r + offset)
            val x = minimum[r - 1 + offset]
            var flag = true
            for (j in (1 shl i) until (r - 1)) {
                val y = cost(j + offset, r - 1 + offset)
                if (y <= x) {
                    n -= j
                    i = 0
                    offset += j
                    flag = false
                    break
                }
            }
            if (flag) {
                if (r == n) break
                i++
            }
        }

        var j = count
        while (j > 0) {
            i = breaks[j]
            val sb = StringBuilder()
            for (w in i until j) {
                sb.append(words[w])
                if (w < j - 1) {
                    sb.append(' ')
                }
            }
            lines.add(sb.toString())
            j = i
        }

        lines.reverse()
        return lines
    }

    private fun reflowGreedy(lineWidth: Int, options: KDocFormattingOptions, words: List<String>): List<String> {
        // Greedy implementation

        var width = lineWidth
        if (options.hangingIndent > 0 && hanging && continuation) {
            width -= getIndentSize(hangingIndent, options)
        }

        val lines = mutableListOf<String>()
        var column = 0
        val sb = StringBuilder()
        for (word in words) {
            when {
                sb.isEmpty() -> {
                    sb.append(word)
                    column += word.length
                }
                column + word.length + 1 <= width -> {
                    sb.append(' ').append(word)
                    column += word.length + 1
                }
                else -> {
                    width = lineWidth
                    if (options.hangingIndent > 0 && hanging && (lines.isNotEmpty() || continuation)) {
                        width -= getIndentSize(hangingIndent, options)
                    }
                    lines.add(sb.toString())
                    column = 0
                    sb.setLength(0)
                    sb.append(word)
                }
            }
        }
        if (sb.isNotEmpty()) {
            lines.add(sb.toString())
        }
        return lines
    }

    override fun toString(): String {
        return "$content, separate=$separate, block=$block, hanging=$hanging, preformatted=$preformatted, quoted=$quoted, continuation=$continuation, allowempty=$allowEmpty"
    }
}
