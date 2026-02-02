import coil3.addNodePolyfillWebpackPlugin
import coil3.applyCoilHierarchyTemplate
import coil3.applyJvm11OnlyToJvmTarget
import coil3.compileSdk
import coil3.minSdk
import coil3.multiplatformAndroidLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

multiplatformAndroidLibrary()

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

    androidLibrary {
        namespace = "sample.compose"
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

    jvm()

    js {
        outputModuleName = "coilSample"
        browser {
            commonWebpackConfig {
                outputFileName = "coilSample.js"
            }
        }
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "coilSample"
        browser {
            commonWebpackConfig {
                outputFileName = "coilSample.js"
            }
        }
        binaries.executable()
    }

    arrayOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", "coil3")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.coilCompose)
                implementation(projects.samples.shared)
                implementation(compose.components.resources)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
            }
        }
        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }

    addNodePolyfillWebpackPlugin()
}

// https://youtrack.jetbrains.com/issue/KT-56025
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
        named("wasmJsBrowserProductionWebpack").configure(configureJs)
    }
}

// Compose 1.8.0 requires JVM 11 only for the JVM target.
applyJvm11OnlyToJvmTarget()
