import coil3.addAllMultiplatformTargets
import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("org.jetbrains.compose")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.compose.singleton")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coil)
                api(projects.coilComposeCore)
            }
        }
    }
}
