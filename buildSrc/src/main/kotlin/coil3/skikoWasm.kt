/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package coil3

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.compose.web.tasks.UnpackSkikoWasmRuntimeTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

// This file is copied and modified from the Compose Multiplatform plugin so we can create the Skiko
// wasm runtime without requiring coil-core to apply the Compose Multiplatform plugin and have a
// Compose runtime dependency.
// https://github.com/JetBrains/compose-multiplatform/blob/master/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/experimental/web/internal/configureExperimentalWebApplication.kt

fun Project.createSkikoWasmJsRuntimeDependency(skikoVersion: Provider<String>) {
    if (plugins.hasPlugin("org.jetbrains.compose")) {
        // This process is already handled by the Compose plugin.
        return
    }

    afterEvaluate {
        extensions.findByType<KotlinMultiplatformExtension>()!!
            .targets.asMap.values.filterIsInstanceTo(mutableSetOf<KotlinJsIrTarget>())
            .configureExperimentalWebApplication(project, skikoVersion)
    }
}

private fun Collection<KotlinJsIrTarget>.configureExperimentalWebApplication(project: Project, skikoVersion: Provider<String>) {
    val skikoJsWasmRuntimeConfiguration = project.configurations.create("skikoJsWasmRuntime")
    val skikoJsWasmRuntimeDependency = skikoVersion.map { version ->
        project.dependencies.create("$SKIKO_GROUP:skiko-js-wasm-runtime:$version")
    }
    skikoJsWasmRuntimeConfiguration.defaultDependencies {
        addLater(skikoJsWasmRuntimeDependency)
    }
    forEach {
        val mainCompilation = it.compilations.getByName("main")
        val testCompilation = it.compilations.getByName("test")
        val unpackedRuntimeDir = project.layout.buildDirectory.dir("compose/skiko-wasm/${it.targetName}")
        mainCompilation.defaultSourceSet.resources.srcDir(unpackedRuntimeDir)
        testCompilation.defaultSourceSet.resources.srcDir(unpackedRuntimeDir)

        val taskName = "unpackSkikoWasmRuntime${it.targetName.uppercaseFirstChar()}"
        val unpackRuntime = project.tasks.register<UnpackSkikoWasmRuntimeTask>(taskName) {
            skikoRuntimeFiles = skikoJsWasmRuntimeConfiguration
            outputDir.set(unpackedRuntimeDir)
        }
        project.tasks.named(mainCompilation.processResourcesTaskName).configure {
            dependsOn(unpackRuntime)
        }
        project.tasks.named(testCompilation.processResourcesTaskName).configure {
            dependsOn(unpackRuntime)
        }
    }
}

private const val SKIKO_GROUP = "org.jetbrains.skiko"
