import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
    }
}

plugins {
    alias(libs.plugins.gitSemverPlugin)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.spring.dependencyManagement) apply false
}

semver {
    // tags managed by jreleaser
    createReleaseTag = false
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
                freeCompilerArgs.addAll("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
                jvmTarget.set(JvmTarget.fromTarget(Config.JVM_VERSION.toString()))
            }
        }

        withType<Test> {
            useJUnitPlatform()
            beforeTest(KotlinClosure1<TestDescriptor, Any>(project::printTestDescriptionDuringBuild))
            afterTest(KotlinClosure2<TestDescriptor, TestResult, Any>(project::printTestResultDuringBuild))
            testLogging {
                showStandardStreams = true
            }
        }
    }
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}
