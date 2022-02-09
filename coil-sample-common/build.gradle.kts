import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule(enableBuildConfig = true)

dependencies {
    api(project(":coil-singleton"))
    api(project(":coil-gif"))
    api(project(":coil-svg"))
    api(project(":coil-video"))

    api(libs.androidx.core)
    api(libs.androidx.lifecycle.viewmodel)
}
