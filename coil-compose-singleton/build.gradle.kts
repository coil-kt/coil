import coil.addAllMultiplatformTargets
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("org.jetbrains.compose")
}

addAllMultiplatformTargets()
setupLibraryModule(name = "coil.compose.singleton")

compose {
    kotlinCompilerPlugin = libs.jetbrains.compose.compiler.get().toString()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilComposeBase)
                api(projects.coilSingleton)
            }
        }
    }
}
