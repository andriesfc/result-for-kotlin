@file:Suppress("LocalVariableName", "SpellCheckingInspection")

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    `maven-publish`
}

val artifactName = "resultk"
val artifactGroup = "andriesfc.kotlin.resultk"

val sourcesJar by tasks.creating(Jar::class) {
    group = "Build"
    description = "Package sources as a JAR"
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>("Lib") {
            groupId = artifactGroup
            artifactId = artifactName
            description = "Result handling in Kotlin"
            version = "1.0.0-SNAPSHOT"
            from(components["java"])
            artifact(sourcesJar)
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }
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
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junit5_version")

    // MockK
    val mockk_version = "1.11.0"
    testImplementation("io.mockk:mockk:$mockk_version")

    // AssertK
    val assertk_version = "0.24"
    testImplementation("com.willowtreeapps.assertk:assertk:$assertk_version")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")


}

tasks.withType<Test> {
    useJUnitPlatform()
}