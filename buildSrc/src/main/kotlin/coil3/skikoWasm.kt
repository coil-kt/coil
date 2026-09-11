/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package coil3

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Sync
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

// This file is copied and modified from the Compose Multiplatform plugin so we can create the Skiko
// wasm runtime without requiring coil-core to apply the Compose Multiplatform plugin and have a
// Compose runtime dependency.
// https://github.com/JetBrains/compose-multiplatform/blob/master/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/web/internal/configureWebApplication.kt

fun Project.createSkikoWasmJsRuntimeDependency() {
    if (plugins.hasPlugin("org.jetbrains.compose")) {
        // This process is already handled by the Compose plugin.
        return
    }

    afterEvaluate {
        extensions.findByType<KotlinMultiplatformExtension>()!!
            .targets.withType(KotlinJsIrTarget::class.java).configureEach {
                configureSkikoWebRuntime()
            }
    }
}

private fun KotlinJsIrTarget.configureSkikoWebRuntime() {
    val mainCompilation = compilations.getByName("main")
    val runtimeConfiguration = project.configurations.getByName(mainCompilation.runtimeDependencyConfigurationName)
    val skikoRuntimeJars = runtimeConfiguration.incoming.artifactView {
        withVariantReselection()
        attributes {
            runtimeConfiguration.attributes.keySet().forEach {
                @Suppress("UNCHECKED_CAST")
                attribute(it as Attribute<Any>, runtimeConfiguration.attributes.getAttribute(it)!!)
            }
            attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "skiko-runtime"))
        }
    }.files
    val skikoRuntimeFiles = skikoRuntimeJars.elements.map { artifacts ->
        artifacts.map { project.zipTree(it) }
    }
    val unpackedRuntimeDir = project.layout.buildDirectory.dir("compose/skiko-wasm/$targetName")
    val unpackRuntime = project.tasks.register<Sync>("unpackSkikoWasmRuntime${targetName.uppercaseFirstChar()}") {
        from(skikoRuntimeFiles)
        into(unpackedRuntimeDir)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    compilations.configureEach {
        if (target.wasmTargetType != null) {
            // Kotlin/Wasm imports the runtime as ES modules during linking.
            binaries.configureEach {
                linkSyncTask.configure {
                    dependsOn(unpackRuntime)
                    from.from(unpackedRuntimeDir)
                }
            }
        } else {
            project.tasks.named(processResourcesTaskName, ProcessResources::class.java).configure {
                from(unpackRuntime)
                exclude("META-INF")
            }
        }
    }
}
