import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.fus.internal.isCiBuild
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
    }
}

plugins {
    alias(libs.plugins.gitSemverPlugin)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.spring.dependencyManagement) apply false
}

semver {
    createReleaseTag = true
    releaseTagNameFormat = "v%s"
}
val ver = semver.version
allprojects {
    group = "io.orangebuffalo.simpleaccounting"
    version = ver
}

subprojects {
    repositories {
        mavenCentral()
    }
    tasks {
        withType<KotlinCompile> {
            compilerOptions {
                freeCompilerArgs.addAll("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn", "-Xannotation-default-target=param-property")
                jvmTarget.set(JvmTarget.fromTarget(Config.JVM_VERSION.toString()))
            }
        }

        withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("started", "passed", "skipped", "failed")
                showStandardStreams = true
            }
        }
    }
}

develocity {
    buildScan {
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
        publishing {
            onlyIf { System.getenv("CI") == "true" }
        }
    }
}

val checkAgentHarness = tasks.register<Exec>("checkAgentHarness") {
    group = "verification"
    description = "Runs offline regression tests for the repository agent controller."
    commandLine("node", "--test", ".harness/tests/core.test.mjs", ".harness/tests/plugin.test.mjs")
}

tasks.register<Exec>("bootstrapAgentHarness") {
    group = "development"
    description = "Installs checksum-pinned Compound Engineering reference skills."
    commandLine("python3", ".harness/bootstrap.py")
}

tasks.register<Exec>("doctorAgentHarness") {
    group = "verification"
    description = "Checks the local agent workflow prerequisites."
    commandLine("python3", ".harness/doctor.py")
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(checkAgentHarness)
    }
}
