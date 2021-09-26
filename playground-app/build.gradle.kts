plugins {
    id("project.kotlin-application-conventions")
}


application {
    mainClass.set("resultk.playgroundapp.AppKt")
}

dependencies {
    implementation("io.github.andriesfc.resultk:resultk-core:0.0.1-SNAPSHOT")
}