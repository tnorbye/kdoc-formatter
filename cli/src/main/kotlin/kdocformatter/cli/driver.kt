@file:JvmName("Main")

package kdocformatter.cli

import kdocformatter.cli.KDocFileFormattingOptions.Companion.usage
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println(usage())
        exitProcess(-1)
    }

    val options = KDocFileFormattingOptions.parse(args)
    val files = options.files
    if (files.isEmpty()) {
        error("no files were provided")
    } else if (options.filter.isEmpty()) {
        if (!options.quiet && (options.gitStaged || options.gitHead)) {
            println(
                "No changes to Kotlin files found in ${
                if (options.gitStaged) "the staged files" else "HEAD"
                }"
            )
        }
        exitProcess(0)
    }
    val formatter = KDocFileFormatter(options)

    var count = 0
    for (file in files) {
        count += formatter.formatFile(file)
    }

    if (!options.quiet) {
        println("Formatted $count files")
    }

    exitProcess(0)
}

fun error(message: String): Nothing {
    System.err.println(message)
    System.err.println()
    System.err.println(usage())
    exitProcess(-1)
}
