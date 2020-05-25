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
    id("org.springframework.boot") version Versions.springBoot
    id("io.spring.dependency-management") version Versions.springDependencyManagement
}

apply<SaJooqCodeGenPlugin>()

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-client")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.microutils:kotlin-logging:${Versions.kotlinLogging}")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:${Versions.kotlinCoroutines}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Versions.kotlinCoroutines}")
    implementation("org.jooq:jooq:${Versions.jooq}")

    implementation("io.jsonwebtoken:jjwt-api:${Versions.jjwt}")
    implementation("io.arrow-kt:arrow-core:${Versions.arrow}")

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    runtimeOnly("org.flywaydb:flyway-core")
    // todo remove specific version once resolved https://github.com/h2database/h2database/issues/2204
    runtimeOnly("com.h2database:h2:1.4.199")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${Versions.jjwt}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Versions.jjwt}")
    runtimeOnly("org.glassfish:javax.el:${Versions.javaxEl}")

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:${Versions.jsonUnit}")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:${Versions.mockitoKotlin}")
    testImplementation("org.mockito:mockito-junit-jupiter:${Versions.mockito}")
    testImplementation("org.flywaydb:flyway-core")
    testImplementation("com.google.guava:guava:${Versions.guava}")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:${Versions.assertk}")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
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

        systemProperty("spring.profiles.active", "test")

        beforeTest(KotlinClosure1<TestDescriptor, Any>(project::printTestDescriptionDuringBuild))
    }
}
