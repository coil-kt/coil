import coil3.addAllMultiplatformTargets
import coil3.androidOnlyLibrary

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
}

addAllMultiplatformTargets(libs.versions.skiko)
androidOnlyLibrary(name = "sample.common", config = true)

kotlin {
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

android {
    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/commonMain/resources")
            }
        }
    }
}
