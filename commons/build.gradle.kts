@file:Suppress("LocalVariableName", "SpellCheckingInspection")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.21"
    id("org.jetbrains.dokka") version "1.4.32"
    `java-library`
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
}

val artifactName = "resultk-commons"
val javaCompileLangVersion = JavaLanguageVersion.of("11")
val kotlinComileLangVersion = "1.5"
val isRelease by extra { !"$version".endsWith("-snapshot", ignoreCase = true) }

javaToolchains {
    compilerFor {
        languageVersion.set(javaCompileLangVersion)
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    /// Uses reflection in unit tests
    testImplementation(kotlin("reflect"))

    // JUnit5
    val junit5_version = "5.7.2"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5_version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit5_version")

    testImplementation("commons-codec:commons-codec:1.15")
    testImplementation("org.apache.commons:commons-text:1.9")

    // MockK
    val mockk_version = "1.11.0"
    testImplementation("io.mockk:mockk:$mockk_version")

    // Mockito (for Java based testing)
    testImplementation("org.mockito:mockito-core:3.11.0")

    // AssertK
    val assertk_version = "0.24"
    testImplementation("com.willowtreeapps.assertk:assertk:$assertk_version")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = javaCompileLangVersion.toString()
    kotlinOptions.apiVersion = kotlinComileLangVersion
    kotlinOptions.languageVersion = kotlinComileLangVersion
}


