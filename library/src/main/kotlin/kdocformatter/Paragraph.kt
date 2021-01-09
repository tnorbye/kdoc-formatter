package kdocformatter

import kotlin.math.min

class Paragraph(var text: String, options: KDocFormattingOptions) {
    fun isBlockTag() = text.isKDocTag()
    fun isListItem() = listItem
    var separate = true
    val listItem = text.isListItem()
    var preformatted = text.startsWith("    ")
    var hangingIndent = if (isBlockTag()) getIndent(options.hangingIndent) else ""
    override fun toString(): String = text

    fun reflow(maxLineWidth: Int, options: KDocFormattingOptions): List<String> {
        val hangingIndentSize = getIndentSize(hangingIndent, options)
        if (text.length < (maxLineWidth - hangingIndentSize)) {
            return listOf(text.collapseSpaces())
        }
        // See divide & conquer algorithm listed here: https://xxyxyz.org/line-breaking/
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }.map { it.trim() }
        if (words.size == 1) {
            return listOf(words[0])
        }
        val lines = reflowDynamic(maxLineWidth, words)

        if (lines.size <= 2) {
            // Just 2 lines? We prefer long+short instead of half+half:
            return reflowGreedy(maxLineWidth, options)
        } else {
            // We could just return [lines] here, but the straightforward algorithm
            // doesn't do a great job with short paragraphs where the the last line
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
                    val remainingLines = reflowDynamic(maxLineWidth - hangingIndentSize, remainingWords)
                    return listOf(firstLine.toString().trim()) + remainingLines
                }

                return reflowDynamic(maxLineWidth - hangingIndentSize, words)
            }
            var lastWord = words.size - 1
            while (true) {
                // We can afford to do this because we're only repeating it for a single line's
                // worth of words and because comments tend to be relatively short anyway
                val newLines = reflowDynamic(maxLineWidth, words.subList(0, lastWord))
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

    private data class Quadruple(val i0: Int, val j0: Int, val i1: Int, val j1: Int)

    fun reflowDynamic(maxLineWidth: Int, words: List<String>): List<String> {
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
            } else
                big
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

    private fun reflowGreedy(lineWidth: Int, options: KDocFormattingOptions): List<String> {
        // Greedy implementation
        val lines = mutableListOf<String>()
        val isBlockTag = isBlockTag()
        val isListItem = isListItem()
        var offset = 0
        while (offset < text.length) {
            val isBeginning = offset == 0
            var width = lineWidth
            if (options.hangingIndent > 0 && (isBlockTag || isListItem) && !isBeginning) {
                width -= getIndentSize(hangingIndent, options)
            }
            while (offset < text.length && text[offset].isWhitespace()) {
                offset++
            }
            var last = offset + width
            if (last > text.length) {
                val remainder = text.substring(offset).trim()
                if (remainder.isNotEmpty()) {
                    lines.add(remainder)
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

                lines.add(text.substring(offset, last).trim())
                offset = last
            }
        }

        return lines
    }
}
