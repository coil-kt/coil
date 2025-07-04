import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.skikoAwtRuntimeDependency
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

addAllMultiplatformTargets(libs.versions.skiko, enableNativeLinux = false)
androidLibrary(name = "coil3.test.composeuimultiplatform")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.coil)
                implementation(projects.coilComposeCore)
                implementation(compose.components.resources)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
        jvmTest {
            dependencies {
                implementation(skikoAwtRuntimeDependency(libs.versions.skiko.get()))
            }
        }
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
