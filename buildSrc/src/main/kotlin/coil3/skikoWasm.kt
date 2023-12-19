/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package coil3

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.UnresolvedDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.gradle.kotlin.dsl.task
import org.jetbrains.compose.experimental.web.tasks.ExperimentalUnpackSkikoWasmRuntimeTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

// This file is copied and modified from the Compose Multiplatform plugin so we can create the Skiko
// wasm runtime without requiring coil-core to apply the Compose Multiplatform plugin and have a
// Compose runtime dependency.
// https://github.com/JetBrains/compose-multiplatform/blob/master/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/experimental/web/internal/configureExperimentalWebApplication.kt

fun Project.createSkikoWasmJsRuntimeDependency() {
    if (plugins.hasPlugin("org.jetbrains.compose")) {
        // This process is already handled by the Compose plugin.
        return
    }

    afterEvaluate {
        extensions.findByType<KotlinMultiplatformExtension>()!!
            .targets.asMap.values.filterIsInstanceTo(mutableSetOf<KotlinJsIrTarget>())
            .configureExperimentalWebApplication(project)
    }
}

private fun Collection<KotlinJsIrTarget>.configureExperimentalWebApplication(project: Project) {
    val skikoJsWasmRuntimeConfiguration = project.configurations.create("skikoJsWasmRuntime")
    val skikoJsWasmRuntimeDependency = skikoVersionProvider(project).map { skikoVersion ->
        project.dependencies.create("$SKIKO_GROUP:skiko-js-wasm-runtime:$skikoVersion")
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
        val unpackRuntime = project.task<ExperimentalUnpackSkikoWasmRuntimeTask>(taskName) {
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

private fun skikoVersionProvider(project: Project): Provider<String> {
    return project.provider {
        val skikoDependency = project.configurations.firstNotNullOfOrNull { configuration ->
            configuration.allDependenciesDescriptors.find(::isSkikoDependency)
        }
        checkNotNull(skikoDependency?.version) { "Cannot determine the version of Skiko." }
    }
}

private fun isSkikoDependency(dep: DependencyDescriptor): Boolean {
    return dep.group == SKIKO_GROUP && dep.version != null
}

private val Configuration.allDependenciesDescriptors: Sequence<DependencyDescriptor>
    get() = with(resolvedConfiguration.lenientConfiguration) {
        allModuleDependencies.asSequence().map(::ResolvedDependencyDescriptor) +
            unresolvedModuleDependencies.asSequence().map(::UnresolvedDependencyDescriptor)
    }

private interface DependencyDescriptor {
    val group: String?
    val name: String?
    val version: String?
}

@JvmInline
private value class ResolvedDependencyDescriptor(
    private val dependency: ResolvedDependency,
) : DependencyDescriptor {

    override val group: String?
        get() = dependency.moduleGroup

    override val name: String?
        get() = dependency.moduleName

    override val version: String?
        get() = dependency.moduleVersion
}

@JvmInline
private value class UnresolvedDependencyDescriptor(
    private val dependency: UnresolvedDependency,
) : DependencyDescriptor {

    override val group: String?
        get() = dependency.selector.group

    override val name: String?
        get() = dependency.selector.name

    override val version: String?
        get() = dependency.selector.version
}
