import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("io.github.takahirom.roborazzi")
}

setupLibraryModule(namespace = "coil.test.roborazzi") {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    api(projects.coilBase)

    implementation(projects.coilComposeBase)
    implementation(projects.coilTest)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)
    testImplementation(libs.compose.ui.test)
    testImplementation(libs.roborazzi.core)
    testImplementation(libs.roborazzi.junit)
}
