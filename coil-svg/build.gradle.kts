import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule(namespace = "coil.svg")

dependencies {
    api(projects.coilBase)

    implementation(libs.androidx.core)
    implementation(libs.svg)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.coilTestInternal)
    androidTestImplementation(libs.bundles.test.android)
}
