package coil3

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

fun Project.addAllMultiplatformTargets() {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            applyCoilHierarchyTemplate()

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

// https://youtrack.jetbrains.com/issue/KT-56025
fun Project.applyKotlinJsImplicitDependencyWorkaround() {
    tasks {
        val configure: Task.() -> Unit = {
            dependsOn(named("jsDevelopmentLibraryCompileSync"))
            dependsOn(named("jsDevelopmentExecutableCompileSync"))
            dependsOn(named("jsProductionLibraryCompileSync"))
            dependsOn(named("jsProductionExecutableCompileSync"))
            dependsOn(named("jsTestTestDevelopmentExecutableCompileSync"))

            dependsOn(getByPath(":coil:jsDevelopmentLibraryCompileSync"))
            dependsOn(getByPath(":coil:jsDevelopmentExecutableCompileSync"))
            dependsOn(getByPath(":coil:jsProductionLibraryCompileSync"))
            dependsOn(getByPath(":coil:jsProductionExecutableCompileSync"))
            dependsOn(getByPath(":coil:jsTestTestDevelopmentExecutableCompileSync"))
        }
        named("jsBrowserProductionWebpack").configure(configure)
        named("jsBrowserProductionLibraryPrepare").configure(configure)
        named("jsNodeProductionLibraryPrepare").configure(configure)
    }
}
