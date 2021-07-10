
val buildVersion by extra("1.0.0-SNAPSHOT")

allprojects {
    group = "io.github.andriesfc.kotlin"
    version = buildVersion
}

tasks.register("build") {
    group = "Build"
    description = "Builds all modules, including running test verification tasks."
    dependsOn(":lib:build")
}