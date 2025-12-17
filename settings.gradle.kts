pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

rootProject.name = "kdoc-formatter"

include("cli")

include("library")

include(
    "ide-plugin")

// Including this in the same project causes Gradle sync
// to repeated sources, which makes the IDE experience poor.
// Instead, this is now a separate project.
// include("gradle-plugin")
