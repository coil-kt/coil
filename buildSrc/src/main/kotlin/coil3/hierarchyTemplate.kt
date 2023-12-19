@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package coil3

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree.Companion.main
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree.Companion.test

private val hierarchyTemplate = KotlinHierarchyTemplate {
    withSourceSetTree(main, test)
    common {
        withCompilations { true }

        groupNonAndroid()
        groupJsCommon()
        groupNonJsCommon()
        groupJvmCommon()
        groupNonJvmCommon()
        groupNative()
        groupNonWasmJs()
    }
}

private fun KotlinHierarchyBuilder.groupNonAndroid() {
    group("nonAndroid") {
        withJvm()
        groupJsCommon()
        groupNative()
    }
}

private fun KotlinHierarchyBuilder.groupJsCommon() {
    group("jsCommon") {
        withJs()
        withWasm()
    }
}

private fun KotlinHierarchyBuilder.groupNonJsCommon() {
    group("nonJsCommon") {
        groupJvmCommon()
        groupNative()
    }
}

private fun KotlinHierarchyBuilder.groupJvmCommon() {
    group("jvmCommon") {
        withAndroidTarget()
        withJvm()
    }
}

private fun KotlinHierarchyBuilder.groupNonJvmCommon() {
    group("nonJvmCommon") {
        groupJsCommon()
        groupNative()
    }
}

private fun KotlinHierarchyBuilder.groupNative() {
    group("native") {
        withNative()

        group("apple") {
            withApple()

            group("ios") {
                withIos()
            }

            group("macos") {
                withMacos()
            }
        }
    }
}

// TODO: Remove when wasm tests are fixed.
private fun KotlinHierarchyBuilder.groupNonWasmJs() {
    group("nonWasmJs") {
        withJvm()
        withJs()
        groupNative()
    }
}

fun KotlinMultiplatformExtension.applyCoilHierarchyTemplate() {
    applyHierarchyTemplate(hierarchyTemplate)
}
