import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jreleaser.model.Active

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
    id("io.spring.dependency-management")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.springBoot)
    jacoco
    alias(libs.plugins.jib)
    alias(libs.plugins.jreleaser)
}

apply<SaJooqCodeGenPlugin>()
apply<SaHotReloadPlugin>()
apply<SaDgsCodegenPlugin>()

val mockitoAgent = configurations.create("mockitoAgent")
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
    implementation(libs.graphqlKotlin.springServer)

    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientCio)
    implementation(libs.ktor.contentNegotiation)
    implementation(libs.ktor.kotlinxJson)
    implementation(libs.ktor.auth)

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
    // workaround for https://github.com/JetBrains/JetBrainsRuntime/issues/259
    testImplementation(libs.mockito.subclass)
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
    testImplementation(libs.dsgCodegen.core)

    testRuntimeOnly(libs.springdocOpenapi.webfluxApi)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
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

val e2eTestPattern = "*E2eTests"
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    filter {
        excludeTestsMatching(e2eTestPattern)
    }
    // Default logging for tests is too verbose for CI; reduce extra load on the infrastructure
    val loggingProperties = mutableListOf<String>()
    ifCi {
        loggingProperties.add("-Dlogging.level.org.jooq.tools.LoggerListener=warn")
        loggingProperties.add("-Dlogging.level.io.orangebuffalo.simpleaccounting=warn")
        loggingProperties.add("-Dlogging.level.org.springframework.test.context.cache=warn")
    }
    configureTestTask(mockitoAgent, loggingProperties)
    // still reset every once in a while to avoid contexts cache overgrowth
    forkEvery = 1000
    maxParallelForks = (Runtime.getRuntime().availableProcessors() - 1)
        .coerceAtLeast(1)
        .coerceAtMost(5)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(false)
    }
}

tasks.check {
    dependsOn(rootProject.tasks.named("detekt"))
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

jreleaser {
    gitRootSearch = true
    release {
        github {
            dryrun = project.version.get().endsWith("-SNAPSHOT")

            uploadAssets = Active.NEVER
            prerelease {
                enabled = true
            }
            changelog {
                formatted = Active.ALWAYS
                preset = "conventional-commits"
                skipMergeCommits = true
                hide {
                    uncategorized = true
                    contributor("[bot]")
                    contributor("orange-buffalo")
                    contributor("GitHub")
                }
            }
        }
    }
    signing {
        active = Active.NEVER
    }
    deploy {
        active = Active.NEVER
    }
}

// Reduces the number of Playwright browsers installed by default
// and separates installation from the test execution
tasks.register<JavaExec>("installPlaywrightDependencies") {
    group = "playwright"
    description = "Installs Playwright dependencies."
    classpath = configurations.testRuntimeClasspath.get()
    mainClass.set("com.microsoft.playwright.CLI")
    args("install", "chromium", "--only-shell")
}

tasks.register<Test>("updateGraphqlSchema") {
    group = "verification"
    description = "Updates the Git-managed GraphQL schema by running GraphqlSchemaTest with override enabled."

    filter {
        includeTestsMatching("*GraphqlSchemaTest*")
    }

    systemProperty("simpleaccounting.graphql.updateSchema", "true")

    // Ensure the test runs even if it was previously successful
    outputs.upToDateWhen { false }

    configureTestTask(mockitoAgent)
}

// DGS Codegen taskd for test GraphQL client generation
tasks.withType<GenerateJavaTask> {
    // Remove any previously generated sources, e.g. if we renamed something in the schema
    doFirst {
        delete(layout.buildDirectory.map {
            it.dir("generated/sources/dgs-codegen")
        })
    }

    language = "kotlin"
    schemaPaths = mutableListOf(project.file("src/test/resources/api-schema.graphqls"))
    packageName = "io.orangebuffalo.simpleaccounting.infra.graphql"
    subPackageNameClient = "client"
    subPackageNameTypes = "client.types"
    generateClient = true
    generateKotlinNullableClasses = true
    generateKotlinClosureProjections = true
}

tasks.compileTestKotlin {
    dependsOn("generateJava")
}
