plugins {
    `java-base`
    `maven-publish`
}

val packingTasks = "publishing"

val packageSources: Jar by tasks.creating(Jar::class) {
    group = packingTasks
    archiveClassifier.set("sources")
    description = "Package source jar for publication"
    from(sourceSets.getByName("main").allSource)
}

val packageDokkaHtml: Jar by tasks.creating(Jar::class) {
    group = packingTasks
    archiveClassifier.set("dokka-html")
    description = "Package all dokka HTML docs into a single jar for publication"
    dependsOn(":${project.name}:dokkaHtml")
    from(buildDir.resolve("dokka/html"))
}

val packageJavaDocs: Jar by tasks.creating(Jar::class) {
    group = packingTasks
    archiveClassifier.set("javadoc")
    description = "Packages all javadocs into a single jar for publication"
    dependsOn(":${project.name}:dokkaJavadoc")
    from(buildDir.resolve("dokka/javadoc"))
}

val packageArtifacts: Task by tasks.creating {
    group = packingTasks
    description = "Convenience task which just package the sources, dokka html and javadocs into a jars."
    dependsOn(packageSources, packageDokkaHtml, packageJavaDocs)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "${rootProject.group}"
            artifactId = "${rootProject.name}-${project.name}"
            version = "${rootProject.version}"
            description = project.description
            artifact(packageSources)
            artifact(packageDokkaHtml)
            artifact(packageJavaDocs)
            from(components["java"])
        }
    }
}




