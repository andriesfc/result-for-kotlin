pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(uri("https://plugins.gradle.org/m2/"))
    }
    plugins {
        kotlin("jvm").version("1.5.31")
    }
}