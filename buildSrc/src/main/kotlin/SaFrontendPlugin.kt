@file:Suppress("UnstableApiUsage")

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

private const val NPM_DIST_DIR = "dist"

class SaFrontendPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val npmInstall = project.tasks.register("npmInstall", SaNpmTask::class.java) {
            args.set("install")
            outputDirectories.from(project.file("node_modules"))
        }

        project.tasks.register("npmBuild", SaNpmTask::class.java) {
            args.set("run-script build")
            outputDirectories.from(project.file(NPM_DIST_DIR))
            inputFiles.from(project.file("src"), project.file("public"))

            dependsOn(npmInstall)
        }

        project.tasks.register("npmClean", Delete::class.java) {
            delete(project.files(NPM_DIST_DIR))
        }

        project.tasks.register("npmTest", SaNpmTask::class.java) {
            args.set("run-script test:unit")
            inputFiles.from(project.file("src"), project.file("tests"))

            dependsOn(npmInstall)
        }
    }

}

open class SaNpmTask : DefaultTask() {

    @InputFiles
    val npmConfigs: ConfigurableFileCollection = project.objects.fileCollection()

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
        npmConfigs.from(project.file("package.json"), project.file("package-lock.json"))
    }

    @TaskAction
    fun executeNpm() {
        project.exec {
            commandLine(fullCommandLine.get())
        }
    }
}
