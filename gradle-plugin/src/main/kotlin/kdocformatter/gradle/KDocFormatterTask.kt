package kdocformatter.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskAction
import java.io.File
import kdocformatter.cli.KDocFileFormatter
import kdocformatter.cli.KDocFileFormattingOptions

open class KDocFormatterTask : DefaultTask() {
    @TaskAction
    fun action() {
        val convention = convention.findPlugin(JavaPluginConvention::class.java)
        val dirs = convention?.sourceSets?.map { it.allSource }?.map { it.outputDir }?.toList()
            ?: listOf(File("."))

        // TODO: Add extension to customize these options
        val options = KDocFileFormattingOptions()
        val formatter = KDocFileFormatter(options)
        var count = 0
        for (file in dirs) {
            count += formatter.formatFile(file)
        }

        if (!options.quiet) {
            println("Formatted $count files")
        }
    }
}
