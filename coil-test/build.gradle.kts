import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule(name = "coil.test")

dependencies {
    api(projects.coilBase)

    implementation(libs.androidx.core)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)
}
