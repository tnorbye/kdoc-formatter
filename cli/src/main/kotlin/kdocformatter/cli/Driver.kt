/*
 * Copyright (C) 2022 Tor Norbye
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
