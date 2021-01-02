package kdocformatter.cli

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files

/**
 * Searches the current git commit for modified regions and returns
 * these
 */
class GitModifiedRange {
    private val fileToRanges = HashMap<File, MutableList<Range>>()

    fun compute(gitPath: String, fileInRepository: File): Boolean {
        fileToRanges.clear()
        val git = findGit(gitPath) ?: return false
        val args = mutableListOf<String>()
        val gitRepo = findGitRepo(fileInRepository) ?: return false
        val output = Files.createTempFile("gitshow", "diff").toFile()
        args.add(git.path)
        args.add("--git-dir=$gitRepo")
        args.add("--no-pager")
        args.add("show")
        args.add("--no-color")
        args.add("--no-prefix")
        args.add("--unified=0")
        args.add("--output=$output")
        if (!executeProcess(args)) {
            return false
        }
        val root = gitRepo.parentFile
        val diff = output.readText()
        var currentPath = root
        for (line in diff.split("\n")) {
            if (line.startsWith("+++ ")) {
                currentPath = File(root, line.substring(4))
            } else if (line.startsWith("@@ ")) {
                val lineStart = line.indexOf('+') + 1
                val lineEnd = line.indexOf(' ', lineStart + 1)
                val desc = line.substring(lineStart, lineEnd)
                val lineCountStart = desc.indexOf(",")
                val startLine: Int
                val lineCount: Int
                if (lineCountStart == -1) {
                    startLine = desc.toInt()
                    lineCount = 1
                } else {
                    startLine = desc.substring(0, lineCountStart).toInt()
                    lineCount = desc.substring(lineCountStart + 1).toInt()
                }
                val list = fileToRanges[currentPath] ?: ArrayList<Range>().also { fileToRanges[currentPath] = it }
                list.add(Range(startLine, startLine + lineCount))
            }
        }
        return true
    }

    fun isInChangedRange(file: File, startLine: Int, endLine: Int): Boolean {
        val ranges = fileToRanges[file] ?: return false
        for (range in ranges) {
            if (range.overlaps(startLine, endLine)) {
                return true
            }
        }

        return false
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

    companion object {
        /**
         * Returns the .git folder for the [file] or directory
         * somewhere in the report
         */
        fun findGitRepo(file: File): File? {
            var curr = file
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

private class Range(
    private val startLine: Int, // inclusive
    private val endLine: Int // exclusive
) {
    fun overlaps(startLine: Int, endLine: Int): Boolean {
        return this.startLine <= endLine && this.endLine >= startLine
    }
}
