@file:Suppress("LocalVariableName", "SpellCheckingInspection")

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.21"
    id("org.jetbrains.dokka") version "1.4.32"
    `java-library`
}


val javaCompileLangVersion = JavaLanguageVersion.of("11")
val kotlinComileLangVersion = "1.5"
val isRelease by extra { !"$version".endsWith("-snapshot", ignoreCase = true) }

javaToolchains {
    compilerFor {
        languageVersion.set(javaCompileLangVersion)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Still using reflection
    testImplementation("org.jetbrains.kotlin:kotlin-reflect")

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation(project(":lib"))

    // Logging
    val logback_version = "1.2.3"
    implementation("ch.qos.logback:logback-core:$logback_version")
    runtimeOnly("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.slf4j:slf4j-api:1.7.30")

    // JUnit5
    val junit5_version = "5.7.2"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5_version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit5_version")

    testImplementation("commons-codec:commons-codec:1.15")
    testImplementation("org.apache.commons:commons-text:1.9")

    // https://mvnrepository.com/artifact/org.springframework/spring-expression
    implementation("org.springframework:spring-expression:5.3.9")

    // MockK
    val mockk_version = "1.11.0"
    testImplementation("io.mockk:mockk:$mockk_version")

    // AssertK
    val assertk_version = "0.24"
    testImplementation("com.willowtreeapps.assertk:assertk:$assertk_version")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // PDF support
    implementation("org.apache.pdfbox:pdfbox:2.0.22")

}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = javaCompileLangVersion.toString()
        apiVersion = kotlinComileLangVersion
        languageVersion = kotlinComileLangVersion
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
}

