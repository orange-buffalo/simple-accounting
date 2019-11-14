import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage

plugins {
    id("com.bmuschko.docker-remote-api") version Versions.dockerPlugin
}

apply<SaDockerPlugin>()

docker {
    registryCredentials {
        username.set(project.properties["docker.hub.username"] as String?)
        password.set(project.properties["docker.hub.password"] as String?)
    }
}

val saDockerImageExtension = the<SaDockerImageExtension>()

configure<SaDockerImageExtension> {
    imageName.set("orangebuffalo/simple-accounting")
    imageTag.set("${project.version}")
    dockerBuildDir.set(project.layout.buildDirectory.map { projectBuildDir ->
        projectBuildDir.dir("docker-build")
    })

}

val buildDockerImage = tasks.register<DockerBuildImage>("buildDockerImage") {
    inputDir.set(saDockerImageExtension.dockerBuildDir)
    tags.add(saDockerImageExtension.imageName.flatMap { imageName ->
        saDockerImageExtension.imageTag.map { imageTag ->
            "${imageName}:${imageTag}"
        }
    })
    dependsOn("prepareDockerBuild")
}

project.tasks.register<DockerPushImage>("pushDockerImage") {
    imageName.set(saDockerImageExtension.imageName)
    tag.set(saDockerImageExtension.imageTag)
    dependsOn(buildDockerImage)
}

tasks.register("build") {
    dependsOn(buildDockerImage)
}
