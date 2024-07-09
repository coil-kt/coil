import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

setupLibraryModule(name = "coil.test.roborazzi") {
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(projects.coilBase)

    implementation(projects.coilComposeBase)
    implementation(projects.coilTest)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.roborazzi.junit)
}
