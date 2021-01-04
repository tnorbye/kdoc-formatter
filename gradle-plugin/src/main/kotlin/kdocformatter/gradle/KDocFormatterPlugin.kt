package kdocformatter.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin

class KDocFormatterPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.add("kdocformatter", KDocFormatterExtension::class.java)
        project.tasks.register("format-kdoc", KDocFormatterTask::class.java) { task ->
            task.description = "Format the Kotlin source code with the kdoc-formatter"
            task.group = JavaBasePlugin.DOCUMENTATION_GROUP
        }
    }
}
