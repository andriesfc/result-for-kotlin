@file:Suppress("LocalVariableName", "SpellCheckingInspection")

import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    id("project.kotlin-library-conventions")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework:spring-expression:5.3.9")
    implementation(kotlin("reflect"))
}


tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
        configureEach {
            includes.from("Module.md")
        }
    }
}