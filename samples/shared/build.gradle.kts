import coil3.addAllMultiplatformTargets
import coil3.compileSdk
import coil3.multiplatformAndroidLibrary
import coil3.minSdk

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
}

addAllMultiplatformTargets(libs.versions.skiko)
multiplatformAndroidLibrary()

kotlin {
    androidLibrary {
        namespace = "sample.common"
        compileSdk = project.compileSdk
        minSdk = project.minSdk

        lint {
            warningsAsErrors = true
            disable += listOf(
                "ComposableNaming",
                "UnknownIssueId",
                "UnsafeOptInUsageWarning",
                "UnusedResources",
                "UseSdkSuppress",
                "VectorPath",
                "VectorRaster",
            )
        }

        packaging {
            resources.pickFirsts += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*kotlin_module",
            )
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.coil)
                api(projects.coilSvg)
                api(libs.kotlinx.serialization.json)
            }
        }
        androidMain {
            dependencies {
                api(projects.coilNetworkOkhttp)
                api(projects.coilGif)
                api(projects.coilVideo)
                api(libs.androidx.core)
                api(libs.androidx.lifecycle.viewmodel)
                api(libs.google.material)
            }
        }
        appleMain {
            dependencies {
                api(projects.coilNetworkKtor3)
                api(libs.ktor3.engine.darwin)
            }
        }
        jsMain {
            dependencies {
                api(projects.coilNetworkKtor3)
            }
        }
        jvmMain {
            dependencies {
                api(projects.coilNetworkOkhttp)
            }
        }
        named("wasmJsMain") {
            dependencies {
                api(projects.coilNetworkKtor3)
            }
        }
    }
}
