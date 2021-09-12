@file:Suppress("LocalVariableName", "SpellCheckingInspection")
plugins {
    id("project.kotlin-library-conventions")
}

dependencies {
    testImplementation("commons-codec:commons-codec:1.15")
    testImplementation("org.apache.commons:commons-text:1.9")
}