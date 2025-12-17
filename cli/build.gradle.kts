plugins {
  id("java")
  alias(libs.plugins.kotlin.jvm)
  id("application")
  alias(libs.plugins.android.lint)
  alias(libs.plugins.ktfmt)
}

group = "kdoc-formatter"

version = rootProject.extra["buildVersion"].toString()

application {
  mainClass = "kdocformatter.cli.Main"
  applicationName = "kdoc-formatter"
}

repositories {
  google()
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(libs.kotlin.stdlib)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testRuntimeOnly(libs.junit.jupiter.engine)
  implementation(project(":library"))
}

configure<com.android.build.api.dsl.Lint> {
  disable.add("JavaPluginLanguageLevel")
  textReport = true
}

tasks.test { useJUnitPlatform() }

java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }

kotlin { jvmToolchain(17) }
