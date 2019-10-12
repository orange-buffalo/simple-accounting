import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage

plugins {
    id("com.bmuschko.docker-remote-api") version Versions.dockerPlugin
}

docker {
    registryCredentials {
        username.set(project.properties["docker.hub.username"] as String?)
        password.set(project.properties["docker.hub.password"] as String?)
    }
}

val dockerBuildDir = "$buildDir/docker-build/"

val prepareDockerBuild = tasks.register("prepareDockerBuild") {
    doLast {
        copy {
            from(tasks.getByPath(":backend:bootJar").outputs)
            into(dockerBuildDir)
            include("*.jar")
            rename("""(.*)\.jar""", "app.jar")
        }

        copy {
            from("src/main/docker")
            into(dockerBuildDir)
        }
    }

    dependsOn(":backend:bootJar")
}

val buildDockerImage = tasks.register<DockerBuildImage>("buildDockerImage") {
    inputDir.set(file(dockerBuildDir))
    tags.add("orangebuffalo/simple-accounting:${project.version}")
    tags.add("orangebuffalo/simple-accounting:latest")
    dependsOn(prepareDockerBuild)
}

tasks.register<DockerPushImage>("pushDockerImage") {
    imageName.set("orangebuffalo/simple-accounting")
    tag.set("latest")
    dependsOn(buildDockerImage)
}