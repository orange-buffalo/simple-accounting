@file:Suppress("UnstableApiUsage")

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.register
import java.io.File

open class SaDockerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create<SaDockerImageExtension>("saDockerImage")

         project.tasks.register<SaPrepareDockerBuild>("prepareDockerBuild") {
             dockerBuildDir.set(extension.dockerBuildDir)
             bootJar.set(project.tasks.getByPath(":backend:bootJar").outputs.files)
             dockerSourceDir.set(project.file("src/main/docker"))
         }
    }

}

open class SaDockerImageExtension(objects: ObjectFactory) {
    var imageName : Property<String> = objects.property()
    var imageTag : Property<String> = objects.property()
    var dockerBuildDir : Property<Directory> = objects.directoryProperty()
}

open class SaPrepareDockerBuild : DefaultTask() {

    @OutputDirectory
    val dockerBuildDir : Property<Directory> = project.objects.directoryProperty()

    @InputFiles
    val bootJar : Property<FileCollection> = project.objects.property()

    @InputDirectory
    val dockerSourceDir : Property<File> = project.objects.property()

    @TaskAction
    fun prepareBuildFiles() {
        project.copy {
            from(bootJar)
            into(dockerBuildDir)
            include("*.jar")
            rename("""(.*)\.jar""", "app.jar")
        }

        project.copy {
            from(dockerSourceDir)
            into(dockerBuildDir)
        }
    }
}
