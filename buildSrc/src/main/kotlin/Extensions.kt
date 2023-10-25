package coil

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

val multiplatformModules = listOf(
    "coil-base",
    "coil-singleton",
    "coil-compose-base",
    "coil-compose-singleton",
    "coil-test",
)

val publicModules = listOf(
    "coil-base",
    "coil-singleton",
    "coil-compose-base",
    "coil-compose-singleton",
    "coil-gif",
    "coil-svg",
    "coil-video",
    "coil-test",
)

val privateModules = listOf(
    "coil-sample-common",
    "coil-sample-compose",
    "coil-sample-view",
    "coil-test-internal",
    "coil-test-paparazzi",
    "coil-test-roborazzi",
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
