import coil3.androidApplication
import coil3.applyCoilHierarchyTemplate
import coil3.enableWasm
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl

plugins {
    id("com.android.application")
    id("kotlin-multiplatform")
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
}

kotlin {
    applyCoilHierarchyTemplate()

    androidTarget()

    jvm("desktop")

    js {
        moduleName = "coilSample"
        browser {
            commonWebpackConfig {
                outputFileName = "coilSample.js"
            }
        }
        binaries.executable()
    }

    if (enableWasm) {
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            moduleName = "coilSample"
            browser()
            binaries.executable()
            applyBinaryen()
        }
    }

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

// https://youtrack.jetbrains.com/issue/KT-56025
if (enableWasm) {
    afterEvaluate {
        tasks {
            val configureJs: Task.() -> Unit = {
                dependsOn(named("jsDevelopmentExecutableCompileSync"))
                dependsOn(named("jsProductionExecutableCompileSync"))
                dependsOn(named("jsTestTestDevelopmentExecutableCompileSync"))

                dependsOn(named("wasmJsDevelopmentExecutableCompileSync"))
                dependsOn(named("wasmJsProductionExecutableCompileSync"))
                dependsOn(named("wasmJsTestTestDevelopmentExecutableCompileSync"))
            }
            named("jsBrowserProductionWebpack").configure(configureJs)
            named("wasmJsBrowserProductionExecutableDistributeResources").configure(configureJs)
        }
    }
}
