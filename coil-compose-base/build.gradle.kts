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
    api(project(":coil-base"))

    implementation(libs.androidx.core)
    implementation(libs.accompanist.drawablepainter)
    api(libs.compose.foundation)

    testImplementation(project(":coil-test"))
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(project(":coil-test"))
    androidTestImplementation(libs.bundles.test.android)
    androidTestImplementation(libs.compose.ui.test)
}
