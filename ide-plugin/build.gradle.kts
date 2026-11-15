import java.util.Properties
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private fun properties(key: String) = project.findProperty(key).toString()

plugins {
  id("java")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.intellij.platform)
  alias(libs.plugins.changelog)
  alias(libs.plugins.android.lint)
  alias(libs.plugins.ktfmt)
}

val pluginVersion: String =
    Properties()
        .apply { load(file("../library/src/main/resources/version.properties").inputStream()) }
        .getProperty("buildVersion")

group = properties("pluginGroup")

version = pluginVersion

repositories {
  google()
  mavenCentral()
  intellijPlatform { defaultRepositories() }
}

intellijPlatform {
  pluginConfiguration { name = properties("pluginName") }
  // `publishPlugin` uploads to the JetBrains Marketplace; the release workflow passes the
  // token from the PUBLISH_TOKEN repository secret.
  publishing { token = providers.environmentVariable("PUBLISH_TOKEN") }
  // `verifyPlugin` checks the plugin against each of these IDEs (the oldest and newest
  // supported releases), catching uses of APIs that are missing or removed there.
  pluginVerification {
    ides {
      properties("pluginVerifierIdeVersions").split(',').forEach {
        create(properties("platformType"), it.trim())
      }
    }
  }
}

changelog {
  version.set(pluginVersion)
  groups.set(emptyList())
  repositoryUrl.set(properties("pluginRepositoryUrl"))
}

tasks {
  properties("javaVersion").let {
    withType<JavaCompile> {
      sourceCompatibility = it
      targetCompatibility = it
    }
    withType<KotlinCompile> { compilerOptions.jvmTarget.set(JvmTarget.fromTarget(it)) }
  }

  patchPluginXml {
    version = pluginVersion.get()
    sinceBuild.set(properties("pluginSinceBuild"))
    untilBuild.set(properties("pluginUntilBuild"))

    pluginDescription.set(
        projectDir
            .resolve("README.md")
            .readText()
            .lines()
            .run {
              val start = "<!-- Plugin description -->"
              val end = "<!-- Plugin description end -->"

              if (!containsAll(listOf(start, end))) {
                throw GradleException(
                    "Plugin description section not found in README.md:\n$start ... $end")
              }
              subList(indexOf(start) + 1, indexOf(end))
            }
            .joinToString("\n")
            .run { markdownToHTML(this) })

    // Get the latest available change notes from the changelog file
    changeNotes.set(
        provider {
          with(changelog) {
            renderItem(
                getOrNull(project.version.toString())
                    ?: runCatching { getLatest() }.getOrElse { getUnreleased() },
                Changelog.OutputType.HTML,
            )
          }
        })
  }

  // Read more: https://github.com/JetBrains/intellij-ui-test-robot
  val runIdeForUiTests by
      intellijPlatformTesting.runIde.registering {
        task {
          jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Drobot-server.port=8082",
                "-Dide.mac.message.dialogs.as.sheets=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
                "-Djb.consents.confirmation.enabled=false",
            )
          }
        }

        plugins { robotServerPlugin() }
      }
}

lint {
  textReport = true
  baseline = file("lint-baseline.xml")
}

dependencies {
  // IntelliJ ships its own Kotlin standard library; bundling another copy can cause
  // ClassNotFoundException or version mismatches at runtime.
  implementation(project(":library")) { exclude(group = "org.jetbrains.kotlin") }
  intellijPlatform {
    create(properties("platformType"), properties("platformVersion"))
    bundledPlugins(properties("platformPlugins").split(','))
    pluginVerifier()
  }
}

defaultTasks("buildPlugin")
