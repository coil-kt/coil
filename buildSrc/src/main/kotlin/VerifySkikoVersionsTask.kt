import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class VerifySkikoVersionsTask : DefaultTask() {

    @get:Input
    abstract val coreRequestedSkikoVersion: Property<String>

    @get:Input
    abstract val composeRequestedSkikoVersion: Property<String>

    @TaskAction
    fun verify() {
        val coreSkiko = coreRequestedSkikoVersion.get()
        val composeSkiko = composeRequestedSkikoVersion.get()
        if (coreSkiko != composeSkiko) {
            error("Skiko version mismatch: coil-core uses $coreSkiko, coil-compose-core uses $composeSkiko. Update libs.toml to use $composeSkiko.")
        }
    }
}
