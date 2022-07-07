package kdocformatter.gradle

import kdocformatter.cli.KDocFileFormatter
import kdocformatter.cli.KDocFileFormattingOptions
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskAction
import java.io.File

open class KDocFormatterTask : DefaultTask() {
    @TaskAction
    fun action() {
        val convention = convention.findPlugin(JavaPluginConvention::class.java)
        val dirs = convention?.sourceSets?.map { it.allSource }?.map { it.outputDir }?.toList()
            ?: listOf(File("."))

        val extension = project.extensions.findByName("kdocformatter") as KDocFormatterExtension
        val flags = extension.options
        val args = flags.split(" ").toTypedArray()
        val options = KDocFileFormattingOptions.parse(args)
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
