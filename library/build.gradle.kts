plugins {
  id("java")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.android.lint)
  alias(libs.plugins.ktfmt)
}

group = "kdoc-formatter"

version = rootProject.extra["buildVersion"].toString()

repositories {
  google()
  mavenCentral()
  gradlePluginPortal()
}

configure<com.android.build.api.dsl.Lint> {
  disable.add("JavaPluginLanguageLevel")
  textReport = true
}

dependencies {
  implementation(libs.kotlin.stdlib)
  testImplementation(libs.junit4)
  testImplementation(libs.truth)
}

java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }

kotlin { jvmToolchain(17) }
