package kdocformatter

import java.util.regex.Pattern

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
        if (c == '\t')
            size += options.tabWidth
        else
            size++
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

private val numberPattern = Pattern.compile("^\\d+\\. ")

fun String.isListItem(): Boolean {
    return startsWith("- ") || startsWith("* ") || startsWith("+ ") ||
        firstOrNull()?.isDigit() == true && numberPattern.matcher(this).find()
}

fun String.collapseSpaces(): String {
    if (indexOf("  ") == -1) {
        return this.trimEnd()
    }
    val sb = StringBuilder()
    var prev: Char = this[0]
    for (i in 0 until length) {
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

fun String.isKDocTag(): Boolean {
    if (!startsWith("@")) {
        return false
    }
    // Match actual set of kdoc tags instead of just "@something" such that we
    // don't accidentally interpret docs that contain for example annotation names
    // as block tags. (See KDocFormatterTest.testAtInMiddle for an example
    // which was affected by this.)
    // (See https://kotlinlang.org/docs/reference/kotlin-doc.html#block-tags)
    return startsWith("@param") ||
        startsWith("@param") ||
        startsWith("@return") ||
        startsWith("@constructor") ||
        startsWith("@receiver") ||
        startsWith("@property") ||
        startsWith("@throws") ||
        startsWith("@exception") ||
        startsWith("@sample") ||
        startsWith("@see") ||
        startsWith("@author") ||
        startsWith("@since") ||
        startsWith("@suppress") ||
        // Not an actual kdoc tag but might appear in converted docs
        startsWith("@deprecated")
}
