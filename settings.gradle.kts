// todo: enables spring milestone repo - should be cleaned one we migrate to release
pluginManagement {
  repositories {
    maven { url = uri("https://repo.spring.io/milestone") }
    gradlePluginPortal()
  }
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == "org.springframework.boot") {
        useModule("org.springframework.boot:spring-boot-gradle-plugin:${requested.version}")
      }
    }
  }
}

plugins {
    id("com.gradle.enterprise").version("3.1")
}

rootProject.name = "simple-accounting"

include("backend")
include("frontend")
include("docker-image")
