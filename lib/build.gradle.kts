@file:Suppress("LocalVariableName", "SpellCheckingInspection")

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
    id("org.jetbrains.dokka") version "1.4.32"
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

val javadocJar by tasks.creating(Jar::class) {
    group = "Build"
    dependsOn(":lib:dokkaJavadoc")
    description = "Package Java Doc"
    archiveClassifier.set("javadoc")
    from(buildDir.resolve("dokka/javadoc"))
}

val htmlDokkaJar by tasks.creating(Jar::class) {
    group = "Build"
    description = "Packages Kotlin HTML documentation"
    archiveClassifier.set("html")
    dependsOn(":lib:dokkaHtml")
    from(buildDir.resolve("dokka/html"))
}

publishing {
    publications {
        create<MavenPublication>("Lib") {
            groupId = artifactGroup
            artifactId = artifactName
            description = "Result handling in Kotlin"
            version = "1.0.0-SNAPSHOT"
            pom {
                developers {
                    developer {
                        name.set("AndriesFC")
                        email.set("andriesfc@gmail.com")
                        roles.add("Maintainer")
                    }
                }
                scm {
                    developerConnection.set("git")
                    url.set("https://github.com/andriesfc/result-for-kotlin.git")
                }
            }
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            artifact(htmlDokkaJar)
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