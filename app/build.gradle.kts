import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    alias(libs.plugins.kotlin.serialization)
    kotlin("kapt")
    id("io.spring.dependency-management")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.springBoot)
    jacoco
    alias(libs.plugins.jib)
}

apply<SaJooqCodeGenPlugin>()
apply<SaHotReloadPlugin>()

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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.dateTime)

    implementation(libs.jooq)
    implementation(libs.jjwt.api)
    implementation(libs.springdocOpenapi.starterCommon)

    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientCio)
    implementation(libs.ktor.contentNegotiation)
    implementation(libs.ktor.kotlinxJson)
    implementation(libs.ktor.auth)

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
    testImplementation(libs.jsonUnit.kotest)
    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.mockito.junitJupiter)
    testImplementation("org.flywaydb:flyway-core")
    testImplementation(libs.guava)
    testImplementation(libs.assertk)
    testImplementation(libs.wiremock.standalone)
    testImplementation(libs.awaitility)
    testImplementation(libs.zjsonpatch)
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.nginx)
    testImplementation(libs.playwright)
    testImplementation("org.springframework.retry:spring-retry")
    testImplementation(libs.kotest.assertionsCore)
    testImplementation(libs.mockOauth2Server)
    testImplementation(libs.kotest.assertionsPlaywright)

    testRuntimeOnly(libs.springdocOpenapi.webfluxApi)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

val frontendBuildTaskName = ":frontend:buildFrontend"

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

tasks.withType<KaptGenerateStubs> {
    dependsOn("jooqGenerateSources")
}

jib {
    from {
        image = "eclipse-temurin:21-jre-jammy"
    }
    to {
        image = "orangebuffalo/simple-accounting:${project.version}"
        tags = setOf("latest")
    }
    container {
        ports = listOf("9393")
        jvmFlags = listOf("-Xmx128m")
    }
}

val screenshotsTestPattern = "*UiComponentsScreenshotsTest"
val e2eTestPattern = "*E2eTests"
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    filter {
        excludeTestsMatching(screenshotsTestPattern)
        excludeTestsMatching(e2eTestPattern)
    }
    // to support mocking libraries via byte-buddy agent
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(false)
    }
}

tasks.withType<Test>().configureEach {
    // CI machines are not powerful enough to run multiple UI test suites in parallel, they get flaky
    ifLocal {
        maxParallelForks = (Runtime.getRuntime().availableProcessors() - 1)
            .coerceAtLeast(1)
            .coerceAtMost(5)
    }
}

tasks.register<Test>("screenshotsTest") {
    description = "Runs screenshot tests for UI components"
    filter {
        includeTestsMatching(screenshotsTestPattern)
    }
    // any change in frontend code could change the stories outcome
    inputs.files(project(":frontend").projectDir)
    // do not add this dependency in dev to avoid storybook rebuild on each change,
    // as we recommend to run tests against running storybook locally
    ifCi {
        dependsOn(tasks.getByPath(":frontend:buildStorybook"))
        inputs.files(tasks.getByPath(":frontend:buildStorybook").outputs)
    }
}

tasks.register<Test>("e2eTest") {
    description = "Runs E2E tests."

    // in local dev, docker build is broken as we do not build frontend
    ifCi {
        dependsOn(":app:jibDockerBuild")
    }
    // jibDockerBuild does not have outputs, so we cannot make this task cache based on jibDockerBuild;
    // workaround this via a fake property
    inputs.property("cacheIgnoreProperty", System.currentTimeMillis())

    filter {
        includeTestsMatching(e2eTestPattern)
    }
}

val copyFrontendTask = tasks.register<Copy>("copyFrontend") {
    from(tasks.getByPath(frontendBuildTaskName))
    into("build/resources/main")
}

tasks.withType<KotlinCompile> {
    ifCi {
        dependsOn(copyFrontendTask)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(Config.JVM_VERSION))
        // vendor is set in SaHotReloadPlugin
    }
}
