package coil

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer

fun Project.addAllMultiplatformTargets() {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.getByType<KotlinMultiplatformExtension>().apply {
            if (plugins.hasPlugin("com.android.library")) {
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

            applyDefaultHierarchyTemplate()
        }
    }
}

fun KotlinSourceSetContainer.jvmCommon() = createSourceSet(
    name = "jvmCommon",
    children = listOf("androidMain", "jvmMain"),
)

fun KotlinSourceSetContainer.nonAndroidMain() = createSourceSet(
    name = "nonAndroidMain",
    children = listOf("jsMain", "jvmMain", "nativeMain"),
)

fun KotlinSourceSetContainer.nonJsMain() = createSourceSet(
    name = "nonJsMain",
    children = listOf("jvmCommon", "nativeMain"),
)

fun KotlinSourceSetContainer.nonJvmCommon() = createSourceSet(
    name = "nonJvmCommon",
    children = listOf("jsMain", "nativeMain"),
)

private fun KotlinSourceSetContainer.createSourceSet(
    name: String,
    children: List<String>,
) {
    val sourceSet = sourceSets.create(name)
    sourceSet.dependsOn(sourceSets.getByName("commonMain"))
    for (child in children) {
        sourceSets.getByName(child).dependsOn(sourceSet)
    }
}
