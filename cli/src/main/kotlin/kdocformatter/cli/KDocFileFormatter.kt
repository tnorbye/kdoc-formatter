package kdocformatter.cli

import java.io.File
import kdocformatter.EditorConfigs
import kdocformatter.KDocFormatter
import kdocformatter.KDocFormattingOptions

/**
 * This class attempts to iterate over an entire Kotlin source file
 * and reformat all the KDocs it finds within. This is based on some
 * light-weight lexical analysis to identify comments.
 */
class KDocFileFormatter(private val options: KDocFileFormattingOptions) {
    init {
        EditorConfigs.root = options.formattingOptions
    }

    /** Formats the given file or directory recursively. */
    fun formatFile(file: File): Int {
        if (file.isDirectory) {
            val name = file.name
            if (name.startsWith(".") && name != "." && name != "../") {
                // Skip .git and friends
                return 0
            }
            val files = file.listFiles() ?: return 0
            var count = 0
            for (f in files) {
                count += formatFile(f)
            }
            return count
        }

        val formatter = file.getFormatter() ?: return 0
        if (file.isFile && options.filter.includes(file)) {
            val original = file.readText()
            val reformatted = reformat(file, original, formatter)
            if (reformatted != original) {
                if (options.dryRun) {
                    println(file.path)
                } else {
                    if (options.verbose) {
                        println(file.path)
                    }
                    file.writeText(reformatted)
                }
                return 1
            }
        }
        return 0
    }

    private fun File.getFormatter(): ((String, KDocFormattingOptions, File?) -> String)? {
        return if (path.endsWith(".kt")) {
            ::reformatKotlinFile
        } else if (options.includeMd && (path.endsWith(".md") || path.endsWith(".md.html"))) {
            ::reformatMarkdownFile
        } else {
            null
        }
    }

    fun reformatFile(file: File, source: String): String {
        return reformat(file, source, file.getFormatter())
    }

    private fun reformat(
        file: File?,
        source: String,
        reformatter: ((String, KDocFormattingOptions, File?) -> String)? = file?.getFormatter()
    ): String {
        reformatter ?: return source
        val formattingOptions =
            file?.let { EditorConfigs.getOptions(it) } ?: options.formattingOptions
        return reformatter(source, formattingOptions, file)
    }

    /** Reformats the given Markdown file. */
    private fun reformatMarkdownFile(
        source: String,
        formattingOptions: KDocFormattingOptions,
        // Here such that this function has signature (String, KDocFormattingOptions, File?)
        @Suppress("UNUSED_PARAMETER") unused: File? = null
    ): String {
        // Just leverage the comment machinery here -- convert the markdown into a
        // kdoc comment, reformat that, and then uncomment it.
        // Note that this adds 3 characters to the line requirements (" * " on each line after the
        // opening "/**")
        // so we duplicate the options and pre-add 3 to account for this
        val formatter = KDocFormatter(formattingOptions.copy().apply { maxLineWidth += 3 })
        val comment =
            "/**\n" + source.split("\n").joinToString(separator = "\n") { " * $it" } + "\n*/"
        val reformattedComment =
            " " +
                formatter
                    .reformatComment(comment, "")
                    .trim()
                    .removePrefix("/**")
                    .removeSuffix("*/")
                    .trim()
        val reformatted =
            reformattedComment.split("\n").joinToString(separator = "\n") {
                if (it.startsWith(" * ")) {
                    it.substring(3)
                } else if (it.startsWith(" *")) {
                    ""
                } else if (it.startsWith("* ")) {
                    it.substring(2)
                } else {
                    it
                }
            }
        return reformatted
    }

    /**
     * Reformats the given Kotlin source file contents using the given
     * [formattingOptions]. The corresponding [file] can be consulted in
     * case we want to limit filtering to particular lines (for example
     * modified git lines)
     */
    private fun reformatKotlinFile(
        source: String,
        formattingOptions: KDocFormattingOptions,
        file: File? = null
    ): String {
        val sb = StringBuilder()
        val lexer = KotlinLexer(source)
        val tokens = lexer.tokenizeKotlin()
        val formatter = KDocFormatter(formattingOptions)
        val filter = options.filter
        var prev = 0
        for ((start, end) in tokens) {
            if (file == null || filter.overlaps(file, source, start, end)) {
                // Include all the non-comments between previous comment end and here
                val segment = source.substring(prev, start)
                sb.append(segment)
                prev = end

                val comment = source.substring(start, end)
                val originalIndent = getIndent(source, start)
                val suffix = !originalIndent.all { it.isWhitespace() }
                val indent =
                    if (suffix) {
                        val endIndex = originalIndent.indexOfFirst { !it.isWhitespace() }
                        originalIndent.substring(0, endIndex)
                    } else {
                        originalIndent
                    }

                val formatted =
                    try {
                        formattingOptions.orderedParameterNames =
                            lexer.getParameterNames(end) ?: emptyList()

                        val formatted = formatter.reformatComment(comment, indent)

                        // If it's the suffix of a line, see if it can fit there even when indented
                        // with the previous code
                        var addNewline = false
                        val reformatted =
                            if (suffix && !formatted.contains("\n")) {
                                val sameLineFormatted =
                                    formatter.reformatComment(comment, originalIndent)
                                if (sameLineFormatted.contains('\n')) {
                                    addNewline = true
                                    formatted
                                } else sameLineFormatted
                            } else if (suffix) {
                                addNewline = true
                                formatted
                            } else {
                                formatted
                            }
                        if (addNewline) {
                            // Remove trailing whitespace on the line (e.g. separator between code
                            // and /** */)
                            while (sb.isNotEmpty() && sb[sb.length - 1].isWhitespace()) {
                                sb.setLength(sb.length - 1)
                            }
                            sb.append('\n').append(indent)
                        }
                        reformatted
                    } catch (error: Throwable) {
                        System.err.println(
                            "Failed formatting comment in $file:\n\"\"\"\n$comment\n\"\"\""
                        )
                        throw error
                    }
                sb.append(formatted)
            }
        }
        sb.append(source.substring(prev, source.length))

        return sb.toString()
    }

    private fun getIndent(source: String, start: Int): String {
        var i = start - 1
        while (i >= 0 && source[i] != '\n') {
            i--
        }
        return source.substring(i + 1, start)
    }
}
