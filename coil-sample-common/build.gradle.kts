import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule(name = "coil.sample.common", buildConfig = true)

dependencies {
    api(projects.coilSingleton)
    api(projects.coilGif)
    api(projects.coilSvg)
    api(projects.coilVideo)

    api(libs.androidx.core)
    api(libs.androidx.lifecycle.viewmodel)
}
