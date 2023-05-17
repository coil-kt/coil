package coil

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun addAllTargets(project: Project) {
    project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
        project.extensions.getByType<KotlinMultiplatformExtension>().apply {
            val hasAndroidPlugin = project.plugins.hasPlugin("com.android.library")
            if (hasAndroidPlugin) {
                android {
                    publishLibraryVariants("release")
                }
            }

            iosArm64()
            iosX64()
            iosSimulatorArm64()

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

            if (hasAndroidPlugin) {
                val androidJvmMain = sourceSets.create("androidJvmMain").apply {
                    dependsOn(commonMain)
                }
                sourceSets.getByName("androidMain").apply {
                    dependsOn(androidJvmMain)
                }
                sourceSets.getByName("jvmMain").apply {
                    dependsOn(androidJvmMain)
                }
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
