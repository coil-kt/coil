import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

setupLibraryModule {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.get()
    }
}

dependencies {
    api(projects.coilBase)

    implementation(libs.androidx.core)
    implementation(libs.accompanist.drawablepainter)
    api(libs.compose.foundation)

    testImplementation(projects.coilTest)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.coilTest)
    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(libs.compose.ui.test)
}
