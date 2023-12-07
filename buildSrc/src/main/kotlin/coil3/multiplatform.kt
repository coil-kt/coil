package coil3

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer

fun Project.addAllMultiplatformTargets() {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            applyDefaultHierarchyTemplate()

            val isAndroidApp = plugins.hasPlugin("com.android.application")
            val isAndroidLibrary = plugins.hasPlugin("com.android.library")
            if (isAndroidApp || isAndroidLibrary) {
                androidTarget {
                    if (isAndroidLibrary) {
                        publishLibraryVariants("release")
                    }
                }
            }

            jvm()

            js {
                browser()
                nodejs {
                    testTask {
                        useMocha {
                            timeout = "60s"
                        }
                    }
                }
                binaries.executable()
                binaries.library()
            }

            iosX64()
            iosArm64()
            iosSimulatorArm64()

            macosX64()
            macosArm64()
        }

        applyKotlinJsImplicitDependencyWorkaround()
    }
}

val NamedDomainObjectContainer<KotlinSourceSet>.androidUnitTest: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named("androidUnitTest")

val NamedDomainObjectContainer<KotlinSourceSet>.androidInstrumentedTest: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named("androidInstrumentedTest")

fun KotlinSourceSetContainer.jvmCommon() = sourceSet(
    name = "jvmCommon",
    children = listOf("androidMain", "jvmMain"),
)

fun KotlinSourceSetContainer.nonAndroidMain() = sourceSet(
    name = "nonAndroidMain",
    children = listOf("jsMain", "jvmMain", "nativeMain"),
)

fun KotlinSourceSetContainer.nonJsMain() = sourceSet(
    name = "nonJsMain",
    children = listOf("jvmCommon", "nativeMain"),
)

fun KotlinSourceSetContainer.nonJsTest() = sourceSet(
    name = "nonJsTest",
    children = listOf("androidUnitTest", "jvmTest", "nativeTest"),
    isTest = true,
)

fun KotlinSourceSetContainer.nonJvmCommon() = sourceSet(
    name = "nonJvmCommon",
    children = listOf("jsMain", "nativeMain"),
)

fun KotlinSourceSetContainer.sourceSet(
    name: String,
    children: List<String>,
    isTest: Boolean = false,
) {
    val sourceSet = sourceSets.create(name)
    val commonName = if (isTest) "commonTest" else "commonMain"
    sourceSet.dependsOn(sourceSets.getByName(commonName))
    for (child in children) {
        sourceSets.getByName(child).dependsOn(sourceSet)
    }
}

// https://youtrack.jetbrains.com/issue/KT-56025
fun Project.applyKotlinJsImplicitDependencyWorkaround() {
    tasks {
        val configure: Task.() -> Unit = {
            dependsOn(named("jsDevelopmentLibraryCompileSync"))
            dependsOn(named("jsDevelopmentExecutableCompileSync"))
            dependsOn(named("jsProductionLibraryCompileSync"))
            dependsOn(named("jsProductionExecutableCompileSync"))
            dependsOn(named("jsTestTestDevelopmentExecutableCompileSync"))
        }
        named("jsBrowserProductionWebpack").configure(configure)
        named("jsBrowserProductionLibraryPrepare").configure(configure)
        named("jsNodeProductionLibraryPrepare").configure(configure)
    }
}
