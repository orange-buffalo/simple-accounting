plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.enterprise").version("3.14.1")
}

rootProject.name = "simple-accounting"

include("frontend")
include("app")
