plugins {
    id("com.gradle.enterprise").version("3.10.2")
}

rootProject.name = "simple-accounting"

include("frontend")
include("app")
include("tests")
