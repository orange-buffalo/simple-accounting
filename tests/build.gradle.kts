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
}

val e2eTestImplementation: Configuration by configurations.getting
val e2eTestRuntimeOnly: Configuration by configurations.getting

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
