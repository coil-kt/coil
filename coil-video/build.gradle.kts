import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-android")
}

androidLibrary(name = "coil3.video")

dependencies {
    api(projects.coilCore)

    implementation(libs.androidx.core)

    testImplementation(projects.internal.testUtils)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.internal.testUtils)
    androidTestImplementation(libs.bundles.test.android)
}
