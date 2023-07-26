package coil

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer

fun addAllTargets(project: Project) {
    project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
        project.extensions.getByType<KotlinMultiplatformExtension>().apply {
            val hasAndroidPlugin = project.plugins.hasPlugin("com.android.library")
            if (hasAndroidPlugin) {
                androidTarget {
                    publishLibraryVariants("release")
                }
            }

            iosArm64()
            iosX64()
            iosSimulatorArm64()

            js {
                browser()
                binaries.executable()
            }

            jvm()

            macosArm64()
            macosX64()

            val commonMain = sourceSets.getByName("commonMain")
            val commonTest = sourceSets.getByName("commonTest")

            val nativeMain = sourceSets.create("nativeMain").apply {
                dependsOn(commonMain)
            }
            val nativeTest = sourceSets.create("nativeTest").apply {
                dependsOn(commonTest)
            }

            val iosMain = sourceSets.create("iosMain").apply {
                dependsOn(nativeMain)
            }
            val iosTest = sourceSets.create("iosTest").apply {
                dependsOn(nativeTest)
            }

            val macosMain = sourceSets.create("macosMain").apply {
                dependsOn(nativeMain)
            }
            val macosTest = sourceSets.create("macosTest").apply {
                dependsOn(nativeTest)
            }

            targets.forEach { target ->
                // Some Kotlin targets do not have this property, but native ones always will.
                if (target.platformType.name == "native") {
                    if (target.name.startsWith("ios")) {
                        target.compilations.getByName("main").defaultSourceSet.dependsOn(iosMain)
                        target.compilations.getByName("test").defaultSourceSet.dependsOn(iosTest)
                    } else if (target.name.startsWith("macos")) {
                        target.compilations.getByName("main").defaultSourceSet.dependsOn(macosMain)
                        target.compilations.getByName("test").defaultSourceSet.dependsOn(macosTest)
                    } else {
                        throw AssertionError("Unknown target ${target.name}")
                    }
                }
            }
        }
    }
}

// nonAndroidMain: jsMain, jvmMain, nativeMain
fun KotlinSourceSetContainer.createNonAndroidMain(): KotlinSourceSet {
    val nonAndroidMain = sourceSets.create("nonAndroidMain").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("jsMain", "jvmMain", "nativeMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(nonAndroidMain)
    }
    return nonAndroidMain
}

// nonJsMain: androidMain, jvmMain, nativeMain
fun KotlinSourceSetContainer.createNonJsMain(): KotlinSourceSet {
    val nonJsMain = sourceSets.create("nonJsMain").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("androidMain", "jvmMain", "nativeMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(nonJsMain)
    }
    return nonJsMain
}

// jvmCommon: androidMain, jvmMain
fun KotlinSourceSetContainer.createJvmCommon(): KotlinSourceSet {
    val jvmCommon = sourceSets.create("jvmCommon").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("androidMain", "jvmMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(jvmCommon)
    }
    return jvmCommon
}

// nonJvmCommon: jsMain, nativeMain
fun KotlinSourceSetContainer.createNonJvmCommon(): KotlinSourceSet {
    val nonJvmCommon = sourceSets.create("nonJvmCommon").apply {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    listOf("jsMain", "nativeMain").forEach { name ->
        sourceSets.getByName(name).dependsOn(nonJvmCommon)
    }
    return nonJvmCommon
}
