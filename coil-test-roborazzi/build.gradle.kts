import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("io.github.takahirom.roborazzi")
    id("org.jetbrains.compose")
}

setupLibraryModule(name = "coil.test.roborazzi")

compose {
    kotlinCompilerPlugin = libs.jetbrains.compose.compiler.get().toString()
}

dependencies {
    api(projects.coilBase)

    implementation(projects.coilComposeBase)
    implementation(projects.coilTest)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.roborazzi.junit)
}
