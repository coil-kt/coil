import coil3.addAllMultiplatformTargets
import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
}

addAllMultiplatformTargets()
androidLibrary(name = "sample.common", config = true)

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coil)
                api(projects.coilNetworkKtor)
                api(projects.coilSvg)
                api(libs.kotlinx.serialization.json)
            }
        }
        androidMain {
            dependencies {
                api(projects.coilGif)
                api(projects.coilVideo)
                api(libs.androidx.core)
                api(libs.androidx.lifecycle.viewmodel)
                api(libs.ktor.engine.android)
            }
        }
        appleMain {
            dependencies {
                api(libs.ktor.engine.darwin)
            }
        }
        jsMain {
            dependencies {
                api(libs.ktor.engine.js)
            }
        }
        jvmMain {
            dependencies {
                api(libs.coroutines.swing)
                api(libs.ktor.engine.java)
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
