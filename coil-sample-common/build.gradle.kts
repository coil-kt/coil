import coil.Library
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

    api(Library.ANDROIDX_CORE)
    api(Library.ANDROIDX_LIFECYCLE_VIEW_MODEL)
}
