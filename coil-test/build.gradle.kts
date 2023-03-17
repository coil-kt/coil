import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule(name = "coil.test", publish = true)

dependencies {
    api(projects.coilBase)

    implementation(libs.androidx.core)
    implementation(libs.okio.fakefilesystem)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)
}
