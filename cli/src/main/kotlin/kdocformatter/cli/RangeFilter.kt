package kdocformatter.cli

import java.io.File

/** Filter to decide whether given text regions should be included */
open class RangeFilter {
    /**
     * Return true if the range in [file] containing the contents
     * [source] overlaps the range from [startOffset] inclusive to
     * [endOffset] exclusive.
     */
    open fun overlaps(file: File, source: String, startOffset: Int, endOffset: Int): Boolean = true
}
