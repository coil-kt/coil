import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
}

setupLibraryModule()

dependencies {
    api(projects.coilBase)

    implementation(libs.androidx.core)

    testImplementation(projects.coilTest)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.coilTest)
    androidTestImplementation(libs.bundles.test.android)
}
