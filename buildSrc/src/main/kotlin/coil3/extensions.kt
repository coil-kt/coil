package coil3

import kotlin.math.pow
import org.gradle.api.Project

val Project.minSdk: Int
    get() = intProperty("minSdk")

val Project.targetSdk: Int
    get() = intProperty("targetSdk")

val Project.compileSdk: Int
    get() = intProperty("compileSdk")

val Project.groupId: String
    get() = stringProperty("POM_GROUP_ID")

val Project.versionName: String
    get() = stringProperty("POM_VERSION")

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

val publicModules = listOf(
    "coil",
    "coil-core",
    "coil-compose",
    "coil-compose-core",
    "coil-network",
    "coil-gif",
    "coil-svg",
    "coil-video",
    "coil-test",
)

val privateModules = listOf(
    "coil-test-internal",
    "coil-test-paparazzi",
    "coil-test-roborazzi",
    "compose", // samples:compose
    "shared", // samples:shared
    "view", // samples:view
)

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
