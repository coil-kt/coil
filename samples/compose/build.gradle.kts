import coil3.androidApplication
import coil3.applyCoilHierarchyTemplate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.application")
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

androidApplication(name = "sample.compose") {
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs["debug"]
        }
        create("minifiedRelease") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            matchingFallbacks += "release"
            proguardFiles(
                "../shared/shrinker-rules.pro",
                "../shared/shrinker-rules-android.pro",
            )
            signingConfig = signingConfigs["debug"]
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
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.samples.shared)
                implementation(projects.coilCompose)
                implementation(compose.components.resources)
                implementation(compose.material3)
            }
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

// Compose 1.8.0 requires JVM 11.
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget = JvmTarget.JVM_11
}
