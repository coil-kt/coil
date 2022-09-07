import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule(name = "coil.base", publish = true)

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.androidx.collection)
    implementation(libs.androidx.core)
    implementation(libs.androidx.exifinterface)
    api(libs.androidx.lifecycle.common)
    api(libs.coroutines.android)
    api(libs.kotlin.stdlib)
    api(libs.okhttp)
    api(libs.okio)

    constraints {
        implementation(libs.androidx.lifecycle.runtime)
        implementation(libs.androidx.lifecycle.viewmodel)
    }

    testImplementation(projects.coilTest)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.coilTest)
    androidTestImplementation(libs.bundles.test.android)
}
