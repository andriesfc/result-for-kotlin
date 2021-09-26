
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(uri("https://plugins.gradle.org/m2/"))
    }
    plugins {
        id("org.jetbrains.dokka") version("1.5.31")
        kotlin("jvm").version("1.5.30")
    }
}

rootProject.name = "resultk"

include(
    "core",
    "core-error",
    "playground-app"
)
