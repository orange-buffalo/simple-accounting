plugins {
    id("com.gradle.enterprise").version("3.1")
}

rootProject.name = "simple-accounting"

include("backend")
include("frontend")
include("docker-image")
