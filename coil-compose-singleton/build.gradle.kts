import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
}

setupLibraryModule(namespace = "coil.compose.singleton") {
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    api(projects.coilComposeBase)
    api(projects.coilSingleton)
}
