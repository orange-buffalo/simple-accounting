buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.kotlin.allopen)
        classpath(libs.kotlin.noarg)
    }
}

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("io.spring.dependency-management")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.springBoot)
    jacoco
    alias(libs.plugins.jib)
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
    implementation(libs.kotlinLogging)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.kotlinx.coroutines.reactor)

    implementation(libs.jooq)
    implementation(libs.jjwt.api)
    implementation(libs.arrow.core)
    implementation(libs.springdocOpenapi.starterCommon)

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    runtimeOnly("org.flywaydb:flyway-core")
    runtimeOnly("com.h2database:h2")
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    runtimeOnly(libs.javax.el)

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.jsonUnit.assertj)
    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.mockito.junitJupiter)
    testImplementation("org.flywaydb:flyway-core")
    testImplementation(libs.guava)
    testImplementation(libs.assertk)
    testImplementation(libs.wiremock.jre8Standalone)
    testImplementation(libs.awaitility)
    testImplementation(libs.zjsonpatch)
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    testImplementation(libs.testcontainers)

    testRuntimeOnly(libs.springdocOpenapi.webfluxApi)
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
