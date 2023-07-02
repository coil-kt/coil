import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule(name = "coil.test.internal")

dependencies {
    api(projects.coilBase)
    api(libs.androidx.activity)
    api(libs.androidx.core)
    api(libs.androidx.test.core)
    api(libs.androidx.test.junit)
    api(libs.coroutines.android)
    api(libs.coroutines.test)
    api(libs.junit)
    api(libs.okio)

    testImplementation(libs.kotlin.test)
}
