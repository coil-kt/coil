import coil.addAllTargets
import coil.createJvmCommon
import coil.createNonAndroidMain
import coil.createNonJsMain
import coil.createNonJvmCommon
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("dev.drewhamilton.poko")
}

addAllTargets(project)
setupLibraryModule(name = "coil.base")

kotlin {
    createNonAndroidMain()
    createNonJsMain()
    createJvmCommon()
    createNonJvmCommon()

    sourceSets {
        commonMain {
            dependencies {
                api(libs.coroutines.core)
                api(libs.kotlin.stdlib)
                api(libs.okio)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test.common)
            }
        }
        named("nonJvmCommon") {
            dependencies {
                implementation(libs.kotlinx.immutable.collections)
            }
        }
        named("nonAndroidMain") {
            dependencies {
                implementation(libs.skiko)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(libs.androidx.annotation)
                implementation(libs.androidx.appcompat.resources)
                implementation(libs.androidx.core)
                implementation(libs.androidx.exifinterface)
                implementation(libs.androidx.profileinstaller)
                api(libs.androidx.lifecycle.runtime)
                api(libs.coroutines.android)
            }
        }
        named("androidUnitTest") {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.jvm)
            }
        }
        named("androidInstrumentedTest") {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.android)
            }
        }
    }
}
