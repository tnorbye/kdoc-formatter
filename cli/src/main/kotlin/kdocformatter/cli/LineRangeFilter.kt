package kdocformatter.cli

import java.io.File
import kdocformatter.getLineNumber

open class LineRangeFilter protected constructor(private val rangeMap: RangeMap) : RangeFilter() {
    var valid: Boolean = false

    override fun isEmpty(): Boolean = rangeMap.isEmpty()

    override fun includes(file: File): Boolean {
        return rangeMap.getRanges(file).isNotEmpty()
    }

    override fun overlaps(file: File, source: String, startOffset: Int, endOffset: Int): Boolean {
        val startLine = getLineNumber(source, startOffset)
        val endLine = getLineNumber(source, endOffset, startLine, startOffset)
        val ranges = rangeMap.getRanges(file)
        for (range in ranges) {
            if (range.overlaps(startLine, endLine)) {
                return true
            }
        }

        return false
    }

    protected class Range(
        private val startLine: Int, // inclusive
        private val endLine: Int // exclusive
    ) {
        fun overlaps(startLine: Int, endLine: Int): Boolean {
            return this.startLine <= endLine && this.endLine >= startLine
        }

        override fun toString(): String {
            return "From $startLine to $endLine"
        }
    }

    protected class RangeMap {
        private val fileToRanges = HashMap<File, MutableList<Range>>()
        fun getRanges(file: File): List<Range> {
            return fileToRanges[file] ?: emptyList()
        }
        fun isEmpty(): Boolean {
            return fileToRanges.isEmpty()
        }
        fun addRange(file: File, startLine: Int, endLine: Int) {
            val list = fileToRanges[file] ?: ArrayList<Range>().also { fileToRanges[file] = it }
            list.add(Range(startLine, endLine))
        }
    }

    companion object {
        fun fromRangeStrings(file: File, rangeStrings: List<String>): LineRangeFilter {
            val rangeMap = RangeMap()

            val filter = LineRangeFilter(rangeMap)
            for (rangeString in rangeStrings) {
                for (s in rangeString.split(",")) {
                    val endSeparator = s.indexOf(':')
                    if (endSeparator == -1) {
                        val line = s.toIntOrNull() ?: error("Line $s is not a number")
                        rangeMap.addRange(file, line, line + 1)
                    } else {
                        val startString = s.substring(0, endSeparator)
                        val endString = s.substring(endSeparator + 1)
                        val start =
                            startString.toIntOrNull() ?: error("Line $startString is not a number")
                        val end =
                            endString.toIntOrNull() ?: error("Line $endString is not a number")
                        // ranges operate with endLine is exclusive, but command line option is
                        // inclusive
                        rangeMap.addRange(file, start, end + 1)
                    }
                }
            }
            filter.valid = true
            return LineRangeFilter(rangeMap)
        }
    }
}
