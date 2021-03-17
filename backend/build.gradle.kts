import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage

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
    id("com.bmuschko.docker-remote-api") version Versions.dockerPlugin
    jacoco
}

apply<SaJooqCodeGenPlugin>()
apply<SaDockerPlugin>()

repositories {
    mavenCentral()
    jcenter()
}

sourceSets {
    create("e2eTest")
}

val e2eTestImplementation: Configuration by configurations.getting
val e2eTestRuntimeOnly: Configuration by configurations.getting

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
    testImplementation("com.github.tomakehurst:wiremock-jre8:${Versions.wireMock}")
    testImplementation("org.awaitility:awaitility:${Versions.awaitility}")
    testImplementation("com.github.joschi.openapi-diff:core:${Versions.openapiDiff}")

    testRuntimeOnly("org.springdoc:springdoc-openapi-kotlin:${Versions.springdocOpenapi}")
    testRuntimeOnly("org.springdoc:springdoc-openapi-webflux-core:${Versions.springdocOpenapi}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    e2eTestImplementation("org.junit.jupiter:junit-jupiter-api")
    e2eTestImplementation("com.codeborne:selenide:${Versions.selenide}")
    e2eTestImplementation("org.testcontainers:selenium:${Versions.testContainers}")
    e2eTestImplementation("io.github.microutils:kotlin-logging:${Versions.kotlinLogging}")

    e2eTestRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    e2eTestRuntimeOnly("org.slf4j:slf4j-simple")
}

tasks.register<Copy>("copyFrontend") {
    from(tasks.getByPath(":frontend:npmBuild"))
    into("${sourceSets.getByName("main").output.resourcesDir}/META-INF/resources")
}

tasks.bootJar {
    dependsOn("copyFrontend")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    kotlinOptions.jvmTarget = "1.8"
}

tasks {
    test {
        useJUnitPlatform()
        beforeTest(KotlinClosure1<TestDescriptor, Any>(project::printTestDescriptionDuringBuild))
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
        csv.isEnabled = false
        html.isEnabled = false
    }
}

docker {
    registryCredentials {
        username.set(project.properties["docker.hub.username"] as String?)
        password.set(project.properties["docker.hub.password"] as String?)
    }
}

val saDockerImageExtension = the<SaDockerImageExtension>()

configure<SaDockerImageExtension> {
    image.set("orangebuffalo/simple-accounting:${project.version}")
    dockerBuildDir.set(project.layout.buildDirectory.map { projectBuildDir ->
        projectBuildDir.dir("docker-build")
    })
}

val buildDockerImage = tasks.register<DockerBuildImage>("buildDockerImage") {
    inputDir.set(saDockerImageExtension.dockerBuildDir)
    images.add(saDockerImageExtension.image)
    images.add("orangebuffalo/simple-accounting:latest")
    dependsOn("prepareDockerBuild")
}

project.tasks.register<DockerPushImage>("pushDockerImage") {
    images.add(saDockerImageExtension.image)
    dependsOn(buildDockerImage)
}

tasks.build {
    dependsOn(buildDockerImage)
}

val e2eTest = task<Test>("e2eTest") {
    description = "Runs E2E tests."
    group = "verification"

    useJUnitPlatform()
    beforeTest(KotlinClosure1<TestDescriptor, Any>(project::printTestDescriptionDuringBuild))

    testClassesDirs = sourceSets["e2eTest"].output.classesDirs
    classpath = sourceSets["e2eTest"].runtimeClasspath

    dependsOn(buildDockerImage)
}
