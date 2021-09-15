@file:Suppress("LocalVariableName", "SpellCheckingInspection")

import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    id("project.kotlin-library-conventions")
    id("org.jetbrains.dokka")
}

dependencies {
    testImplementation("commons-codec:commons-codec:1.15")
    testImplementation("org.apache.commons:commons-text:1.9")
}

tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
        configureEach {
            includes.from("Module.md")
        }
    }
}