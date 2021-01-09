package kdocformatter

/** Options controlling how the [KDocFormatter] will behave */
class KDocFormattingOptions(maxLineWidth: Int = 72, maxCommentWidth: Int = Integer.MAX_VALUE) {
    /** Right hand side margin to write lines at */
    @Suppress("CanBePrimaryConstructorProperty")
    var maxLineWidth: Int = maxLineWidth

    /**
     * Limit comment to be at most [maxCommentWidth] characters even if
     * more would fit on the line
     */
    @Suppress("CanBePrimaryConstructorProperty")
    var maxCommentWidth: Int = maxCommentWidth

    /**
     * Whether to collapse multi-line comments that would fit on a
     * single line into a single line
     */
    var collapseSingleLine: Boolean = true

    /** Whether to collapse repeated spaces */
    var collapseSpaces: Boolean = true

    /**
     * Whether to convert basic markup like **bold** into **bold**, <
     * into <, etc.
     */
    var convertMarkup: Boolean = true

    /**
     * How many spaces to use for hanging indents in numbered lists and
     * after block tags
     */
    var hangingIndent: Int = 4

    /**
     * Don't format with tabs! (See
     * https://kotlinlang.org/docs/reference/coding-conventions.html#formatting)
     *
     * But if you do, this is the tab width.
     */
    var tabWidth: Int = 8

    /** Creates a copy of this formatting object */
    fun copy(): KDocFormattingOptions {
        val copy = KDocFormattingOptions()
        copy.maxLineWidth = maxLineWidth
        copy.maxCommentWidth = maxCommentWidth
        copy.collapseSingleLine = collapseSingleLine
        copy.collapseSpaces = collapseSpaces
        copy.hangingIndent = hangingIndent
        copy.tabWidth = tabWidth
        return copy
    }
}
