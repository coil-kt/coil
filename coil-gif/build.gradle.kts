import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-android")
}

androidLibrary(name = "coil3.gif")

dependencies {
    api(projects.coilCore)

    implementation(libs.androidx.core)
    implementation(libs.androidx.vectordrawable.animated)

    testImplementation(projects.internal.testUtils)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.internal.testUtils)
    androidTestImplementation(libs.bundles.test.android)
}
