
import coil.addAllMultiplatformTargets
import coil.darwinMain
import coil.jvmCommon
import coil.nonAndroidMain
import coil.nonJvmCommon
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
}

addAllMultiplatformTargets()
setupLibraryModule(name = "sample.common", config = true)

kotlin {
    darwinMain()
    jvmCommon()
    nonAndroidMain()
    nonJvmCommon()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilSingleton)
                api(projects.coilNetwork)

                api(libs.kotlinx.serialization.json)
            }
        }
        androidMain {
            dependencies {
                api(projects.coilGif)
                api(projects.coilSvg)
                api(projects.coilVideo)

                api(libs.androidx.core)
                api(libs.androidx.lifecycle.viewmodel)

                api(libs.ktor.engine.okhttp)
            }
        }
        named("darwinMain") {
            dependencies {
                api(libs.ktor.engine.darwin)
            }
        }
        named("jsMain") {
            dependencies {
                api(libs.ktor.engine.js)
            }
        }
        named("jvmMain") {
            dependencies {
                api(libs.ktor.engine.java)
            }
        }
    }
}

android {
    sourceSets["main"].apply {
        res.srcDirs(
            "src/androidMain/resources",
            "src/commonMain/resources",
        )
    }
}
