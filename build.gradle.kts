import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
    }
}

plugins {
    alias(libs.plugins.semverGit)
    alias(libs.plugins.versions)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.spring.dependencyManagement) apply false
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
