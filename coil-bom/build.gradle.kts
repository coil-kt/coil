import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.vanniktech.maven.publish")
}

setupLibraryModule()

dependencies {
    constraints {
        api(projects.coilBase)
        api(projects.coilSingleton)
        api(projects.coilComposeBase)
        api(projects.coilComposeSingleton)
        api(projects.coilGif)
        api(projects.coilSvg)
        api(projects.coilVideo)
    }
}
