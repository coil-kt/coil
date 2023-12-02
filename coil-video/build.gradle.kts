import coil3.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule(name = "coil3.video")

dependencies {
    api(projects.coilBase)

    implementation(libs.androidx.core)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.coilTestInternal)
    androidTestImplementation(libs.bundles.test.android)
}
