buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm")
    id("io.spring.dependency-management")
}

sourceSets {
    create("e2eTest")
    create("storybookTest") {
        // do not add this dependency in dev to avoid storybook rebuild on each change,
        // as we recommend to run tests against running storybook locally
        if (System.getenv("CI") == "true") {
            resources {
                srcDirs(tasks.getByPath(":frontend:buildStorybook"))
            }
        }
    }
}

val e2eTestImplementation: Configuration by configurations.getting
val e2eTestRuntimeOnly: Configuration by configurations.getting
val storybookTestImplementation: Configuration by configurations.getting
val storybookTestRuntimeOnly: Configuration by configurations.getting

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}")
    }
}

dependencies {
    e2eTestImplementation("org.junit.jupiter:junit-jupiter-api")
    e2eTestImplementation(libs.kotlinLogging)
    e2eTestImplementation(libs.testcontainers.playwright)

    e2eTestRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    e2eTestRuntimeOnly("org.slf4j:slf4j-simple")

    storybookTestImplementation("org.junit.jupiter:junit-jupiter-api")
    storybookTestImplementation(libs.testcontainers.playwright)
    storybookTestImplementation(libs.testcontainers.nginx)
    storybookTestImplementation(libs.kotlinLogging)
    storybookTestImplementation(libs.kotest.assertionsCore)
    storybookTestImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    storybookTestImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    storybookTestImplementation(libs.imageComparison)
    storybookTestImplementation("org.springframework.retry:spring-retry")
    // required by spring retry
    storybookTestImplementation("org.springframework:spring-core")

    storybookTestRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    storybookTestRuntimeOnly("org.slf4j:slf4j-simple")
}

val e2eTest = task<Test>("e2eTest") {
    description = "Runs E2E tests."
    group = "verification"

    testClassesDirs = sourceSets["e2eTest"].output.classesDirs
    classpath = sourceSets["e2eTest"].runtimeClasspath

    // in local dev, docker build is broken as we do not build frontend
    if (System.getenv("CI") == "true") {
        dependsOn(":app:jibDockerBuild")
    }
    // jibDockerBuild does not have outputs, so we cannot make this task cache based on jibDockerBuild;
    // workaround this via a fake property
    inputs.property("cacheIgnoreProperty", System.currentTimeMillis())
}

val storybookTest = task<Test>("storybookTest") {
    description = "Runs Storybook tests."
    group = "verification"

    testClassesDirs = sourceSets["storybookTest"].output.classesDirs
    classpath = sourceSets["storybookTest"].runtimeClasspath
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(Config.JVM_VERSION))
    }
}

// disable extra artifacts as we do not need them (including container image)
tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.classes)
}
