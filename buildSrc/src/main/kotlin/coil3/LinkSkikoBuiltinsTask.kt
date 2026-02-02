package coil3

import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem
import org.gradle.process.ExecOperations

abstract class LinkSkikoBuiltinsTask @Inject constructor() : DefaultTask() {

    init {
        outputs.upToDateWhen { false }
    }

    @get:InputDirectory
    abstract val ndkDirectory: DirectoryProperty

    @get:Input
    abstract val builtinsArch: Property<String>

    @get:Input
    abstract val triple: Property<String>

    @get:Input
    abstract val apiLevel: Property<Int>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun link() {
        val ndkDir = ndkDirectory.get().asFile
        val hostPrebuiltDir = resolveHostPrebuiltDir(ndkDir)
        val clangVersionDir = findLatestClangDir(hostPrebuiltDir)
        val builtinsArchive = clangVersionDir.resolve("lib/linux/libclang_rt.builtins-${builtinsArch.get()}-android.a")
        val clangExecutable = hostPrebuiltDir.resolve("bin/${triple.get()}${apiLevel.get()}-clang")
        val llvmArExecutable = hostPrebuiltDir.resolve("bin/llvm-ar")

        val output = outputFile.get().asFile
        output.parentFile.mkdirs()

        val filteredArchive = File(temporaryDir, "libclang_rt.builtins-${builtinsArch.get()}-filtered.a")
        builtinsArchive.copyTo(filteredArchive, overwrite = true)

        val listOutput = ByteArrayOutputStream()
        execOperations.exec {
            executable = llvmArExecutable.absolutePath
            args("t", filteredArchive.absolutePath)
            standardOutput = listOutput
        }
        val archiveEntries = listOutput.toString().lineSequence().filter { it.isNotBlank() }.toList()
        val ldaddObjects = archiveEntries.filter { it.startsWith("outline_atomic_ldadd") }
        ldaddObjects.forEach { entry ->
            execOperations.exec {
                executable = llvmArExecutable.absolutePath
                args("d", filteredArchive.absolutePath, entry)
            }
        }

        val shimSource = File(temporaryDir, "skiko_builtins_shim_${builtinsArch.get()}.c")
        shimSource.writeText(generateShimSource())

        val shimObject = File(temporaryDir, "skiko_builtins_shim_${builtinsArch.get()}.o")
        execOperations.exec {
            executable = clangExecutable.absolutePath
            args(
                "-fPIC",
                "-O2",
                "-std=c11",
                "-c",
                shimSource.absolutePath,
                "-o",
                shimObject.absolutePath,
            )
        }

        execOperations.exec {
            executable = clangExecutable.absolutePath
            args(
                "-shared",
                "-nostdlib",
                shimObject.absolutePath,
                "-Wl,--whole-archive,${filteredArchive.absolutePath}",
                "-Wl,--no-whole-archive",
                "-llog",
                "-landroid",
                "-lm",
                "-lc",
                "-latomic",
                "-o",
                output.absolutePath,
            )
        }
    }

    private fun resolveHostPrebuiltDir(ndkDir: File): File {
        val prebuiltRoot = ndkDir.resolve("toolchains/llvm/prebuilt")
        val candidateNames = hostCandidateNames()
        require(candidateNames.isNotEmpty()) { "Unsupported host OS: ${System.getProperty("os.name")}" }

        return candidateNames
            .map { prebuiltRoot.resolve(it) }
            .firstOrNull { it.exists() }
            ?: error("Unable to locate NDK prebuilt toolchain in ${prebuiltRoot.absolutePath} for host OS ${System.getProperty("os.name")}")
    }

    private fun findLatestClangDir(hostPrebuiltDir: File): File {
        val clangRoot = hostPrebuiltDir.resolve("lib/clang")
        val candidates = clangRoot.listFiles()?.filter(File::isDirectory).orEmpty()
        return candidates.maxWithOrNull { a, b -> compareVersionStrings(a.name, b.name) }
            ?: error("Unable to locate clang version directory in ${clangRoot.absolutePath}")
    }

    private fun compareVersionStrings(a: String, b: String): Int {
        val aParts = a.split('.')
        val bParts = b.split('.')
        val size = maxOf(aParts.size, bParts.size)
        for (index in 0 until size) {
            val aPart = aParts.getOrNull(index)?.toIntOrNull() ?: 0
            val bPart = bParts.getOrNull(index)?.toIntOrNull() ?: 0
            if (aPart != bPart) return aPart.compareTo(bPart)
        }
        return 0
    }

    private fun hostCandidateNames(): List<String> {
        val os = OperatingSystem.current()
        return when {
            os.isMacOsX -> listOf("darwin-aarch64", "darwin-x86_64")
            os.isLinux -> listOf("linux-x86_64")
            os.isWindows -> listOf("windows-x86_64")
            else -> emptyList()
        }
    }

    private fun generateShimSource(): String {
        val builder = StringBuilder()
        builder.appendLine("#include <stdint.h>")
        builder.appendLine("#include <stdatomic.h>")
        builder.appendLine("typedef unsigned __int128 uint128_t;")
        builder.appendLine()

        val typeMap = listOf(
            1 to "uint8_t",
            2 to "uint16_t",
            4 to "uint32_t",
            8 to "uint64_t",
            16 to "uint128_t",
        )
        val memoryOrders = mapOf(
            "relax" to "__ATOMIC_RELAXED",
            "rel" to "__ATOMIC_RELEASE",
            "acq" to "__ATOMIC_ACQUIRE",
            "acq_rel" to "__ATOMIC_ACQ_REL",
        )

        for ((bits, cType) in typeMap) {
            for ((suffix, orderConst) in memoryOrders) {
                builder.appendLine("__attribute__((visibility(\"default\"))) $cType __aarch64_ldadd${bits}_$suffix($cType value, $cType *ptr) {")
                builder.appendLine("    return __atomic_fetch_add(ptr, value, $orderConst);")
                builder.appendLine("}")
                builder.appendLine()
            }
        }

        return builder.toString()
    }
}
