import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("app.cash.paparazzi")
}

setupLibraryModule(name = "coil.base", publish = true)

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.androidx.collection)
    implementation(libs.androidx.core)
    implementation(libs.androidx.exifinterface)
    api(libs.androidx.lifecycle.runtime)
    api(libs.coroutines.android)
    api(libs.kotlin.stdlib)
    api(libs.okhttp)
    api(libs.okio)
    api(libs.androidx.profileinstaller)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.coilTestInternal)
    androidTestImplementation(libs.bundles.test.android)
}
