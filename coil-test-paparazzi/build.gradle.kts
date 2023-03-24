import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("app.cash.paparazzi")
}

setupLibraryModule(name = "coil.test.paparazzi") {
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
}
