import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.vanniktech.maven.publish")
}

setupLibraryModule()

dependencies {
    constraints {
        api(project(":coil-base"))
        api(project(":coil-singleton"))
        api(project(":coil-compose-base"))
        api(project(":coil-compose-singleton"))
        api(project(":coil-gif"))
        api(project(":coil-svg"))
        api(project(":coil-video"))
    }
}
