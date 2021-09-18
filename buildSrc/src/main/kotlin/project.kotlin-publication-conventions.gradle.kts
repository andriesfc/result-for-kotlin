@file:Suppress("EnumEntryName")


plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-base`
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "${rootProject.group}"
            artifactId = project.name
            version = "${rootProject.version}"
            description = project.description
            from(components["java"])
         }
    }
}