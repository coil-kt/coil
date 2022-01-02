@file:Suppress("NOTHING_TO_INLINE")

package coil

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
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

private fun DependencyHandler.testImplementation(dependencyNotation: Any): Dependency? {
    return add("testImplementation", dependencyNotation)
}

private fun DependencyHandler.androidTestImplementation(dependencyNotation: Any): Dependency? {
    return add("androidTestImplementation", dependencyNotation)
}

fun DependencyHandler.addTestDependencies(kotlinVersion: String) {
    testImplementation(project(":coil-test"))

    testImplementation(Library.JUNIT)
    testImplementation(kotlin("test-junit", kotlinVersion))

    testImplementation(Library.KOTLINX_COROUTINES_TEST)

    testImplementation(Library.ANDROIDX_TEST_CORE)
    testImplementation(Library.ANDROIDX_TEST_JUNIT)
    testImplementation(Library.ANDROIDX_TEST_RULES)
    testImplementation(Library.ANDROIDX_TEST_RUNNER)

    testImplementation(Library.OKHTTP_MOCK_WEB_SERVER)
    testImplementation(Library.OKIO_FAKE_FILESYSTEM)

    testImplementation(Library.ROBOLECTRIC)
}

fun DependencyHandler.addAndroidTestDependencies(kotlinVersion: String) {
    androidTestImplementation(project(":coil-test"))

    androidTestImplementation(Library.JUNIT)
    androidTestImplementation(kotlin("test-junit", kotlinVersion))

    androidTestImplementation(Library.KOTLINX_COROUTINES_TEST)

    androidTestImplementation(Library.MATERIAL)

    androidTestImplementation(Library.ANDROIDX_TEST_CORE)
    androidTestImplementation(Library.ANDROIDX_TEST_JUNIT)
    androidTestImplementation(Library.ANDROIDX_TEST_RULES)
    androidTestImplementation(Library.ANDROIDX_TEST_RUNNER)

    androidTestImplementation(Library.OKHTTP_MOCK_WEB_SERVER)
}

inline infix fun <T> Property<T>.by(value: T) = set(value)

inline infix fun <T> SetProperty<T>.by(value: Set<T>) = set(value)
