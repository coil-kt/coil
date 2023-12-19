package coil3

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

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

            val configureJs: KotlinJsTargetDsl.() -> Unit = {
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
            js(configureJs)
            if (enableWasm) {
                @OptIn(ExperimentalWasmDsl::class)
                wasmJs(configureJs)
            }

            iosX64()
            iosArm64()
            iosSimulatorArm64()

            macosX64()
            macosArm64()
        }

        applyKotlinJsImplicitDependencyWorkaround()
        createSkikoWasmJsRuntimeDependency()
    }
}

val NamedDomainObjectContainer<KotlinSourceSet>.androidUnitTest: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named("androidUnitTest")

val NamedDomainObjectContainer<KotlinSourceSet>.androidInstrumentedTest: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named("androidInstrumentedTest")

// https://youtrack.jetbrains.com/issue/KT-56025
fun Project.applyKotlinJsImplicitDependencyWorkaround() {
    tasks {
        val configureJs: Task.() -> Unit = {
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
        named("jsBrowserProductionWebpack").configure(configureJs)
        named("jsBrowserProductionLibraryPrepare").configure(configureJs)
        named("jsNodeProductionLibraryPrepare").configure(configureJs)

        if (enableWasm) {
            val configureWasmJs: Task.() -> Unit = {
                dependsOn(named("wasmJsDevelopmentLibraryCompileSync"))
                dependsOn(named("wasmJsDevelopmentExecutableCompileSync"))
                dependsOn(named("wasmJsProductionLibraryCompileSync"))
                dependsOn(named("wasmJsProductionExecutableCompileSync"))
                dependsOn(named("wasmJsTestTestDevelopmentExecutableCompileSync"))

                dependsOn(getByPath(":coil:wasmJsDevelopmentLibraryCompileSync"))
                dependsOn(getByPath(":coil:wasmJsDevelopmentExecutableCompileSync"))
                dependsOn(getByPath(":coil:wasmJsProductionLibraryCompileSync"))
                dependsOn(getByPath(":coil:wasmJsProductionExecutableCompileSync"))
                dependsOn(getByPath(":coil:wasmJsTestTestDevelopmentExecutableCompileSync"))
            }
            named("wasmJsBrowserProductionWebpack").configure(configureWasmJs)
            named("wasmJsBrowserProductionLibraryPrepare").configure(configureWasmJs)
            named("wasmJsNodeProductionLibraryPrepare").configure(configureWasmJs)
            named("wasmJsBrowserProductionExecutableDistributeResources").configure {
                dependsOn(named("wasmJsDevelopmentLibraryCompileSync"))
                dependsOn(named("wasmJsDevelopmentExecutableCompileSync"))
                dependsOn(named("wasmJsProductionLibraryCompileSync"))
                dependsOn(named("wasmJsProductionExecutableCompileSync"))
                dependsOn(named("wasmJsTestTestDevelopmentExecutableCompileSync"))
            }
        }
    }
}
