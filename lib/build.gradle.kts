@file:Suppress("LocalVariableName")

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // JUnit5
    val junit5_version = "5.7.2"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5_version")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")


}
