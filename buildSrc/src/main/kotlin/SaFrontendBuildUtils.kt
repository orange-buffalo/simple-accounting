@file:Suppress("UnstableApiUsage")

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import java.io.File

abstract class SaFrontendTask : DefaultTask() {

    @Input
    val args: Property<String> = project.objects.property(String::class.java)

    private val fullCommandLine = args.map { userInput ->
        listOf(
            "bun",
            *(userInput.split(" ").map { it.trim() }.toTypedArray())
        )
    }

    init {
        group = "Frontend"
    }

    @TaskAction
    fun executeFrontendTask() {
        project.exec {
            commandLine(fullCommandLine.get())
        }
    }
}

@CacheableTask
abstract class SaCacheableFrontendTask : SaFrontendTask() {

    private val inputFilesPatterns: PatternSet = PatternSet()
    private val outputFilesPatterns: PatternSet = PatternSet()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    val inputFiles: Provider<Set<File>> = project.provider {
        project.files(project.projectDir).asFileTree.matching(inputFilesPatterns).files
    }

    @OutputDirectories
    @Optional
    val outputDirectories: Property<ConfigurableFileCollection> =
        project.objects.property(ConfigurableFileCollection::class.java)

    fun inputFiles(spec: PatternFilterable.() -> Unit) {
        spec(this.inputFilesPatterns)
    }
}

@Serializable
internal data class TsConfigInternal(
    val extends: String? = null,
    val include: Array<String>? = null,
    val exclude: Array<String>? = null
)

data class TsConfig(
    val include: Array<String>?,
    val exclude: Array<String>?,
) {
    fun applyIncludesExcludes(patternSet: PatternFilterable) {
        include?.forEach {
            patternSet.include(it)
        }
        exclude?.forEach {
            patternSet.exclude(it)
        }
    }
}

fun readTsConfig(targetConfigFile: File): TsConfig {
    val json = Json {
        ignoreUnknownKeys = true
    }
    val configsHierarchy = mutableListOf<TsConfig>()
    var currentConfigFile: File? = targetConfigFile
    while (currentConfigFile != null) {
        val currentFileConfig = json.decodeFromString<TsConfigInternal>(currentConfigFile.readText())
        configsHierarchy.add(TsConfig(include = currentFileConfig.include, exclude = currentFileConfig.exclude))
        if (currentFileConfig.extends != null) {
            val extendsFile = File(targetConfigFile.parentFile, currentFileConfig.extends)
            if (extendsFile.exists()) {
                currentConfigFile = extendsFile
                continue
            }
        }
        currentConfigFile = null
    }
    return configsHierarchy.reduceRight { childConfig, parentConfig ->
        TsConfig(
            include = childConfig.include ?: parentConfig.include,
            exclude = childConfig.exclude ?: parentConfig.exclude,
        )
    }
}
