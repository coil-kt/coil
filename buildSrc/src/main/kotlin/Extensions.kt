@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.project
import kotlin.math.pow

val Project.minSdk: Int
    get() = intProperty("minSdk")

val Project.targetSdk: Int
    get() = intProperty("targetSdk")

val Project.compileSdk: Int
    get() = intProperty("compileSdk")

val Project.groupId: String
    get() = stringProperty("GROUP")

val Project.versionName: String
    get() = stringProperty("VERSION_NAME")

val Project.versionCode: Int
    get() = versionName
        .takeWhile { it.isDigit() || it == '.' }
        .split('.')
        .map { it.toInt() }
        .reversed()
        .sumByIndexed { index, unit ->
            // 1.2.3 -> 102030
            (unit * 10.0.pow(2 * index + 1)).toInt()
        }

private fun Project.intProperty(name: String): Int {
    return (property(name) as String).toInt()
}

private fun Project.stringProperty(name: String): String {
    return property(name) as String
}

private inline fun <T> List<T>.sumByIndexed(selector: (Int, T) -> Int): Int {
    var index = 0
    var sum = 0
    for (element in this) {
        sum += selector(index++, element)
    }
    return sum
}

private fun DependencyHandler.testImplementation(vararg names: Any): Array<Dependency?> =
    names.map {
        add("testImplementation", it)
    }.toTypedArray()

private fun DependencyHandler.androidTestImplementation(vararg names: Any): Array<Dependency?> =
    names.map {
        add("androidTestImplementation", it)
    }.toTypedArray()

fun DependencyHandler.addTestDependencies(kotlinVersion: String) {
    testImplementation(
        project(":coil-test"),

        Library.JUNIT,
        kotlin("test-junit", kotlinVersion),

        Library.KOTLINX_COROUTINES_TEST,

        Library.ANDROIDX_TEST_CORE,
        Library.ANDROIDX_TEST_JUNIT,
        Library.ANDROIDX_TEST_RULES,
        Library.ANDROIDX_TEST_RUNNER,

        Library.OKHTTP_MOCK_WEB_SERVER,

        Library.ROBOLECTRIC
    )
}

fun DependencyHandler.addAndroidTestDependencies(kotlinVersion: String, includeTestProject: Boolean = true) {
    if (includeTestProject) {
        androidTestImplementation(project(":coil-test"))
    }

    androidTestImplementation(
        Library.JUNIT,
        kotlin("test-junit", kotlinVersion),

        Library.ANDROIDX_APPCOMPAT,
        Library.MATERIAL,

        Library.ANDROIDX_TEST_CORE,
        Library.ANDROIDX_TEST_JUNIT,
        Library.ANDROIDX_TEST_RULES,
        Library.ANDROIDX_TEST_RUNNER,

        Library.OKHTTP_MOCK_WEB_SERVER
    )
}

fun Project.setupBase(block: LibraryExtension.() -> Unit = {}): LibraryExtension {
    return (extensions.getByName<BaseExtension>("android") as LibraryExtension).apply {
        compileSdkVersion(project.compileSdk)
        defaultConfig {
            minSdkVersion(project.minSdk)
            targetSdkVersion(project.targetSdk)
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        libraryVariants.all {
            generateBuildConfigProvider?.configure { enabled = false }
        }
        testOptions {
            unitTests.isIncludeAndroidResources = true
        }
        block()
    }
}

inline infix fun <T> Property<T>.by(value: T) = set(value)

inline infix fun <T> SetProperty<T>.by(value: Set<T>) = set(value)
