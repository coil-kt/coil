import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule(publish = true)

dependencies {
    api(projects.coilBase)

    implementation(libs.androidx.core)
    implementation(libs.androidx.vectordrawable.animated)

    testImplementation(projects.coilTest)
    testImplementation(libs.bundles.test.jvm)

    androidTestImplementation(projects.coilTest)
    androidTestImplementation(libs.bundles.test.android)
}
