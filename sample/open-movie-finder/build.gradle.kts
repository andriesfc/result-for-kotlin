@file:Suppress("LocalVariableName", "SpellCheckingInspection")

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
    `java-library`
    application
}

val javaCompileVersion = JavaLanguageVersion.of("11")
val kotlinCompileVersion = "1.5"

javaToolchains {
    compilerFor { languageVersion.set(javaCompileVersion) }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // JUnit5
    val junit5_version = "5.7.2"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5_version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit5_version")

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

    // ResultK
    implementation("io.github.andriesfc.kotlin:resultk:1.0.0-SNAPSHOT")

}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        languageVersion = kotlinCompileVersion
        apiVersion = kotlinCompileVersion
        jvmTarget = javaCompileVersion.toString()
    }
}

application {
    mainClass.set("io.github.andriesfc.openmoviefinder.MovieFinderKt")
}