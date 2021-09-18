import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-base`
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {

    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
        implementation("com.github.ajalt.clikt:clikt:3.2.0")
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        testImplementation("org.jetbrains.kotlin:kotlin-reflect")
        testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
        testImplementation("org.junit.jupiter:junit-jupiter:5.7.2")
        testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.2")
        testImplementation("io.mockk:mockk:1.12.0")
    }

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.5.30"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm")
    testImplementation(kotlin("reflect"))
}


tasks.test {
    useJUnitPlatform()
}

val kotlinTarget = "1.5"
val jvmTarget = "11"
val dokkaVersion = "1.5.30"

tasks.withType<KotlinCompile>().all {
    kotlinOptions.languageVersion = kotlinTarget
    kotlinOptions.jvmTarget = jvmTarget
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmTarget))
    }
}
