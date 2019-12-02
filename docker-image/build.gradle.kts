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
    image.set("orangebuffalo/simple-accounting:${project.version}")
    dockerBuildDir.set(project.layout.buildDirectory.map { projectBuildDir ->
        projectBuildDir.dir("docker-build")
    })

}

val buildDockerImage = tasks.register<DockerBuildImage>("buildDockerImage") {
    inputDir.set(saDockerImageExtension.dockerBuildDir)
    images.add(saDockerImageExtension.image)
    dependsOn("prepareDockerBuild")
}

project.tasks.register<DockerPushImage>("pushDockerImage") {
    images.add(saDockerImageExtension.image)
    dependsOn(buildDockerImage)
}

tasks.register("build") {
    dependsOn(buildDockerImage)
}
