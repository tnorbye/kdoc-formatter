plugins {
  id("java")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.ktfmt)
}

apply(from = "$rootDir/version.gradle")

group = "kdocformatter"

version = "1.0"

repositories {
  google()
  mavenCentral()
  gradlePluginPortal()
}

tasks.register("lint") { dependsOn(":library:lint", ":cli:lint", ":ide-plugin:lint") }

tasks.register("install") { dependsOn(":cli:installDist", "test") }

tasks.register("zip") { dependsOn(":cli:distZip", "test") }

tasks.register("runIde") { dependsOn(":ide-plugin:runIde") }

tasks.register("plugin") { dependsOn(":ide-plugin:buildPlugin") }

tasks.register("all") { dependsOn("clean", "install", "plugin", "zip") }

tasks.named<Delete>("clean") {
  doFirst {
    delete("$rootDir/m2")
    delete("$rootDir/gradle-plugin/build")
  }
}
