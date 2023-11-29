import coil.addAllMultiplatformTargets
import coil.nonAndroidMain
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
}

addAllMultiplatformTargets()
setupLibraryModule(name = "sample.compose.common")

compose {
    kotlinCompilerPlugin = libs.jetbrains.compose.compiler.get().toString()
}

kotlin {
    nonAndroidMain()

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.coilSampleCommon)
                implementation(projects.coilComposeSingleton)

                implementation(libs.jetbrains.compose.material)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}
