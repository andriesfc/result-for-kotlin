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

    val kotlinVersion = "1.5.30"
    val junitVersion = "5.8.1"
    val assertkVersion = "0.25"
    val mockkVersion = "1.12.0"
    val coroutineVersion = "1.5.1"
    val clicktVersion = "3.2.0"

    constraints {
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
        implementation("com.github.ajalt.clikt:clikt:$clicktVersion")
        implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
        testImplementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
        testImplementation("com.willowtreeapps.assertk:assertk-jvm:$assertkVersion")
        testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
        testImplementation("io.mockk:mockk:$mockkVersion")
    }

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("io.mockk:mockk")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm")
    testImplementation(kotlin("reflect"))
}


tasks.test {
    useJUnitPlatform()
}

val kotlinTarget = "1.5"
val jvmTarget = "11"
val dokkaVersion = "1.5.30"

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.languageVersion = kotlinTarget
    kotlinOptions.jvmTarget = jvmTarget
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = jvmTarget
    targetCompatibility = jvmTarget
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmTarget))
    }
}
