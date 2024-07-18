import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
    }
}

plugins {
    alias(libs.plugins.gitVersioning)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.spring.dependencyManagement) apply false
}

version = "0.0.0-SNAPSHOT"
gitVersioning.apply {
    refs {
        branch("master") {
            version = "\${describe.tag.version.major}.\${describe.tag.version.minor}.\${describe.tag.version.patch.next}-SNAPSHOT"
        }
        branch(".+") {
            version = "\${ref}-SNAPSHOT"
        }
        tag("v(?<version>.*)") {
            version = "\${ref.version}"
        }
    }
    rev {
        version = "\${commit}"
    }
}

allprojects {
    group = "io.orangebuffalo.simpleaccounting"
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
