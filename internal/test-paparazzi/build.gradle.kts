import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("app.cash.paparazzi")
    id("org.jetbrains.compose")
}

androidLibrary(name = "coil3.test.paparazzi")

dependencies {
    api(projects.coilCore)

    implementation(projects.coilComposeCore)
    implementation(projects.coilTest)

    testImplementation(projects.internal.testUtils)
    testImplementation(libs.bundles.test.jvm)
}
