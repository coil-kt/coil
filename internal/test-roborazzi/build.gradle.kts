import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("io.github.takahirom.roborazzi")
    id("org.jetbrains.compose")
}

androidLibrary(name = "coil3.test.roborazzi")

dependencies {
    api(projects.coilCore)

    implementation(projects.coilComposeCore)
    implementation(projects.coilTest)

    testImplementation(projects.internal.testUtils)
    testImplementation(libs.bundles.test.jvm)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.roborazzi.junit)
    testImplementation(compose.desktop.uiTestJUnit4)
}
