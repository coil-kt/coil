import coil.addAllMultiplatformTargets
import coil.nonAndroidMain
import coil.setupAppModule
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("com.android.application")
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
}

addAllMultiplatformTargets()
setupAppModule(name = "sample.compose") {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                "../coil-sample-common/shrinker-rules.pro",
                "../coil-sample-common/shrinker-rules-android.pro",
            )
            signingConfig = signingConfigs["debug"]
        }
        create("benchmark") {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

compose {
    kotlinCompilerPlugin = libs.jetbrains.compose.compiler.get().toString()
    desktop {
        application {
            mainClass = "MainKt"

            nativeDistributions {
                targetFormats(TargetFormat.Dmg, TargetFormat.Deb)
                packageName = "sample.common"
                packageVersion = "1.0.0"
            }
        }
    }
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
                implementation(libs.androidx.lifecycle.viewmodel.compose)
            }
        }
    }
}
