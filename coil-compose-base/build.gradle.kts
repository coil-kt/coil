import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
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

    constraints {
        implementation(libs.androidx.lifecycle.runtime)
        implementation(libs.androidx.lifecycle.viewmodel)
    }

    testImplementation(projects.coilTest)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.coilTest)
    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(libs.compose.ui.test)
}
