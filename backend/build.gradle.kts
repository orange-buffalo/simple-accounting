import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    kotlin("jvm") version Versions.kotlin
    kotlin("kapt") version Versions.kotlin
    id("org.jetbrains.kotlin.plugin.spring") version Versions.kotlin
    id("org.jetbrains.kotlin.plugin.jpa") version Versions.kotlin
    id("org.springframework.boot") version Versions.springBoot
}

apply(plugin = "io.spring.dependency-management")

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile("org.springframework.boot:spring-boot-starter-data-jpa")
    compile("org.springframework.boot:spring-boot-starter-webflux")
    compile("org.springframework.boot:spring-boot-starter-security")
    compile("org.springframework.security:spring-security-oauth2-client")

    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("org.jetbrains.kotlin:kotlin-reflect")
    compile("io.github.microutils:kotlin-logging:${Versions.kotlinLogging}")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Versions.kotlinCoroutines}")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Versions.kotlinCoroutines}")

    compile("io.jsonwebtoken:jjwt-api:${Versions.jjwt}")
    compile("com.querydsl:querydsl-jpa:${Versions.queryDsl}")
    compile("com.github.kittinunf.result:result:1.5.0") //todo #68: use arrow

    kapt("com.querydsl:querydsl-apt:${Versions.queryDsl}:jpa")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    runtime("org.flywaydb:flyway-core")
    runtime("com.h2database:h2")
    runtime("io.jsonwebtoken:jjwt-impl:${Versions.jjwt}")
    runtime("io.jsonwebtoken:jjwt-jackson:${Versions.jjwt}")
    runtime("org.glassfish:javax.el:${Versions.javaxEl}")

    compileOnly("org.projectlombok:lombok")

    testCompile("org.junit.jupiter:junit-jupiter-api")
    testCompile("org.junit.jupiter:junit-jupiter-params")
    testCompile("org.springframework.boot:spring-boot-starter-test")
    testCompile("io.projectreactor:reactor-test")
    testCompile("org.springframework.security:spring-security-test")
    testCompile("net.javacrumbs.json-unit:json-unit-assertj:${Versions.jsonUnit}")
    testCompile("com.nhaarman:mockito-kotlin:${Versions.mockitoKotlin}")
    testCompile("org.mockito:mockito-junit-jupiter:${Versions.mockito}")
    testCompile("org.flywaydb:flyway-core")
    testCompile("com.willowtreeapps.assertk:assertk-jvm:${Versions.assertk}")

    testRuntime("org.junit.jupiter:junit-jupiter-engine")
}

val frontendDistDir = "$buildDir/generated-resources/main"
sourceSets {
    main {
        output.dir(frontendDistDir, "builtBy" to "copyFrontend")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    kotlinOptions.jvmTarget = "1.8"
}

tasks.register<Copy>("copyFrontend") {
    from(tasks.getByPath(":frontend:npmBuild"))
    into("$frontendDistDir/META-INF/resources")
}

tasks {
    test {
        useJUnitPlatform()

        beforeTest(KotlinClosure1<TestDescriptor, Any>(project::printTestDescriptionDuringBuild))
    }
}