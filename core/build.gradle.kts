@file:Suppress("LocalVariableName", "SpellCheckingInspection")

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.21"
    id("org.jetbrains.dokka") version "1.4.32"
    `java-library`
    `maven-publish`
    signing
}

val artifactName = "resultk"

val sourcesJar by tasks.creating(Jar::class) {
    group = "Build"
    description = "Package sources as a JAR"
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    group = "Build"
    dependsOn(":core:dokkaJavadoc")
    description = "Package Java Doc"
    archiveClassifier.set("javadoc")
    from(buildDir.resolve("dokka/javadoc"))
}

val htmlDokkaJar by tasks.creating(Jar::class) {
    group = "Build"
    description = "Packages Kotlin HTML documentation"
    archiveClassifier.set("html")
    dependsOn(":core:dokkaHtml")
    from(buildDir.resolve("dokka/html"))
}

val testJar by tasks.creating(Jar::class) {
    group = "Build"
    description = "Builds seperate jar which contains all thge tests."
    archiveClassifier.set("test")
    from(sourceSets.test.get().output) {
        include("resultk/testing/**")
    }
}

fun MavenPublication.describePublication() {
    groupId = "${project.group}"
    artifactId = artifactName
    description = project.description
    version = "${project.version}"
    pom {
        developers {
            developer {
                name.set("Andries")
                email.set("andriesfc@gmail.com")
                roles.add("Owner")
            }
        }
        scm {
            developerConnection.set("scm:git@github.com:andriesfc/result-for-kotlin.git")
            url.set("https://github.com/andriesfc/result-for-kotlin.git")
            connection.set("scm:git@github.com:andriesfc/result-for-kotlin.git")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("Lib") {
            describePublication()
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
            artifact(htmlDokkaJar)
            artifact(testJar)
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
    val mockk_version = "1.12.0"
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

tasks.withType<DokkaTask> {
    dokkaSourceSets {
        named("main") {
            moduleName.set("resultk")
            includes.from("module.md")
            jdkVersion.set(javaCompileLangVersion.asInt())
            sourceLink {
                remoteLineSuffix.set("#L")
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/andriesfc/result-for-kotlin/tree/main/lib/src/main/kotlin/"))
            }
        }
    }
}