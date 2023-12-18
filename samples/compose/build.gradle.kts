import coil3.androidApplication
import coil3.applyCoilHierarchyTemplate
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

plugins {
    id("com.android.application")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("org.jetbrains.compose")
}

androidApplication(name = "sample.compose") {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                "../shared/shrinker-rules.pro",
                "../shared/shrinker-rules-android.pro",
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
            mainClass = "sample.compose.MainKt"
            nativeDistributions {
                targetFormats(
                    TargetFormat.Deb,
                    TargetFormat.Dmg,
                    TargetFormat.Msi,
                )
                packageName = "sample.compose"
                packageVersion = "1.0.0"
            }
        }
    }
    experimental {
        web.application {}
    }
}

kotlin {
    applyCoilHierarchyTemplate()

    androidTarget()

    jvm("desktop")

    val configureJs: KotlinJsTargetDsl.() -> Unit = {
        moduleName = "coilSample"
        browser {
            commonWebpackConfig {
                outputFileName = "coilSample.js"
            }
        }
        binaries.executable()
    }
    js(configureJs)
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs(configureJs)

    arrayOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.samples.shared)
                implementation(projects.coilCompose)
                implementation(compose.material)
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
            }
            resources.srcDir("../shared/src/commonMain/resources")
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
            }
        }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
