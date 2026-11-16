plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.develocity").version("4.6.1")
}

rootProject.name = "simple-accounting"

include("frontend")
include("app")
