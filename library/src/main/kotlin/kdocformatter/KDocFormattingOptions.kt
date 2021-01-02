package kdocformatter

/** Options controlling how the [KDocFormatter] will behave */
class KDocFormattingOptions(
    /** Right hand side margin to write lines at */
    val lineWidth: Int = 72,

    /**
     * Whether to collapse multi-line comments that would fit on a
     * single line into a single line
     */
    val collapseSingleLine: Boolean = true,

    /** Whether to collapse repeated spaces */
    val collapseSpaces: Boolean = true,

    /**
     * Whether to have hanging indents in numbered lists and after
     * block tags
     */
    val hangingIndents: Boolean = true,
) {
    /**
     * Don't format with tabs! (See
     * https://kotlinlang.org/docs/reference/coding-conventions.html#formatting)
     *
     * But if you do, this is the tab width.
     */
    var tabWidth: Int = 8
}
