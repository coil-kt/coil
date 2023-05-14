import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule(name = "coil.base")

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.androidx.collection)
    implementation(libs.androidx.core)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.profileinstaller)
    api(libs.androidx.lifecycle.runtime)
    api(libs.coroutines.android)
    api(libs.kotlin.stdlib)
    api(libs.okhttp)
    api(libs.okio)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.coilTestInternal)
    androidTestImplementation(libs.bundles.test.android)
}
