import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.eygraber.paparazzi")
}

androidLibrary(name = "coil3.test.paparazzi")

dependencies {
    api(projects.coilCore)

    implementation(projects.coilComposeCore)
    implementation(projects.coilTest)

    testImplementation(projects.internal.testUtils)
    testImplementation(libs.bundles.test.jvm)
}
