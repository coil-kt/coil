plugins {
    `kotlin-dsl`
}

apply(from = "extra.gradle.kts")

dependencies {
    implementation(rootProject.extra["androidPlugin"].toString())
    implementation(rootProject.extra["kotlinPlugin"].toString())
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

// https://issuetracker.google.com/issues/179291081
configurations.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.trove4j" && requested.name == "trove4j" && requested.version == "20160824") {
            useTarget("org.jetbrains.intellij.deps:trove4j:1.0.20181211")
        }
    }
}
