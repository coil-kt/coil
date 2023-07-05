import coil.addAllTargets
import coil.by
import coil.createNonAndroidMain
import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("org.jetbrains.compose")
}

addAllTargets(project)
setupLibraryModule(name = "coil.compose.base")

compose {
    kotlinCompilerPlugin by libs.jetbrains.compose.compiler.get().toString()
}

kotlin {
    createNonAndroidMain()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilBase)
                api(libs.jetbrains.compose.foundation)
            }
        }
        named("androidMain") {
            dependencies {
                implementation(libs.accompanist.drawablepainter)
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
                implementation(libs.androidx.compose.ui.test)
            }
        }
    }
}
