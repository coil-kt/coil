import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule()

dependencies {
    // Prevent a dependency cycle.
    compileOnly(project(":coil-base"))

    api(libs.androidx.activity)
    api(libs.androidx.core)
    api(libs.androidx.test.core)
    api(libs.androidx.test.junit)
    api(libs.coroutines.android)
    api(libs.coroutines.test)
    api(libs.junit)
    api(libs.okhttp)
    api(libs.okhttp.mockwebserver)
    api(libs.okio)

    testImplementation(libs.kotlin.test)
}
