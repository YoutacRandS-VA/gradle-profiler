plugins {
    id("profiler.embedded-library")
    id("profiler.publication")
}

dependencies {
    implementation(libs.ideStarter) {
        exclude(group = "io.ktor")
        exclude(group = "com.jetbrains.infra")
        exclude(group = "com.jetbrains.intellij.remoteDev")
    }
    testImplementation(libs.bundles.testDependencies)
    testImplementation(libs.commonIo)
}
