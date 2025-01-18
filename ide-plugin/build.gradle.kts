import java.util.Properties
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm")
  id("org.jetbrains.intellij") version "1.17.4"
  id("org.jetbrains.changelog") version "2.2.1"
  id("com.android.lint")
  id("com.ncorti.ktfmt.gradle")
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
}

intellij {
  pluginName.set(properties("pluginName"))
  version.set(properties("platformVersion"))
  type.set(properties("platformType"))
  plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
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
    version.set(pluginVersion)
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
                getOrNull(properties("pluginVersion"))
                    ?: runCatching { getLatest() }.getOrElse { getUnreleased() },
                Changelog.OutputType.HTML,
            )
          }
        })
  }

  // Configure UI tests plugin
  // Read more: https://github.com/JetBrains/intellij-ui-test-robot
  runIdeForUiTests {
    systemProperty("robot-server.port", "8082")
    systemProperty("ide.mac.message.dialogs.as.sheets", "false")
    systemProperty("jb.privacy.policy.text", "<!--999.999-->")
    systemProperty("jb.consents.confirmation.enabled", "false")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    dependsOn("patchChangelog")
    token.set(System.getenv("PUBLISH_TOKEN"))
    // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels,
    // like 2.1.7-alpha.3
    // Specify pre-release label to publish the plugin in a custom Release Channel automatically.
    // Read more:
    // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
    val channelName: String = pluginVersion.split('-')
      .getOrElse(1) { "default" }
      .split('.')
      .first()

    channels.set(listOf(channelName))
  }
}

lint {
  textReport = true
  baseline = file("lint-baseline.xml")
}

dependencies { implementation(project(":library")) }

defaultTasks("buildPlugin")
