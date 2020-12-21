plugins {
    `kotlin-dsl`
}

apply("extra.gradle.kts")

dependencies {
    implementation(rootProject.extra.get("androidGradlePlugin").toString())
    implementation(rootProject.extra.get("kotlinPlugin").toString())
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
