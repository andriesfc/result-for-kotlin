val buildVersion by extra("1.0.0-SNAPSHOT")

allprojects {
    group = "io.github.andriesfc.kotlin"
    version = buildVersion
}

fun Task.doFirstOnSubProjects(taskName: String? = null) {
    group = "Project Specific"
    val todo = taskName ?: name
    doFirst {
        subprojects {
            tasks.findByName(todo)?.also { todoOnSubProject ->
                todoOnSubProject.actions.forEach { action ->
                    action.execute(todoOnSubProject)
                }
            }
        }
    }
}

tasks.register("build") {
    description = "Builds all modules, including running test verification tasks."
    doFirstOnSubProjects()
}

tasks.register("clean") {
    description = "Cleans projects"
    doFirstOnSubProjects()
}

tasks.register("assemble") {
    description = "Assemble all projects"
    doFirstOnSubProjects()
}

