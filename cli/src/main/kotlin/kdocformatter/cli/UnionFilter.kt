package kdocformatter.cli

import java.io.File

class UnionFilter(private val filters: List<RangeFilter>) : RangeFilter() {
    override fun overlaps(file: File, source: String, startOffset: Int, endOffset: Int): Boolean {
        for (filter in filters) {
            if (filter.overlaps(file, source, startOffset, endOffset)) {
                return true
            }
        }
        return false
    }

    override fun includes(file: File): Boolean {
        for (filter in filters) {
            if (filter.includes(file)) {
                return true
            }
        }
        return false
    }

    override fun isEmpty(): Boolean {
        for (filter in filters) {
            if (!filter.isEmpty()) {
                return false
            }
        }
        return true
    }
}
