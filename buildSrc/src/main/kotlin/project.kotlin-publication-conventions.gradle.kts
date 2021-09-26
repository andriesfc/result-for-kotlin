@file:Suppress("UNCHECKED_CAST")

import Project_kotlin_publication_conventions_gradle.UpdatableRecord
import org.gradle.kotlin.dsl.support.useToRun
import java.util.*

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
    description =
        "Convenience task which just package the sources, dokka html and javadocs into a jars."
    dependsOn(packageSources, packageDokkaHtml, packageJavaDocs)
}


fun String.prefixed() = if (endsWith(".")) this else buildString {
    append(this)
    append('.')
}

fun Properties.load(file: File): Properties {
    file.reader(Charsets.UTF_8).useToRun { load(this) }
    return this
}


fun Project.props(): Map<String, String?> {

    val properties = generateSequence(this) { p -> p.parent }.map { p ->
        p.file(
            "project.properties",
            PathValidation.NONE
        ).absoluteFile
    }.filter { p -> p.exists() }.map { file -> Properties().load(file) }.toList().reversed()


    return properties.fold(mutableMapOf<String, String?>()) { collected, p ->
        p.forEach { (k, v) -> collected[k as String] = v as String? }
        collected
    }
}


private typealias UpdatableRecord = MutableMap<String, MutableMap<String, String?>>

fun Map<String, String?>.records(recordPrefix: String): Map<String, Map<String, String?>> {

    if (isEmpty()) {
        return emptyMap()
    }

    val prefix = if (recordPrefix.endsWith("."))
        recordPrefix
    else "%s.".format(recordPrefix)

    val collected: UpdatableRecord = entries.map { (k, v) ->
        k to v.takeUnless(String?::isNullOrEmpty)
    }.fold(mutableMapOf()) { collecting, (path, value) ->
        if (path.startsWith(prefix) && path.length > prefix.length) {

            val parentStart = prefix.length
            val parentEnd = path.indexOf('.', startIndex = parentStart + 1)
            val parent = if (parentEnd == -1) path.substring(parentStart) else path.substring(
                parentStart,
                parentEnd
            )

            val record = collecting.computeIfAbsent(parent) { mutableMapOf() }

            if (parentEnd != -1) {
                val field = path.substring(parentEnd + 1)
                record[field] = value
            }
        }
        collecting
    }

    return collected.toMap()
}

fun String.unquoted() = trim().trim('"', '\'')
fun String.unblocked() = unquoted().lineSequence().joinToString(" ")

publishing {
    publications {
        create<MavenPublication>("maven") {
            val props = props()
            val pubArtifact = "%s-%s".format(rootProject.name, project.name)
            groupId = "${rootProject.group}"
            artifactId = pubArtifact
            version = "${rootProject.version}"
            artifact(packageSources)
            artifact(packageDokkaHtml)
            artifact(packageJavaDocs)
            from(components["java"])
            pom.inceptionYear.set(props["project.inceptionYear"])
            pom.description.set(props["project.description"]?.unblocked())
            val licenses = props.records("project.license")
            pom.licenses {
                licenses.forEach { (_, details) ->
                    license {
                        name.set(details["name"])
                        url.set(details["url"])
                        distribution.set(details["distribution"])
                        comments.set(details["comments"]?.unblocked())
                    }
                }
            }
            props.records("project.developer").filter { (_, r) -> r.isNotEmpty() }.let { devs ->
                pom.developers {
                    devs.forEach { (developerId, r) ->
                        developer {
                            id.set(developerId)
                            email.set(r["email"])
                            timezone.set(r["timezone"])
                            organization.set(r["org.name"])
                            organizationUrl.set(r["org.url"])
                            val devRoles =
                                r.entries.filter { (k, _) -> k.startsWith("role") }
                                    .map { it.value }
                            this.roles.set(devRoles)
                        }
                    }
                }
            }
        }
    }
}




