@file:JvmName("Main")

package kdocformatter.cli

import kdocformatter.KDocFormattingOptions
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println(usage())
        exitProcess(-1)
    }

    var i = 0

    var lineWidth = 70
    var singleLine = true
    var quiet = false
    var dryRun = false
    val files = mutableListOf<File>()
    while (i < args.size) {
        val arg = args[i]
        i++
        when (arg) {
            "--help", "-help", "-h" -> { println(usage()); exitProcess(0) }
            "--line-width" -> lineWidth = args[i++].toInt()
            "--single-line-comments" -> singleLine = true
            "--no-single-line-comments" -> singleLine = false
            "--dry-run", "-n" -> dryRun = true
            "--quiet", "-q" -> quiet = true
            else -> {
                val paths =
                    if (arg.startsWith("@")) {
                        val f = File(arg.substring(1))
                        if (!f.exists()) {
                            error("$f does not exist")
                        }
                        f.readText().split('\n').toList()
                    } else {
                        listOf(arg)
                    }
                for (path in paths) {
                    if (path.isBlank()) {
                        continue
                    }
                    val file = File(arg.trim())
                    if (!file.exists()) {
                        error("$file does not exist")
                    }
                    files.add(file)
                }
            }
        }
    }

    if (files.isEmpty()) {
        error("no files were provided")
    }

    val options = KDocFormattingOptions(lineWidth, singleLine)
    var count = 0
    for (file in files) {
        count += formatFile(file, options, dryRun)
    }

    if (!quiet) {
        println("Formatted $count files")
    }

    exitProcess(0)
}

private fun formatFile(file: File, options: KDocFormattingOptions, dryRun: Boolean): Int {
    if (file.isDirectory) {
        val name = file.name
        if (name.startsWith(".") && name != "." && name != "../") {
            // Skip .git and friends
            return 0
        }
        val files = file.listFiles() ?: return 0
        var count = 0
        for (f in files) {
            count += formatFile(f, options, dryRun)
        }
        return count
    }

    return if (file.path.endsWith(".kt")) {
        val original = file.readText()
        val reformatted = KDocFileFormatter(options).reformatFile(original)
        if (reformatted != original) {
            if (dryRun) {
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

fun error(message: String) {
    System.err.println(message)
    System.err.println()
    System.err.println(usage())
    exitProcess(-1)
}

fun usage(): String {
    return """
        Usage: kdoc-formatter [options] file(s)
        
        Options:
          --line-width
            Sets the length of lines. Defaults to 72.
          --single-line-comments
            Turns multi-line comments into a single lint if it fits.
          --single-line-comments
            Always creates multi-line comments, even for comments that would fit on a single line.
          --dry-run, -n
            Prints the paths of the files whose contents would change if the formatter were run normally.
          --help, -help, -h
            Print this usage statement.
          @<filename>
            Read filenames from file.
        
        kdoc-formatter: Version 1.0-SNAPSHOT
        https://github.com/tnorbye/kdoc-formatter        
    """.trimIndent()
}
