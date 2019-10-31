@file:Suppress("UnstableApiUsage")

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

class SaFrontendPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val npmInstall = project.tasks.register("npmInstall", SaNpmTask::class.java) {
            args.set("install")
            outputDirectories.from(project.file("node_modules"))
        }

        project.tasks.register("npmBuild", SaNpmTask::class.java) {
            args.set("run-script build")
            outputDirectories.from(project.file("dist"))
            inputFiles.from(project.file("src"), project.file("public"))

            dependsOn(npmInstall)
        }
    }

}

open class SaNpmTask : DefaultTask() {

    @InputFile
    val packageJson: RegularFileProperty = project.objects.fileProperty()

    @InputFiles
    val inputFiles: ConfigurableFileCollection = project.objects.fileCollection()

    @OutputDirectories
    val outputDirectories: ConfigurableFileCollection = project.objects.fileCollection()

    @Input
    val args: Property<String> = project.objects.property(String::class.java)

    private val fullCommandLine = args.map { userInput ->
        listOf(
            "npm",
            *(userInput.split(" ").map { it.trim() }.toTypedArray())
        )
    }

    init {
        packageJson.set(project.file("package.json"))
    }

    @TaskAction
    fun executeNpm() {
        project.exec {
            commandLine(fullCommandLine.get())
        }
    }
}
