@file:JvmName("Main")

package kdocformatter.cli

import kdocformatter.cli.KDocFileFormattingOptions.Companion.usage
import java.io.File
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
    }

    var count = 0
    for (file in files) {
        count += formatFile(file, options)
    }

    if (!options.quiet) {
        println("Formatted $count files")
    }

    exitProcess(0)
}

private fun formatFile(file: File, options: KDocFileFormattingOptions): Int {
    if (file.isDirectory) {
        val name = file.name
        if (name.startsWith(".") && name != "." && name != "../") {
            // Skip .git and friends
            return 0
        }
        val files = file.listFiles() ?: return 0
        var count = 0
        for (f in files) {
            count += formatFile(f, options)
        }
        return count
    }

    return if (file.path.endsWith(".kt")) {
        val original = file.readText()
        val reformatted = KDocFileFormatter(options).reformatFile(file, original)
        if (reformatted != original) {
            if (options.dryRun) {
                println(file.path)
            } else {
                file.writeText(reformatted)
            }
            1
        } else {
            0
        }
    } else {
        0
    }
}

fun error(message: String): Nothing {
    System.err.println(message)
    System.err.println()
    System.err.println(usage())
    exitProcess(-1)
}
