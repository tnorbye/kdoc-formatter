package kdocformatter.cli

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files

/**
 * Searches the current git commit for modified regions and returns
 * these.
 *
 * TODO: Allow specifying an arbitrary range of git sha's. This requires
 *     some work to figure out the correct line numbers
 *     in the current version from older patches.
 */
class GitRangeFilter private constructor(rangeMap: RangeMap) : LineRangeFilter(rangeMap) {
    companion object {
        fun create(gitPath: String, fileInRepository: File, staged: Boolean = false): GitRangeFilter? {
            val git = findGit(gitPath) ?: return null
            val args = mutableListOf<String>()
            val gitRepo = findGitRepo(fileInRepository) ?: return null
            val output = Files.createTempFile("gitshow", ".diff").toFile()
            args.add(git.path)
            args.add("--git-dir=$gitRepo")
            args.add("--no-pager")
            if (staged) {
                args.add("diff")
                args.add("--cached")
            } else {
                args.add("show")
            }
            args.add("--no-color")
            args.add("--no-prefix")
            args.add("--unified=0")
            args.add("--output=$output")
            if (!executeProcess(args)) {
                return null
            }
            val root = gitRepo.parentFile
            val diff = output.readText()
            return create(root, diff)
        }

        /**
         * Creates range from the given diff contents. Extracted to
         * be separate from the git invocation above for unit test
         * purposes.
         */
        fun create(root: File?, diff: String): GitRangeFilter {
            val rangeMap = RangeMap()
            var currentPath = root
            for (line in diff.split("\n")) {
                if (line.startsWith("+++ ")) {
                    val relative = line.substring(4)
                    // Canonicalize files here to match the canonicalization we perform in
                    // KDocFileFormattingOptions.parse (which is necessary such that we don't
                    // accidentally handle relative paths like "./" etc as "foo/./bar" which
                    // isn't treated as equal to "foo/bar").
                    currentPath = (if (root != null) File(root, relative) else File(relative)).canonicalFile
                } else if (line.startsWith("@@ ")) {
                    //noinspection FileComparisons
                    if (currentPath === root || currentPath == null || !currentPath.path.endsWith(".kt")) {
                        continue
                    }
                    val rangeStart = line.indexOf('+') + 1
                    val rangeEnd = line.indexOf(' ', rangeStart + 1)
                    val range = line.substring(rangeStart, rangeEnd)
                    val lineCountStart = range.indexOf(",")
                    val startLine: Int
                    val lineCount: Int
                    if (lineCountStart == -1) {
                        startLine = range.toInt()
                        lineCount = 1
                    } else {
                        startLine = range.substring(0, lineCountStart).toInt()
                        lineCount = range.substring(lineCountStart + 1).toInt()
                    }
                    rangeMap.addRange(currentPath, startLine, startLine + lineCount)
                }
            }

            return GitRangeFilter(rangeMap)
        }

        private fun executeProcess(args: List<String>): Boolean {
            try {
                val process = Runtime.getRuntime().exec(args.toTypedArray())
                val input = BufferedReader(InputStreamReader(process.inputStream))
                val error = BufferedReader(InputStreamReader(process.errorStream))
                val exitVal = process.waitFor()
                if (exitVal != 0) {
                    val sb = StringBuilder()
                    sb.append("Failed to run git command.\n")
                    sb.append("Command args:\n")
                    for (arg in args) {
                        sb.append("  ").append(arg).append("\n")
                    }
                    sb.append("Standard output:\n")
                    var line: String?
                    while (input.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                    sb.append("Error output:\n")
                    while (error.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                    input.close()
                    error.close()
                    System.err.println(sb.toString())
                    return false
                }
                return true
            } catch (t: Throwable) {
                val sb = StringBuilder()
                for (arg in args) {
                    sb.append("  ").append(arg).append("\n")
                }
                System.err.println(sb.toString())
                return false
            }
        }

        /**
         * Returns the .git folder for the [file] or directory somewhere
         * in the report
         */
        private fun findGitRepo(file: File): File? {
            var curr = file.absoluteFile
            while (true) {
                val git = File(curr, ".git")
                if (git.isDirectory) {
                    return git
                }
                curr = curr.parentFile ?: return null
            }
        }

        private fun findGit(gitPath: String): File? {
            if (gitPath.isNotEmpty()) {
                val file = File(gitPath)
                return if (file.exists()) {
                    file
                } else {
                    System.err.println("$gitPath does not exist")
                    null
                }
            }

            val git = findOnPath("git")
                ?: findOnPath("git.exe")
            if (git != null) {
                val gitFile = File(git)
                if (!gitFile.canExecute()) {
                    System.err.println("Cannot execute $gitFile")
                    return null
                }
                return gitFile
            } else {
                return null
            }
        }

        private fun findOnPath(target: String): String? {
            val path = System.getenv("PATH")?.split(File.pathSeparator) ?: return null
            for (binDir in path) {
                val file = File(binDir + File.separator + target)
                if (file.isFile) { // maybe file.canExecute() too but not sure how .bat files behave
                    return file.path
                }
            }
            return null
        }
    }
}
