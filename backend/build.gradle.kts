buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-allopen:${Versions.kotlin}")
        classpath("org.jetbrains.kotlin:kotlin-noarg:${Versions.kotlin}")
    }
}

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("io.spring.dependency-management")
    id("org.jetbrains.kotlin.plugin.spring") version Versions.kotlin
    id("org.springframework.boot") version Versions.springBoot
    jacoco
    id("com.google.cloud.tools.jib") version "3.2.1"
}

apply<SaJooqCodeGenPlugin>()

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.microutils:kotlin-logging:${Versions.kotlinLogging}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:${Versions.kotlinCoroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Versions.kotlinCoroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Versions.kotlinCoroutines}")

    implementation("org.jooq:jooq:${Versions.jooq}")
    implementation("io.jsonwebtoken:jjwt-api:${Versions.jjwt}")
    implementation("io.arrow-kt:arrow-core:${Versions.arrow}")
    implementation("org.springdoc:springdoc-openapi-starter-common:${Versions.springdocOpenapi}")

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    runtimeOnly("org.flywaydb:flyway-core")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${Versions.jjwt}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Versions.jjwt}")
    runtimeOnly("org.glassfish:javax.el:${Versions.javaxEl}")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:${Versions.jsonUnit}")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}")
    testImplementation("org.mockito:mockito-junit-jupiter:${Versions.mockito}")
    testImplementation("org.flywaydb:flyway-core")
    testImplementation("com.google.guava:guava:${Versions.guava}")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:${Versions.assertk}")
    testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:${Versions.wireMock}")
    testImplementation("org.awaitility:awaitility:${Versions.awaitility}")
    testImplementation("com.flipkart.zjsonpatch:zjsonpatch:${Versions.zjsonpatch}")
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    testImplementation("org.testcontainers:testcontainers:${Versions.testContainers}")

    testRuntimeOnly("org.springdoc:springdoc-openapi-starter-webflux-api:${Versions.springdocOpenapi}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

sourceSets {
    main {
        // add frontend application build results;
        // do not add this dependency in dev environment to avoid npm rebuild on each change,
        // as running against the dev server is a typical use case
        if (System.getenv("CI") == "true") {
            resources {
                srcDirs(tasks.getByPath(":frontend:buildFrontend"))
            }
        }
    }
}

// disable extra artifacts as we do not need them (including container image)
tasks.jar {
    enabled = false
}

tasks.bootJar {
    enabled = false
}

tasks.resolveMainClassName {
    enabled = false
}

jib {
    to {
        image = "orangebuffalo/simple-accounting:${project.version}"
        tags = setOf("latest")
    }
    container {
        ports = listOf("9393")
        jvmFlags = listOf("-Xmx128m")
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(false)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(Config.JVM_VERSION))
    }
}
