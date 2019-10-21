@file:Suppress("UnstableApiUsage")

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

class FrontendPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val npmInstall = project.tasks.register("npmInstall", NpmTask::class.java) {
            args.set("install")
            outputDirectories.from(project.file("node_modules"))
        }

        project.tasks.register("npmBuild", NpmTask::class.java) {
            args.set("run-script build")
            outputDirectories.from(project.file("dist"))
            inputFiles.from(project.file("src"), project.file("public"))

            dependsOn(npmInstall)
        }
    }

}

open class NpmTask @javax.inject.Inject constructor(objects: ObjectFactory) : DefaultTask() {

    @InputFile
    val packageJson: RegularFileProperty = objects.fileProperty()

    @InputFiles
    val inputFiles: ConfigurableFileCollection = objects.fileCollection()

    @OutputDirectories
    val outputDirectories: ConfigurableFileCollection = objects.fileCollection()

    @Input
    val args: Property<String> = objects.property(String::class.java)

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
