val buildVersion by extra("1.0.0-SNAPSHOT")

allprojects {
    group = "io.github.andriesfc.resultk"
    version = buildVersion
}

//<editor-fold desc="Tasks">
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

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
//</editor-fold>


//<editor-fold desc="Supporting functions">
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

//</editor-fold>
