package kdocformatter

import java.util.regex.Pattern
import kotlin.math.min

fun getIndent(width: Int): String {
    val sb = StringBuilder()
    for (i in 0 until width) {
        sb.append(' ')
    }
    return sb.toString()
}

fun getIndentSize(indent: String, options: KDocFormattingOptions): Int {
    var size = 0
    for (c in indent) {
        if (c == '\t') {
            size += options.tabWidth
        } else {
            size++
        }
    }
    return size
}

/** Returns line number (1-based) */
fun getLineNumber(source: String, offset: Int, startLine: Int = 1, startOffset: Int = 0): Int {
    var line = startLine
    for (i in startOffset until offset) {
        val c = source[i]
        if (c == '\n') {
            line++
        }
    }
    return line
}

private val numberPattern = Pattern.compile("^\\d+([.)]) ")

fun String.isListItem(): Boolean {
    return startsWith("- ") ||
        startsWith("* ") ||
        startsWith("+ ") ||
        firstOrNull()?.isDigit() == true && numberPattern.matcher(this).find() ||
        startsWith("<li>", ignoreCase = true)
}

fun String.collapseSpaces(): String {
    if (indexOf("  ") == -1) {
        return this.trimEnd()
    }
    val sb = StringBuilder()
    var prev: Char = this[0]
    for (i in indices) {
        if (prev == ' ') {
            if (this[i] == ' ') {
                continue
            }
        }
        sb.append(this[i])
        prev = this[i]
    }
    return sb.trimEnd().toString()
}

fun String.isTodo(): Boolean {
    return startsWith("TODO:") || startsWith("TODO(")
}

fun String.isHeader(): Boolean {
    return startsWith("#") || startsWith("<h", true)
}

fun String.isQuoted(): Boolean {
    return startsWith("> ")
}

fun String.isDirectiveMarker(): Boolean {
    return startsWith("<!---") || startsWith("-->")
}

/**
 * Returns true if the string ends with a symbol that implies more text
 * is coming, e.g. ":" or ","
 */
fun String.isExpectingMore(): Boolean {
    val last = lastOrNull() { !it.isWhitespace() } ?: return false
    return last == ':' || last == ','
}

/**
 * Does this String represent a divider line? (Markdown also requires
 * it to be surrounded by empty lines which has to be checked by the
 * caller)
 */
fun String.isLine(minCount: Int = 3): Boolean {
    return startsWith('-') && containsOnly('-', ' ') && count { it == '-' } >= minCount ||
        startsWith('_') && containsOnly('_', ' ') && count { it == '_' } >= minCount
}

fun String.isKDocTag(): Boolean {
    // Not using a hardcoded list here since tags can change over time
    if (startsWith("@")) {
        for (i in 1 until length) {
            val c = this[i]
            if (c.isWhitespace()) {
                return i > 2
            } else if (!c.isLetter() || !c.isLowerCase()) {
                return false
            }
        }
        return true
    }
    return false
}

/**
 * Attempt to preserve the caret position across reformatting. Returns
 * the delta in the new comment.
 */
fun findSamePosition(comment: String, delta: Int, reformattedComment: String): Int {
    // First see if the two comments are identical up to the delta; if so, same
    // new position
    for (i in 0 until min(comment.length, reformattedComment.length)) {
        if (i == delta) {
            return delta
        } else if (comment[i] != reformattedComment[i]) {
            break
        }
    }

    var i = comment.length - 1
    var j = reformattedComment.length - 1
    if (delta == i + 1) {
        return j + 1
    }
    while (i >= 0 && j >= 0) {
        if (i == delta) {
            return j
        }
        if (comment[i] != reformattedComment[j]) {
            break
        }
        i--
        j--
    }

    fun isSignificantChar(c: Char): Boolean = c.isWhitespace() || c == '*'

    // Finally it's somewhere in the middle; search by character skipping
    // over insignificant characters (space, *, etc)
    fun nextSignificantChar(s: String, from: Int): Int {
        var curr = from
        while (curr < s.length) {
            val c = s[curr]
            if (isSignificantChar(c)) {
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
        if (offset == delta) {
            return reformattedOffset
        }
        offset++
        reformattedOffset++
    }
    return reformattedOffset
}

// Until stdlib version is no longer experimental
fun <T, R : Comparable<R>> Iterable<T>.maxOf(selector: (T) -> R): R {
    val iterator = iterator()
    if (!iterator.hasNext()) throw NoSuchElementException()
    var maxValue = selector(iterator.next())
    while (iterator.hasNext()) {
        val v = selector(iterator.next())
        if (maxValue < v) {
            maxValue = v
        }
    }
    return maxValue
}
