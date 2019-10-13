import com.moowork.gradle.node.npm.NpmTask

plugins {
    id("com.moowork.node") version Versions.nodePlugin
}

node {
    download = false
}

tasks.register<NpmTask>("npmBuild") {
    setArgs(listOf("run", "build"))
    dependsOn("npmInstall")
}

tasks.register("build") {
    dependsOn("npmBuild")
}