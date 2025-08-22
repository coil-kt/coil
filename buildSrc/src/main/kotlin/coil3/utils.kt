package coil3

import kotlin.math.pow
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

val publicModules = setOf(
    "coil",
    "coil-core",
    "coil-compose",
    "coil-compose-core",
    "coil-network-core",
    "coil-network-ktor2",
    "coil-network-ktor3",
    "coil-network-okhttp",
    "coil-network-cache-control",
    "coil-gif",
    "coil-svg",
    "coil-video",
    "coil-test",
)

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

// ./gradlew coil-compose:assemble -PenableComposeMetrics=true
val Project.enableComposeMetrics: Boolean
    get() = booleanProperty("enableComposeMetrics") { false }

// Compose 1.8.0 requires JVM 11 only for the JVM target.
fun Project.applyJvm11OnlyToJvmTarget() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        when {
            name.contains("kotlinjvm", ignoreCase = true) -> {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
            }
            name.contains("kotlinandroid", ignoreCase = true) -> {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_1_8)
            }
        }
    }
}

private fun Project.intProperty(
    name: String,
    default: () -> Int = { error("unknown property: $name") },
): Int = (properties[name] as String?)?.toInt() ?: default()

private fun Project.stringProperty(
    name: String,
    default: () -> String = { error("unknown property: $name") },
): String = (properties[name] as String?) ?: default()

private fun Project.booleanProperty(
    name: String,
    default: () -> Boolean = { error("unknown property: $name") },
): Boolean = (properties[name] as String?)?.toBooleanStrict() ?: default()

private inline fun <T> List<T>.sumByIndexed(selector: (Int, T) -> Int): Int {
    var index = 0
    var sum = 0
    for (element in this) {
        sum += selector(index++, element)
    }
    return sum
}
