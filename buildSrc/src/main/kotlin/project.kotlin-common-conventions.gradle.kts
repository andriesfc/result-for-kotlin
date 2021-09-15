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
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.5.30"))
    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
        implementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
        implementation("com.github.javafaker:javafaker:1.0.2")
        implementation("org.junit.jupiter:junit-jupiter:5.7.2")
        implementation("org.junit.jupiter:junit-jupiter-params:5.7.2")
        implementation("com.github.ajalt.clikt:clikt:3.2.0")
        implementation("io.mockk:mockk:1.12.0")
    }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm")
    testImplementation("com.github.javafaker:javafaker")
    testImplementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
}

group = "io.github.andriesfc.niftyget"
val kotlinTarget = "1.5"
val jvmTarget = "11"
val dokkaVersion = "1.5.0"

tasks.withType<KotlinCompile>().all {
    kotlinOptions.languageVersion = kotlinTarget
    kotlinOptions.jvmTarget = jvmTarget
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmTarget))
    }
}
