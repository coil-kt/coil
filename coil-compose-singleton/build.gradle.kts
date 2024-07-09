import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose")
}

setupLibraryModule(name = "coil.compose.singleton") {
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(projects.coilComposeBase)
    api(projects.coilSingleton)
}
