
pluginManagement {
    plugins {
        id("org.jetbrains.dokka") version("1.5.0")
    }
}

rootProject.name = "result-for-kotlin"

include(
    "core",
    "core-error"
)

