plugins {
    id("project.kotlin-library-conventions")
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework:spring-expression:5.3.9")
    implementation(kotlin("reflect"))
}