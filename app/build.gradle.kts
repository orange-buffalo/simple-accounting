import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
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
    id("io.spring.dependency-management")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.springBoot)
    jacoco
    alias(libs.plugins.jib)
    alias(libs.plugins.jooq.codegen)
}

apply<SaHotReloadPlugin>()
apply<SaDgsCodegenPlugin>()

val mockitoAgent = configurations.create("mockitoAgent")
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(libs.kotlinLogging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.dateTime)

    implementation(libs.jooq)
    jooqCodegen(libs.jooq.metaExtensions) {
        exclude(group = "org.springframework")
    }
    implementation(libs.jjwt.api)
    implementation(libs.commonsLang3)
    implementation(libs.graphqlKotlin.schemaGenerator)
    implementation(libs.graphqlKotlin.dataloader)
    implementation(libs.graphqlKotlin.federation)


    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.gson)
    runtimeOnly(libs.javax.el)

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation(libs.jsonUnit.kotest)
    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.mockito.junitJupiter)
    // workaround for https://github.com/JetBrains/JetBrainsRuntime/issues/259
    testImplementation(libs.mockito.subclass)
    testImplementation(libs.guava)
    testImplementation(libs.wiremock.standalone)
    testImplementation(libs.awaitility)
    testImplementation(libs.rerunnerJupiter)
    testImplementation(libs.zjsonpatch)
    testImplementation(libs.jackson.dataformat.yaml)
    testImplementation("tools.jackson.module:jackson-module-kotlin")
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.nginx)
    testImplementation(libs.playwright)
    testImplementation(libs.springRetry)
    testImplementation(libs.kotest.assertionsCore)
    testImplementation(libs.mockOauth2Server)
    testImplementation(libs.kotest.assertionsPlaywright)
    testImplementation(libs.dsgCodegen.core)

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    mockitoAgent("org.mockito:mockito-core") { isTransitive = false }
}

val flywayMigrationDir = "src/main/resources/db/migration"
jooq {
    configuration {
        generator {
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                properties {
                    property {
                        key = "scripts"
                        value = "$flywayMigrationDir/*.sql"
                    }
                    property {
                        key = "sort"
                        value = "flyway"
                    }
                    property {
                        key = "defaultNameCase"
                        value = "lower"
                    }
                }
                forcedTypes {
                    forcedType {
                        includeExpression = """INCOME\.STATUS"""
                        userType = "io.orangebuffalo.simpleaccounting.business.incomes.IncomeStatus"
                        setEnumConverter(true)
                    }
                    forcedType {
                        includeExpression = """EXPENSE\.STATUS"""
                        userType = "io.orangebuffalo.simpleaccounting.business.expenses.ExpenseStatus"
                        setEnumConverter(true)
                    }
                    forcedType {
                        includeExpression = """INVOICE\.STATUS"""
                        userType = "io.orangebuffalo.simpleaccounting.business.invoices.InvoiceStatus"
                        setEnumConverter(true)
                    }
                    forcedType {
                        name = "DECIMAL"
                        includeExpression = """ACCOUNT_STATEMENT_RECORD\.EXCHANGE_RATE"""
                    }
                    forcedType {
                        includeTypes = "TIMESTAMP.*"
                        userType = "java.time.Instant"
                        converter = "io.orangebuffalo.simpleaccounting.infra.jooq.InstantConverter"
                    }
                    forcedType {
                        includeTypes = "DATE"
                        userType = "java.time.LocalDate"
                        converter = "io.orangebuffalo.simpleaccounting.infra.jooq.LocalDateConverter"
                    }
                }
            }
            strategy { name = SaGeneratorStrategy::class.java.name }
            target {
                directory = layout.buildDirectory.dir("generated-sources/jooq").get().asFile.absolutePath
                packageName = "io.orangebuffalo.simpleaccounting.services.persistence.model"
            }
            generate { setJavaTimeTypes(false) }
        }
    }
}

val generateJooq = tasks.named("jooqCodegen") {
    inputs.files(fileTree(flywayMigrationDir) { include("*.sql") })
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
tasks.compileJava { dependsOn(generateJooq) }
tasks.compileKotlin { dependsOn(generateJooq) }

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

tasks.bootRun {
    mainClass.set("io.orangebuffalo.simpleaccounting.SimpleAccountingApplicationKt")
}

jib {
    from {
        image = "eclipse-temurin:25-jre-jammy"
    }
    to {
        image = "orangebuffalo/simple-accounting:${project.version}"
        tags = setOf("latest")
    }
    container {
        user = "65532:65532"
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

val forceSimpleAccountingFrontendBuild = providers.gradleProperty("forceSimpleAccountingFrontendBuild")
    .map(String::toBoolean)
    .getOrElse(false)
tasks.withType<KotlinCompile> {
    ifCi {
        dependsOn(copyFrontendTask)
    }
    ifLocal {
        if (forceSimpleAccountingFrontendBuild) {
            dependsOn(copyFrontendTask)
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(Config.JVM_VERSION))
        // vendor is set in SaHotReloadPlugin
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

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    inputs.files(sourceSets.main.get().allSource)
    inputs.files(sourceSets.test.get().allSource)

    filter {
        includeTestsMatching("*GraphqlSchemaTest*")
    }

    systemProperty("sa.graphql.updateSchema", "true")

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
    typeMapping = mutableMapOf(
        "LocalDate" to "java.time.LocalDate",
        "Long" to "kotlin.Long",
        "DateTime" to "java.time.Instant",
    )
}

tasks.compileTestKotlin {
    dependsOn("generateJava")
}
