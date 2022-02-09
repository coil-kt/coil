import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

setupLibraryModule()

dependencies {
    api(project(":coil-base"))

    testImplementation(project(":coil-test"))
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(project(":coil-test"))
    androidTestImplementation(libs.bundles.test.android)
}
