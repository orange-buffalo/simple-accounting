plugins {
    id("com.gradle.enterprise").version("3.14.1")
}

rootProject.name = "simple-accounting"

include("frontend")
include("app")
