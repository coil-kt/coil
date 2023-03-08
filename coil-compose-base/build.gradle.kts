import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("app.cash.paparazzi")
}

setupLibraryModule(name = "coil.compose.base", publish = true) {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    api(projects.coilBase)

    implementation(libs.androidx.core)
    implementation(libs.accompanist.drawablepainter)
    api(libs.compose.foundation)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.coilTestInternal)
    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(libs.compose.ui.test)
}
