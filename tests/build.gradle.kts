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
                srcDirs(tasks.getByPath(":frontend:npmBuildStorybook"))
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
        mavenBom("org.springframework.boot:spring-boot-dependencies:${Versions.springBoot}") {
            // spring boot comes with 3.x while selenide needs 4.x
            bomProperty("selenium.version", Versions.selenium)
        }
    }
}

dependencies {
    e2eTestImplementation("org.junit.jupiter:junit-jupiter-api")
    e2eTestImplementation("com.codeborne:selenide:${Versions.selenide}")
    e2eTestImplementation("org.testcontainers:selenium:${Versions.testContainers}")
    e2eTestImplementation("io.github.microutils:kotlin-logging:${Versions.kotlinLogging}")
    // add version explicitly to manage upgrades by sa-deppy
    e2eTestImplementation("org.seleniumhq.selenium:selenium-java:${Versions.selenium}")

    e2eTestRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    e2eTestRuntimeOnly("org.slf4j:slf4j-simple")

    storybookTestImplementation("org.junit.jupiter:junit-jupiter-api")
    storybookTestImplementation("com.codeborne:selenide:${Versions.selenide}")
    storybookTestImplementation("org.testcontainers:selenium:${Versions.testContainers}")
    storybookTestImplementation("org.testcontainers:nginx:${Versions.testContainers}")
    storybookTestImplementation("io.github.microutils:kotlin-logging:${Versions.kotlinLogging}")
    // add version explicitly to manage upgrades by sa-deppy
    storybookTestImplementation("org.seleniumhq.selenium:selenium-java:${Versions.selenium}")
    storybookTestImplementation("io.kotest:kotest-assertions-core-jvm:5.3.1")
    storybookTestImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    storybookTestImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    storybookTestImplementation("com.github.romankh3:image-comparison:4.4.0")

    storybookTestRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    storybookTestRuntimeOnly("org.slf4j:slf4j-simple")
}

val e2eTest = task<Test>("e2eTest") {
    description = "Runs E2E tests."
    group = "verification"

    testClassesDirs = sourceSets["e2eTest"].output.classesDirs
    classpath = sourceSets["e2eTest"].runtimeClasspath

    inputs.files(tasks.getByPath(":backend:prepareDockerBuild").outputs.files)

    dependsOn(":backend:buildDockerImage")
}

val storybookTest = task<Test>("storybookTest") {
    description = "Runs Storybook tests."
    group = "verification"

    testClassesDirs = sourceSets["storybookTest"].output.classesDirs
    classpath = sourceSets["storybookTest"].runtimeClasspath
}
