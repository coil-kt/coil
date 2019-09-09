@file:Suppress("unused")

package coil

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.kotlin
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
    get() {
        return versionName
            .trimEnd { !it.isDigit() }
            .split('.')
            .map { it.toInt() }
            .reversed()
            .sumByIndexed { index, unit ->
                // 1.2.3 -> 102030
                (unit * 10.0.pow(2 * index + 1)).toInt()
            }
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

fun DependencyHandler.testImplementation(dependencyNotation: Any): Dependency? {
    return add("testImplementation", dependencyNotation)
}

fun DependencyHandler.androidTestImplementation(dependencyNotation: Any): Dependency? {
    return add("androidTestImplementation", dependencyNotation)
}

fun DependencyHandler.addTestDependencies(kotlinVersion: String) {
    testImplementation(kotlin("test-junit", kotlinVersion))
    testImplementation(Library.Kotlin.Coroutines.test)

    testImplementation(Library.AndroidX.Test.core)
    testImplementation(Library.AndroidX.Test.junit)
    testImplementation(Library.AndroidX.Test.rules)
    testImplementation(Library.AndroidX.Test.runner)

    testImplementation(Library.Square.OkHttp.mockWebServer)
    testImplementation(Library.Robolectric.robolectric)
}

fun DependencyHandler.addAndroidTestDependencies(kotlinVersion: String) {
    androidTestImplementation(kotlin("test-junit", kotlinVersion))

    androidTestImplementation(Library.AndroidX.Test.core)
    androidTestImplementation(Library.AndroidX.Test.junit)
    androidTestImplementation(Library.AndroidX.Test.rules)
    androidTestImplementation(Library.AndroidX.Test.runner)

    androidTestImplementation(Library.Square.OkHttp.mockWebServer)
}
