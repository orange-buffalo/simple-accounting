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
