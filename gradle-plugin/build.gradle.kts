buildscript {
  apply(from = "$rootDir/../version.gradle")

  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
  dependencies { classpath(libs.android.gradle.plugin) }
}

plugins {
  id("java")
  alias(libs.plugins.kotlin.jvm)
  id("java-gradle-plugin")
  id("maven-publish")
  alias(libs.plugins.gradle.plugin.publish)
  alias(libs.plugins.ktfmt)
}

// https://issues.sonatype.org/browse/OSSRH-63191
group = "com.github.tnorbye.kdoc-formatter"

version = rootProject.extra["buildVersion"].toString()

repositories {
  google()
  mavenCentral()
  gradlePluginPortal()
}

// Instead of depending on project(":cli") below, we inline the
// sources in the plugin such that we don't have to publish
// separate artifacts for the library and cli modules
sourceSets {
  main {
    java {
      srcDirs("../cli/src/main/kotlin")
      srcDirs("../library/src/main/kotlin")
    }
  }
}

dependencies {
  implementation(libs.kotlin.stdlib)
  implementation(gradleApi())
}

java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }

kotlin { jvmToolchain(17) }

gradlePlugin {
  website = "https://github.com/tnorbye/kdoc-formatter"
  vcsUrl = "https://github.com/tnorbye/kdoc-formatter.git"

  plugins {
    create("kdocformatter") {
      id = "kdoc-formatter"
      displayName = "KDoc Formatting"
      description = "Plugin which can reformat Kotlin KDoc comments"
      implementationClass = "kdocformatter.gradle.KDocFormatterPlugin"
      tags.set(listOf("kotlin", "kdoc", "formatter", "formatting"))
    }
  }
}

/*
publishing {
    publications {
        pluginPublication (MavenPublication) {
            from components.java
            groupId "com.github.tnorbye.kdoc-formatter"
            artifactId "kdoc-formatter"
            //version project.version
            version rootProject.ext.buildVersion
        }
    }
}
*/

publishing { repositories { maven { url = uri("../m2") } } }

tasks.named<Delete>("clean") { doFirst { delete("${rootDir}/../m2") } }

tasks.register("all") { dependsOn("clean", "publish") }
