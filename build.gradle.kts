import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    }
}

plugins {
    id("io.wusa.semver-git-plugin") version Versions.semverGitPlugin
    id("com.github.ben-manes.versions") version Versions.versionsPlugin
    kotlin("jvm") version 1.7.22 apply false
    kotlin("kapt") version Versions.kotlin apply false
    id("io.spring.dependency-management") version Versions.springDependencyManagement apply false
}

val semVersion = semver.info.version
allprojects {
    version = semVersion
    group = "io.orangebuffalo.simpleaccounting"
}

subprojects {
    repositories {
        mavenCentral()
    }
    tasks {
        withType<KotlinCompile> {
            kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
            kotlinOptions.jvmTarget = "${Config.JVM_VERSION}"
            // a workaround for https://github.com/assertj/assertj-core/issues/2357
            // to be removed with upgrade to kotlin 7
            kotlinOptions.languageVersion = "1.7"
        }

        withType<Test> {
            useJUnitPlatform()
            beforeTest(KotlinClosure1<TestDescriptor, Any>(project::printTestDescriptionDuringBuild))
        }
    }
}

buildScan {
    termsOfServiceUrl = "https://gradle.com/terms-of-service"
    termsOfServiceAgree = "yes"
}
