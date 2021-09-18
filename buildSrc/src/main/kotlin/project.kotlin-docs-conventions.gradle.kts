import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    id("org.jetbrains.dokka")
    `java-library`
}

tasks.withType<DokkaTask>() {
    dokkaSourceSets {
        named("main") {
            moduleName.set(project.name)
            includes.from("module.md")
            jdkVersion.set(java.toolchain.languageVersion.get().asInt())
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL(buildString {
                    append("https://github.com/andriesfc/result-for-kotlin/tree/main/")
                    append(projectDir.name)
                    append("/src/main/kotlin")
                }))
            }
        }
    }
}

